import Combine
import Foundation

final class TraceStore: ObservableObject {
    @Published private(set) var summary = "No trace has been created yet."
    @Published private(set) var preview = "Start observation, place a test call, stop observation, then refresh."
    @Published private(set) var shareableURL: URL?

    func refresh() {
        guard let url = LabConstants.traceURL else {
            summary = "The shared App Group container is unavailable. Check signing and entitlements."
            preview = "No App Group container"
            shareableURL = nil
            return
        }

        guard let data = try? Data(contentsOf: url), !data.isEmpty else {
            summary = "No trace has been created at \(LabConstants.traceFileName)."
            preview = "Start observation, place a test call, stop observation, then refresh."
            shareableURL = nil
            return
        }

        let text = String(decoding: data, as: UTF8.self)
        let lines = text.split(separator: "\n", omittingEmptySubsequences: true)
        preview = lines.suffix(24).joined(separator: "\n")
        summary = "\(lines.count) JSONL events, \(ByteCountFormatter.string(fromByteCount: Int64(data.count), countStyle: .file)). Payload capture is disabled."
        shareableURL = url
    }
}
