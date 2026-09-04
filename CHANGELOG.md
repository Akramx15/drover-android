# Changelog

## 0.1.0-alpha - 2026-09-04

- Add a Quest 3 companion app for the official Discord Android app.
- Route only Discord through an on-device Android `VpnService`.
- Reproduce Drover's direct UDP preamble for the initial 74-byte voice packet.
- Keep TCP, DNS, and all other UDP traffic direct; no remote VPN server is used.
- Add start/stop controls, persistent VPN status, and Android 14 support.
- Bundle an ARM64 direct-only native engine with 16 KiB page alignment.

Known limitation: this is a debug-signed alpha build. Joining an actual Discord
voice call on the user's network remains the final acceptance test.
