import Darwin
import Foundation
import NetworkExtension

private let droverNativeReadyCallback: @convention(c) (UnsafeMutableRawPointer?) -> Void = { context in
    guard let context else { return }
    Unmanaged<PacketTunnelProvider>
        .fromOpaque(context)
        .takeUnretainedValue()
        .nativeDidBecomeReady()
}

final class PacketTunnelProvider: NEPacketTunnelProvider {
    private let nativeQueue = DispatchQueue(label: "app.drover.ios.lab.native", qos: .userInitiated)
    private let stateQueue = DispatchQueue(label: "app.drover.ios.lab.state")
    private let stopQueue = DispatchQueue(label: "app.drover.ios.lab.stop", qos: .userInitiated)

    private var startInProgress = false
    private var nativeRunning = false
    private var nativeCallStarted = false
    private var nativeReady = false
    private var stopRequested = false
    private var pendingStartCompletion: ((Error?) -> Void)?
    private var pendingStopCompletions: [() -> Void] = []

    override func startTunnel(
        options: [String: NSObject]?,
        completionHandler: @escaping (Error?) -> Void
    ) {
        let accepted = stateQueue.sync {
            guard !startInProgress, !nativeRunning else { return false }
            startInProgress = true
            nativeReady = false
            stopRequested = false
            pendingStartCompletion = completionHandler
            return true
        }
        guard accepted else {
            completionHandler(LabTunnelError.alreadyStarting)
            return
        }

        let settings = makeNetworkSettings()
        setTunnelNetworkSettings(settings) { [weak self] error in
            guard let self else {
                completionHandler(LabTunnelError.providerReleased)
                return
            }
            if let error {
                self.finishFailedStart(error)
                return
            }

            guard let traceURL = LabConstants.traceURL else {
                self.finishFailedStart(LabTunnelError.appGroupUnavailable)
                return
            }
            do {
                try self.protectTraceFile(at: traceURL)
            } catch {
                self.finishFailedStart(LabTunnelError.traceProtectionFailed(error.localizedDescription))
                return
            }
            guard let tunFileDescriptor = UTUNFileDescriptor.find(forAddress: "10.77.0.1") else {
                self.finishFailedStart(LabTunnelError.tunnelDescriptorUnavailable)
                return
            }

            // Ignore caller-provided strategy values. This PoC is deliberately
            // observe-only until a WhatsApp-specific hypothesis is reviewed.
            let arguments = self.makeNativeArguments(
                tunFileDescriptor: tunFileDescriptor,
                tracePath: traceURL.path
            )

            let shouldSchedule = self.stateQueue.sync {
                self.startInProgress = false
                guard !self.stopRequested else { return false }
                self.nativeRunning = true
                return true
            }
            guard shouldSchedule else {
                self.finishStoppedBeforeNativeStart()
                return
            }

            self.nativeQueue.async { [self] in
                let shouldRun = self.stateQueue.sync {
                    guard !self.stopRequested else {
                        self.nativeRunning = false
                        return false
                    }
                    self.nativeCallStarted = true
                    return true
                }
                guard shouldRun else {
                    self.finishStoppedBeforeNativeStart()
                    return
                }

                let result = arguments.withCString { pointer in
                    tun2proxy_run_with_cli_args_and_ready(
                        pointer,
                        1500,
                        true,
                        droverNativeReadyCallback,
                        Unmanaged.passUnretained(self).toOpaque()
                    )
                }

                self.stateQueue.async {
                    self.nativeRunning = false
                    self.nativeCallStarted = false
                    let startCompletion = self.pendingStartCompletion
                    self.pendingStartCompletion = nil
                    let stopCompletions = self.pendingStopCompletions
                    self.pendingStopCompletions.removeAll()
                    if let startCompletion {
                        startCompletion(LabTunnelError.nativeEngineStopped(code: result))
                    }
                    if !stopCompletions.isEmpty {
                        stopCompletions.forEach { $0() }
                        return
                    }

                    if startCompletion == nil, !self.stopRequested {
                        let error = LabTunnelError.nativeEngineStopped(code: result)
                        self.cancelTunnelWithError(error)
                    }
                }
            }
        }
    }

    override func stopTunnel(
        with reason: NEProviderStopReason,
        completionHandler: @escaping () -> Void
    ) {
        stateQueue.async { [weak self] in
            guard let self else {
                completionHandler()
                return
            }
            self.stopRequested = true
            guard self.startInProgress || self.nativeRunning else {
                completionHandler()
                return
            }

            self.pendingStopCompletions.append(completionHandler)
            if self.nativeCallStarted {
                self.requestNativeStop()
            }
        }
    }

    override func handleAppMessage(
        _ messageData: Data,
        completionHandler: ((Data?) -> Void)? = nil
    ) {
        let response: [String: Any] = [
            "strategy": LabConstants.strategy,
            "traceFile": LabConstants.traceFileName,
            "payloadCaptured": false
        ]
        completionHandler?(try? JSONSerialization.data(withJSONObject: response, options: []))
    }

    private func makeNetworkSettings() -> NEPacketTunnelNetworkSettings {
        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "192.0.2.2")
        settings.mtu = NSNumber(value: 1500)

        let ipv4 = NEIPv4Settings(
            addresses: ["10.77.0.1"],
            subnetMasks: ["255.255.255.255"]
        )
        ipv4.includedRoutes = [NEIPv4Route.default()]
        settings.ipv4Settings = ipv4

        let ipv6 = NEIPv6Settings(
            addresses: ["fd42:4242:4242::1"],
            networkPrefixLengths: [NSNumber(value: 128)]
        )
        ipv6.includedRoutes = [NEIPv6Route.default()]
        settings.ipv6Settings = ipv6

        // Leave dnsSettings unset so the system keeps the underlying resolver
        // path. This avoids forcing a public DNS provider during observation.
        return settings
    }

    private func makeNativeArguments(tunFileDescriptor: Int32, tracePath: String) -> String {
        [
            "tun2proxy-bin",
            "--tun-fd", String(tunFileDescriptor),
            "--close-fd-on-drop", "false",
            "--proxy", "none",
            "--dns", "direct",
            "--dns-addr", "1.1.1.1",
            "--ipv6-enabled",
            "--mtu", "1500",
            "--tcp-mss", "1440",
            "--udp-timeout", "120",
            "--verbosity", "warn",
            "--drover-strategy", "none",
            "--lab-trace-file", shellQuote(tracePath)
        ].joined(separator: " ")
    }

    private func shellQuote(_ value: String) -> String {
        "'" + value.replacingOccurrences(of: "'", with: "'\"'\"'") + "'"
    }

    fileprivate func nativeDidBecomeReady() {
        stateQueue.async { [weak self] in
            guard let self, self.nativeRunning, self.nativeCallStarted, !self.nativeReady else {
                return
            }
            self.nativeReady = true
            let startCompletion = self.pendingStartCompletion
            self.pendingStartCompletion = nil

            do {
                guard let traceURL = LabConstants.traceURL else {
                    throw LabTunnelError.appGroupUnavailable
                }
                // Rust opens the existing file with truncate semantics. Reapply
                // both attributes after that open so backup remains fail-closed
                // even if filesystem behavior changes on a future iOS release.
                try self.protectTraceFile(at: traceURL)
            } catch {
                self.stopRequested = true
                startCompletion?(LabTunnelError.traceProtectionFailed(error.localizedDescription))
                self.requestNativeStop()
                return
            }

            if self.stopRequested {
                startCompletion?(LabTunnelError.startCancelled)
                self.requestNativeStop()
            } else {
                startCompletion?(nil)
            }
        }
    }

    private func protectTraceFile(at traceURL: URL) throws {
        let fileManager = FileManager.default
        try fileManager.createDirectory(
            at: traceURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        if !fileManager.fileExists(atPath: traceURL.path) {
            let created = fileManager.createFile(
                atPath: traceURL.path,
                contents: nil,
                attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication]
            )
            guard created else { throw CocoaError(.fileWriteUnknown) }
        }
        try fileManager.setAttributes(
            [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication],
            ofItemAtPath: traceURL.path
        )

        var protectedURL = traceURL
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try protectedURL.setResourceValues(values)
        let verification = try protectedURL.resourceValues(forKeys: [.isExcludedFromBackupKey])
        guard verification.isExcludedFromBackup == true else {
            throw CocoaError(.fileWriteUnknown)
        }
    }

    private func finishFailedStart(_ error: Error) {
        stateQueue.async { [self] in
            self.startInProgress = false
            self.nativeRunning = false
            self.nativeCallStarted = false
            self.nativeReady = false
            let startCompletion = self.pendingStartCompletion
            self.pendingStartCompletion = nil
            let stopCompletions = self.pendingStopCompletions
            self.pendingStopCompletions.removeAll()
            startCompletion?(error)
            stopCompletions.forEach { $0() }
        }
    }

    private func finishStoppedBeforeNativeStart() {
        stateQueue.async { [self] in
            self.startInProgress = false
            self.nativeRunning = false
            self.nativeCallStarted = false
            self.nativeReady = false
            let startCompletion = self.pendingStartCompletion
            self.pendingStartCompletion = nil
            let stopCompletions = self.pendingStopCompletions
            self.pendingStopCompletions.removeAll()
            startCompletion?(LabTunnelError.startCancelled)
            stopCompletions.forEach { $0() }
        }
    }

    private func requestNativeStop(attemptsRemaining: Int = 1_000) {
        stopQueue.async { [weak self] in
            guard let self else { return }
            let result = tun2proxy_stop()

            let shouldRetry = self.stateQueue.sync {
                self.stopRequested
                    && self.nativeRunning
                    && self.nativeCallStarted
                    && (!self.nativeReady || result != 0)
            }
            guard shouldRetry, attemptsRemaining > 1 else { return }
            self.stopQueue.asyncAfter(deadline: .now() + .milliseconds(10)) { [weak self] in
                self?.requestNativeStop(attemptsRemaining: attemptsRemaining - 1)
            }
        }
    }
}

private enum LabTunnelError: LocalizedError {
    case alreadyStarting
    case startCancelled
    case providerReleased
    case appGroupUnavailable
    case traceProtectionFailed(String)
    case tunnelDescriptorUnavailable
    case nativeEngineStopped(code: Int32)

    var errorDescription: String? {
        switch self {
        case .alreadyStarting:
            return "The packet-tunnel provider is already starting or running."
        case .startCancelled:
            return "The packet-tunnel start was cancelled before the native engine became ready."
        case .providerReleased:
            return "The packet-tunnel provider was released while starting."
        case .appGroupUnavailable:
            return "The shared App Group container is unavailable. Check both provisioning profiles."
        case let .traceProtectionFailed(reason):
            return "The diagnostic trace could not be protected from device backup: \(reason)"
        case .tunnelDescriptorUnavailable:
            return "The iOS 15+ utun file descriptor could not be identified."
        case let .nativeEngineStopped(code):
            return "The native observation engine stopped unexpectedly (code \(code))."
        }
    }
}
