# Changelog

## 0.2.0 (pre-release) - 2026-09-05

- Rename the product to Drover for Android and move the application ID to
  `app.drover.android`.
- Support Android 10+ across ARM64, ARMv7, and x86_64 with Universal and
  ARM64-only release APKs.
- Add responsive phone, tablet, and headset layouts with English and Arabic
  localization and automatic RTL behavior.
- Add one-tap tunnel startup and Discord launch, including saved selection for
  Stable, PTB, and Canary installations.
- Add opt-in startup after reboot and expose support for Android Always-on VPN.
- Replace the 500 ms UI status poll with event-driven state updates.
- Bound the native async runtime, reduce idle bookkeeping, and keep release
  logging at warning level without shortening network timeouts.
- Open Discord only after a native readiness callback, and use structured
  session shutdown so repeated start/stop cycles do not leak Tokio workers.
- Reap completed native session tasks through runtime events so long-lived
  tunnels do not retain finished Tokio task records.
- Pin the upstream `ipstack` non-blocking TCP-drop fix so shutdown does not
  create temporary replacement workers.
- Serialize native tunnel generations through complete runtime teardown so a
  rapid restart cannot overlap packet loops or lose the active stop token.
- Keep Java's borrowed TUN descriptor alive until the native runtime has fully
  returned, preventing descriptor-number reuse during an immediate restart.
- Bundle the complete audited third-party license set in both APKs and expose
  it through an on-demand Open-source licenses screen.
- Enable R8 and resource shrinking for release builds and add a non-published
  profile build for performance inspection.
- Add signed GitHub pre-release automation with signature, ABI, 16 KiB page
  alignment, and SHA-256 verification.

Migration note: Android treats 0.2.0 as a new application. Uninstall
`app.drover.quest`, install `app.drover.android`, and approve VPN and
notification permissions again.

## 0.1.0-alpha - 2026-09-04

- Add a Quest 3 companion app for the official Discord Android app.
- Route only Discord through an on-device Android `VpnService`.
- Reproduce Drover's direct UDP preamble for the initial 74-byte voice packet.
- Keep TCP, DNS, and all other UDP traffic direct; no remote VPN server is used.
- Add start/stop controls, persistent VPN status, and Android 14 support.
- Bundle an ARM64 direct-only native engine with 16 KiB page alignment.

Known limitation: this is a debug-signed alpha build. Joining an actual Discord
voice call on the user's network remains the final acceptance test.
