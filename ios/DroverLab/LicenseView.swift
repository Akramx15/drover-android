import SwiftUI

struct LicenseView: View {
    @State private var licenseText = "Loading…"

    var body: some View {
        ScrollView {
            Text(licenseText)
                .font(.system(.caption, design: .monospaced))
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
        }
        .navigationTitle("Open-source licenses")
        .onAppear(perform: loadLicenses)
    }

    private func loadLicenses() {
        guard licenseText == "Loading…" else { return }
        guard let url = Bundle.main.url(
            forResource: "third_party_licenses",
            withExtension: "txt"
        ) else {
            licenseText = "The bundled license file is missing."
            return
        }
        do {
            licenseText = try String(contentsOf: url, encoding: .utf8)
        } catch {
            licenseText = "Could not load bundled licenses: \(error.localizedDescription)"
        }
    }
}
