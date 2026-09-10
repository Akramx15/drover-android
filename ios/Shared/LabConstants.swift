import Foundation

enum LabConstants {
    static let appBundleIdentifier = "app.drover.ios.lab"
    static let providerBundleIdentifier = "app.drover.ios.lab.PacketTunnel"
    static let appGroupIdentifier = "group.app.drover.ios.lab"
    static let traceFileName = "drover-lab-trace.jsonl"

    // This value must match NETestAppMapping in the host Info.plist and
    // VPNUUID in Config/WhatsAppPerAppVPN.mobileconfig.template.
    static let perAppVPNIdentifier = "1D4D3F5C-8948-4B6D-9D3C-62C9C22E181B"

    static let strategy = "none"

    static var traceURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier)?
            .appendingPathComponent(traceFileName, isDirectory: false)
    }
}
