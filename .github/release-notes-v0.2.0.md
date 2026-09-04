# Drover for Android 0.2.0

This is a pre-release for Android 10 and newer. Drover runs entirely on the
Android device and works with the official Discord Stable, PTB, and Canary
apps. It is unofficial and is not affiliated with Discord or Meta.

## Downloads

- **Not sure which file to use?** Download
  `Drover-for-Android-v0.2.0-universal.apk`. It supports ARM64, ARMv7, and
  x86_64.
- **Meta Quest 3 or another modern ARM64 device?** Download the smaller
  `Drover-for-Android-v0.2.0-arm64-v8a.apk`.

Both APKs are the same app and have the same signing certificate. Only the
bundled CPU architectures differ.

## Verification

```text
Drover-for-Android-v0.2.0-universal.apk
SHA-256: @UNIVERSAL_SHA256@

Drover-for-Android-v0.2.0-arm64-v8a.apk
SHA-256: @ARM64_SHA256@

Signing certificate SHA-256: @CERT_SHA256@
```

The release workflow verifies both APK signatures, the package ID and version,
the exact ABI set, ELF load-segment alignment, and 16 KiB APK zip alignment
before upload.

The complete license and notice text for the audited 172-package cross-host
direct-build set is bundled inside both APKs and can be opened from
**Open-source licenses** in the app. Bundled license asset SHA-256:
`@LICENSES_SHA256@`.
It also contains Rust 1.97.1's official standard-library notices because the
standard library is linked statically into the native engine.

## Important upgrade note

The package ID changed from `app.drover.quest` to `app.drover.android`.
Uninstall Drover Quest 0.1.0 before installing this release, then approve VPN
and notification permissions again.

Startup after reboot is opt-in. If you enable Android's system Always-on VPN,
keep **Block connections without VPN / Lockdown disabled** because Drover
intentionally routes only the selected Discord app.

An active VPN cannot consume literally zero CPU or RAM. This release removes
periodic UI polling and avoids wake locks, alarms, scheduled jobs, and
telemetry while preserving connection stability.

## Pre-release validation scope

The build, signature, package metadata, JNI exports, ABI contents, and 16 KiB
alignment are verified automatically. The release candidate also completed:

- 8 native Rust tests, including the Drover UDP sequence, tracker batching,
  task reaping, and empty-task-set no-busy-loop checks.
- API 29 x86_64 emulator tests for DNS, IPv4/IPv6 routes, direct TCP, and the
  exact `00`, `01`, then original 74-byte UDP sequence.
- Installation and Arabic/no-Discord behavior from the Universal APK on a
  physical Android 16 ARM64 phone.
- Official Discord Stable discovery and launch on a physical Quest 3, with
  the VPN restricted to Discord, plus 24 rapid stop/start cycles and 6 Discord
  process relaunches. The run stayed at no more than two Tokio workers and one
  Java caller, showed no overlapping native runtime, crash, ANR, `EBADF`, or
  monotonic PSS growth, and released the service, VPN, TUN, and native threads
  after stop.

This pre-release does **not** claim that the full physical-device acceptance
matrix is complete yet: a physical ARMv7 device, 20 real Discord voice joins,
a 30-minute voice call, and the complete 60-minute Quest 3 power/PSS soak still
require maintainer testing. Please file an issue with the Android version,
Discord edition, and device model if you find a regression.

Distribution is through GitHub Releases only; there is no Google Play or AAB
release.
