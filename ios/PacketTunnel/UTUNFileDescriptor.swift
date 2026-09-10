import Darwin
import Foundation

enum UTUNFileDescriptor {
    // Darwin's control socket protocol and UTUN_OPT_IFNAME are both stable
    // numeric ABI values used by the modern tun2proxy iOS integration guide.
    private static let systemControlProtocol: Int32 = 2
    private static let interfaceNameOption: Int32 = 2

    static func find(forAddress address: String) -> Int32? {
        guard #available(iOS 15.0, *) else { return nil }
        guard let expectedInterface = interfaceName(forAddress: address) else { return nil }

        for descriptor in Int32(0)...Int32(1024) {
            var nameBuffer = [CChar](repeating: 0, count: Int(IFNAMSIZ))
            var nameLength = socklen_t(nameBuffer.count)
            let result = nameBuffer.withUnsafeMutableBytes { bytes in
                getsockopt(
                    descriptor,
                    systemControlProtocol,
                    interfaceNameOption,
                    bytes.baseAddress,
                    &nameLength
                )
            }
            guard result == 0 else { continue }

            let interfaceName = String(cString: nameBuffer)
            if interfaceName == expectedInterface {
                return descriptor
            }
        }
        return nil
    }

    private static func interfaceName(forAddress expectedAddress: String) -> String? {
        var firstAddress: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&firstAddress) == 0, let firstAddress else { return nil }
        defer { freeifaddrs(firstAddress) }

        var cursor: UnsafeMutablePointer<ifaddrs>? = firstAddress
        while let current = cursor {
            let interface = current.pointee
            cursor = interface.ifa_next

            guard let socketAddress = interface.ifa_addr else { continue }
            let family = Int32(socketAddress.pointee.sa_family)
            guard family == AF_INET || family == AF_INET6 else { continue }

            var hostBuffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let result = getnameinfo(
                socketAddress,
                socklen_t(socketAddress.pointee.sa_len),
                &hostBuffer,
                socklen_t(hostBuffer.count),
                nil,
                0,
                NI_NUMERICHOST
            )
            guard result == 0 else { continue }
            guard String(cString: hostBuffer) == expectedAddress else { continue }
            return String(cString: interface.ifa_name)
        }
        return nil
    }
}
