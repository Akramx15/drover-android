# Drover WhatsApp Lab v0.3.0-alpha.1

This is an **experimental Android-only pre-release**. It adds WhatsApp and
WhatsApp Business observation while preserving Drover's existing Discord mode.
WhatsApp traffic is forwarded without an application-specific packet change;
this build records bounded transport metadata for comparing a working call with
a failing call and does **not** yet claim to unblock WhatsApp calls.

Choose exactly one APK:

- **Universal:** `Drover-WhatsApp-Lab-v0.3.0-alpha.1-universal.apk` — recommended
  when you do not know the device architecture; supports ARM64, ARMv7, and
  x86_64.
- **ARM64:** `Drover-WhatsApp-Lab-v0.3.0-alpha.1-arm64-v8a.apk` — smaller build
  for Meta Quest 3 and most modern Android devices.

The lab uses package ID `app.drover.android.lab`, so it installs alongside
Drover for Android `0.2.0`. Follow the paired working/failing procedure in
[`LAB_TESTING.md`](https://github.com/Akramx15/drover-android/blob/v0.3.0-alpha.1/LAB_TESTING.md).
Export a trace before uninstalling the lab because uninstalling removes its
internal trace and settings.

The trace contains destination IP addresses and timing, which are private
network metadata. It does not contain packet payloads, DNS question names,
messages, contacts, microphone data, or call audio. Review it before sharing
and do not attach a personal trace to a public GitHub issue.

No IPA or signed iOS artifact is included in this release.

## Verification

```text
Drover-WhatsApp-Lab-v0.3.0-alpha.1-universal.apk  SHA-256: @UNIVERSAL_SHA256@
Drover-WhatsApp-Lab-v0.3.0-alpha.1-arm64-v8a.apk  SHA-256: @ARM64_SHA256@
Signing certificate SHA-256: @CERT_SHA256@
Bundled licenses SHA-256: @LICENSES_SHA256@
```
