# Drover for Android

[اقرأ هذا الدليل بالعربية](README.ar.md)

Drover for Android is an on-device companion for the official Discord Android
app. It applies the Drover UDP handshake locally through Android's `VpnService`;
it does not need a PC, DNS host, remote VPN server, or proxy.

This is an unofficial community project. It is not affiliated with or endorsed
by Discord or Meta.

## Download

Version 0.2.0 is distributed only through this repository's GitHub Releases.
There is no Google Play or AAB release.

| File | Use it when |
| --- | --- |
| `Drover-for-Android-v0.2.0-universal.apk` | You are unsure which CPU your device has. This is the recommended default and supports ARM64, ARMv7, and x86_64. |
| `Drover-for-Android-v0.2.0-arm64-v8a.apk` | You use a Meta Quest 3 or another modern ARM64 Android device and want the smaller download. |

Both packages require Android 10 or newer. Verify the APK SHA-256 checksum and
signing-certificate SHA-256 fingerprint against the values printed in the
GitHub release notes before installing it.
Android selects one matching native library from the Universal APK; it does
not load all three ABIs into memory. See Android's
[ABI packaging documentation](https://developer.android.com/build/configure-apk-splits).

The permanent v0.2.0 signing-certificate SHA-256 fingerprint is:

```text
aa7835a298807cde50c272419bf88230f05b044c1a5c32591de307b7aa9731d1
```

Do not install an APK presented as an official update if this fingerprint does
not match.

### Upgrading from Drover Quest 0.1.0

Version 0.2.0 has a new application ID: `app.drover.android`. Android therefore
treats it as a different app from `app.drover.quest`. Uninstall the old version
first, then install 0.2.0 and grant VPN and notification permission again:

```powershell
adb uninstall app.drover.quest
adb install Drover-for-Android-v0.2.0-universal.apk
```

Uninstalling deletes the old app's settings. Do not use `adb install -r` to
replace 0.1.0 because the package IDs differ.

## What it does

- Creates a local Android VPN restricted to the selected Discord package.
- Supports Discord Stable (`com.discord`), PTB (`com.discord.ptb`), and Canary
  (`com.discord.canary`). If several are installed, Drover asks which one to use
  and remembers the choice.
- Sends TCP, DNS, IPv4, IPv6, and ordinary UDP traffic directly to the internet.
- For the first 74-byte Discord UDP datagram, sends the Drover `00`, `01`, 50 ms
  sequence before forwarding the original packet. This matches Direct mode in
  [discord-drover](https://github.com/hdrover/discord-drover).
- Never decrypts Discord traffic and contains no analytics, telemetry, account,
  message, microphone, or audio access.
- Includes an offline **Open-source licenses** screen containing the preserved
  notices for the complete audited direct-build dependency set.

Drover is not a geographic or privacy VPN and does not hide your IP address.
Android permits only one active VPN at a time; starting another VPN stops
Drover.

## Use

1. Install the official Discord Android app and one of the APKs above.
2. Open **Drover for Android**. Select a Discord edition if prompted.
3. Tap **Start and open Discord**, then approve Android's VPN prompt the first
   time. Drover opens the selected Discord app after the tunnel is ready.
4. Keep Drover running while using Discord voice. The permanent Android
   notification lets you stop it without reopening the app.

Use **Open-source licenses** on the main screen to inspect the complete bundled
third-party notices. They are loaded only when that screen is opened, so the
large legal text has no idle CPU cost.

When the tunnel is already active, the main button changes to **Open Discord**.
If Discord is removed, Drover detects that the saved edition is unavailable and
asks you to install or select another one.

## Start after reboot and Always-on VPN

**Start Drover after reboot** is opt-in and off by default. Enable it only after
granting VPN permission. After a reboot Drover starts its foreground VPN
service without forcing a screen to open; Android may show its notification or
wait until the device has completed booting. If VPN approval was revoked,
tapping the notification opens Drover so you can approve it again.
On Android 13 and newer, allow notifications to receive that setup alert and
the foreground notification's **Stop** action. If you decline, the VPN still
works, but you must open Drover yourself when setup needs attention.

Drover also supports Android's system **Always-on VPN** setting on devices that
expose it. This is optional and independent of the in-app reboot setting.
The implementation follows Android's documented
[boot foreground-service exemptions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start),
[background activity restrictions](https://developer.android.com/guide/components/activities/secure-bal),
and [`VpnService` behavior](https://developer.android.com/reference/android/net/VpnService).

> Keep **Block connections without VPN** (also called Lockdown) **off**.
> Drover is deliberately a per-app VPN for Discord. Lockdown can block network
> access for applications that are outside Drover's VPN allow-list.

## Resource use

No active VPN can literally use zero CPU or RAM: Android must keep a foreground
service and the native packet loop alive while traffic is being routed. Drover
minimizes idle overhead by using event-driven UI updates, blocking network I/O,
two fixed Tokio asynchronous workers, and no wake locks, alarms, scheduled
jobs, background polling, or telemetry. The Java caller thread is separate
from those workers. Drover does not poll for the Discord process and will
continue running until you stop it or Android stops the VPN.

Before promotion from pre-release, acceptance should include CPU, thread, and
proportional-set-size checks on a Quest 3, plus long voice-call and repeated
start/stop tests. The release notes state the validation completed so far.
Functional stability takes priority over an optimization that causes packet
loss or voice dropouts.

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36
- Android NDK `26.1.10909125`
- Rust 1.97.1 with the Android targets used by `native/build-android.ps1`

AGP 8.11.1, Gradle 8.13, JDK 17, and API 36 match Android's official
[AGP 8.11 compatibility table](https://developer.android.com/build/releases/agp-8-11-0-release-notes).

On Windows, rebuild the pinned, direct-only native engine and then build both
debug distributions:

```powershell
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$Tun2proxySource = Join-Path ([IO.Path]::GetTempPath()) ("tun2proxy-" + [Guid]::NewGuid().ToString("N"))
git init $Tun2proxySource
git -C $Tun2proxySource remote add origin https://github.com/tun2proxy/tun2proxy.git
git -C $Tun2proxySource fetch --depth=1 origin fc77ca3182b3a63b84266bb0a5d24c096e022765
git -C $Tun2proxySource checkout --detach FETCH_HEAD
.\native\build-android.ps1 `
    -SourceDir $Tun2proxySource `
    -NdkRoot "$env:ANDROID_SDK_ROOT\ndk\26.1.10909125"
$Cargo197 = (rustup which --toolchain 1.97.1 cargo).Trim()
$Rustc197 = (rustup which --toolchain 1.97.1 rustc).Trim()
python .\.github\scripts\generate-third-party-licenses.py `
    --inventory .\DEPENDENCY_LICENSES.md `
    --tun2proxy-manifest "$Tun2proxySource\Cargo.toml" `
    --project-license .\LICENSE `
    --cargo $Cargo197 `
    --rustc $Rustc197 `
    --output .\app\src\main\assets\third_party_licenses.txt `
    --check
.\gradlew.bat lintUniversalDebug lintArm64Debug assembleUniversalDebug assembleArm64Debug
```

The native build uses `--frozen --no-default-features` for ARM64, ARMv7, and
x86_64 and copies the libraries into `app/src/main/jniLibs/`. See
[`native/README.md`](native/README.md) for the pinned source, patch, and
reproduction details.

Release builds read signing material only from environment variables:

```text
ANDROID_KEYSTORE_FILE
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The native script keeps separate DWARF files under `app/build/native-symbols/`.
The `profile` build type is debug-signable and keeps JNI debugging enabled for
local measurement. The release workflow stores those symbols as a separate
short-lived Actions artifact, never as a public release asset. GitHub Releases
contain only the Universal and ARM64 APKs after signature, ABI, and 16 KiB
alignment checks, following Android's
[16 KiB page-size guidance](https://developer.android.com/guide/practices/page-sizes).

## Maintainer signing warning

GitHub release automation expects the Base64-encoded permanent keystore and its
credentials in repository Actions secrets named:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

GitHub does not allow a secret value to be read back after it is stored. Under
the project's no-local-backup policy, deleting or losing these secrets
permanently removes the ability to publish an update signed as the same app.
Never print the keystore, passwords, or Base64 value in CI logs.

## Project owners and contributors

1. [Akramx15](https://github.com/Akramx15) — project owner, concept, and direction.
2. [AhmadotEng](https://github.com/AhmadotEng) — project contributor.
3. OpenAI Codex — design, implementation, and technical review assistance.

Licensed under the MIT License. See [`LICENSE`](LICENSE),
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md), and
[`DEPENDENCY_LICENSES.md`](DEPENDENCY_LICENSES.md).
