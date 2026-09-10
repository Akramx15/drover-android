import NetworkExtension
import SwiftUI

final class TunnelManager: ObservableObject {
    @Published private(set) var status: NEVPNStatus = .invalid
    @Published private(set) var hasConfiguration = false
    @Published private(set) var detailText = "Install the development per-app VPN profile, then reload."
    @Published private(set) var errorText: String?

    private var manager: NETunnelProviderManager?
    private var statusObserver: NSObjectProtocol?

    init() {
        statusObserver = NotificationCenter.default.addObserver(
            forName: .NEVPNStatusDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.publishConnectionStatus()
        }
    }

    deinit {
        if let statusObserver {
            NotificationCenter.default.removeObserver(statusObserver)
        }
    }

    var statusText: String {
        switch status {
        case .invalid: return "Profile not loaded"
        case .disconnected: return "Disconnected"
        case .connecting: return "Connecting"
        case .connected: return "Observing WhatsApp traffic"
        case .reasserting: return "Reconnecting"
        case .disconnecting: return "Disconnecting"
        @unknown default: return "Unknown"
        }
    }

    var statusColor: Color {
        switch status {
        case .connected: return .green
        case .connecting, .reasserting: return .orange
        case .disconnecting: return .yellow
        case .disconnected, .invalid: return .gray
        @unknown default: return .gray
        }
    }

    var canStop: Bool {
        switch status {
        case .connecting, .connected, .reasserting:
            return true
        default:
            return false
        }
    }

    var canInspectTrace: Bool {
        hasConfiguration && status == .disconnected
    }

    func reload() {
        errorText = nil
        NETunnelProviderManager.loadAllFromPreferences { [weak self] managers, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let error {
                    self.manager = nil
                    self.hasConfiguration = false
                    self.status = .invalid
                    self.detailText = "The VPN configuration could not be loaded."
                    self.errorText = error.localizedDescription
                    return
                }

                let match = managers?.first(where: { candidate in
                    guard let configuration = candidate.protocolConfiguration as? NETunnelProviderProtocol else {
                        return false
                    }
                    return configuration.providerBundleIdentifier == LabConstants.providerBundleIdentifier
                })

                self.manager = match
                self.hasConfiguration = match != nil
                if let match {
                    self.status = match.connection.status
                    self.detailText = "Development mapping loaded. Strategy: none (observe-only)."
                } else {
                    self.status = .invalid
                    self.detailText = "Install Config/WhatsAppPerAppVPN.mobileconfig after replacing the template values, then tap Reload."
                }
            }
        }
    }

    func start() {
        errorText = nil
        guard let manager else {
            errorText = "The development per-app VPN profile is not installed."
            return
        }

        manager.loadFromPreferences { [weak self] error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let error {
                    self.errorText = error.localizedDescription
                    return
                }
                guard let session = manager.connection as? NETunnelProviderSession else {
                    self.errorText = "The installed profile is not a packet-tunnel configuration."
                    return
                }
                do {
                    try session.startTunnel(options: ["droverStrategy": LabConstants.strategy as NSString])
                    self.publishConnectionStatus()
                } catch {
                    self.errorText = error.localizedDescription
                }
            }
        }
    }

    func stop() {
        errorText = nil
        manager?.connection.stopVPNTunnel()
        publishConnectionStatus()
    }

    private func publishConnectionStatus() {
        guard let manager else { return }
        status = manager.connection.status
    }
}
