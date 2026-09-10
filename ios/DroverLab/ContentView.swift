import SwiftUI
import UIKit

struct ContentView: View {
    @StateObject private var tunnel = TunnelManager()
    @StateObject private var trace = TraceStore()
    @State private var sharedTraceURL: URL?

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    labWarning
                    tunnelCard
                    traceCard
                    NavigationLink("Open-source licenses") {
                        LicenseView()
                    }
                }
                .padding()
            }
            .navigationTitle("Drover WhatsApp Lab")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Reload") {
                        tunnel.reload()
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
        .onAppear {
            tunnel.reload()
        }
        .sheet(
            isPresented: Binding(
                get: { sharedTraceURL != nil },
                set: { if !$0 { sharedTraceURL = nil } }
            )
        ) {
            if let url = sharedTraceURL {
                ActivityView(items: [url])
            }
        }
    }

    private var labWarning: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Development experiment", systemImage: "exclamationmark.triangle.fill")
                .font(.headline)
                .foregroundColor(.orange)
            Text("Observe-only mode adds no WhatsApp-specific preamble and does not intentionally alter datagram payload bytes. The userspace tunnel can still change source ports and timing. The trace never stores payloads, messages, microphone data, or audio.")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.orange.opacity(0.10))
        .cornerRadius(12)
    }

    private var tunnelCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Per-app tunnel")
                .font(.title2.bold())

            HStack {
                Circle()
                    .fill(tunnel.statusColor)
                    .frame(width: 10, height: 10)
                Text(tunnel.statusText)
                    .font(.headline)
            }

            Text(tunnel.detailText)
                .font(.footnote)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            Button {
                if tunnel.canStop {
                    tunnel.stop()
                } else {
                    tunnel.start()
                }
            } label: {
                Label(
                    tunnel.canStop ? "Stop observation" : "Start observation",
                    systemImage: tunnel.canStop ? "stop.fill" : "play.fill"
                )
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!tunnel.hasConfiguration && !tunnel.canStop)

            Button {
                openWhatsApp()
            } label: {
                Label("Open WhatsApp", systemImage: "message.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            if let errorText = tunnel.errorText {
                Text(errorText)
                    .font(.footnote)
                    .foregroundColor(.red)
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(12)
    }

    private var traceCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Diagnostic trace")
                .font(.title2.bold())

            Text(trace.summary)
                .font(.footnote)
                .foregroundColor(.secondary)

            ScrollView(.horizontal) {
                Text(trace.preview)
                    .font(.system(.caption, design: .monospaced))
                    .textSelection(.enabled)
                    .padding(10)
            }
            .frame(maxWidth: .infinity, minHeight: 100, alignment: .topLeading)
            .background(Color(uiColor: .tertiarySystemBackground))
            .cornerRadius(8)

            HStack {
                Button("Refresh trace") {
                    trace.refresh()
                }
                .disabled(!tunnel.canInspectTrace)

                Spacer()

                Button("Share JSONL") {
                    sharedTraceURL = trace.shareableURL
                }
                .disabled(trace.shareableURL == nil || !tunnel.canInspectTrace)
            }

            if !tunnel.canInspectTrace {
                Text("Stop observation before reading or sharing the trace.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(12)
    }

    private func openWhatsApp() {
        guard let url = URL(string: "whatsapp://") else { return }
        UIApplication.shared.open(url, options: [:]) { opened in
            if !opened, let storeURL = URL(string: "https://apps.apple.com/app/id310633997") {
                UIApplication.shared.open(storeURL)
            }
        }
    }
}

private struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
