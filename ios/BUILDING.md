# Drover WhatsApp Lab for iOS

This directory is a development-only, observe-only proof of concept. It uses an
iOS Packet Tunnel Provider to pass WhatsApp traffic through the direct
`tun2proxy` engine while writing bounded transport metadata to the shared App
Group file `drover-lab-trace.jsonl`. The native strategy is fixed to
`--drover-strategy none`; it does not apply the Discord preamble or
intentionally alter application datagram payload bytes. This remains a
userspace tunnel, so source ports and transport timing can change.

The project is intentionally not signed and does not contain an IPA, certificate,
private key, provisioning profile, or compiled XCFramework.

## Requirements

- A Mac with a current Xcode release and the iOS 15.1+ SDK.
- XcodeGen **2.46.0** (`xcodegen --version` must report `2.46.0`).
- Rust 1.97.1. The build script installs the device ARM64 and Simulator
  ARM64/x86_64 targets for that pinned toolchain.
- An active Apple Developer Program team. A certificate by itself is not
  sufficient; the provisioning profiles must contain the Network Extension and
  App Groups entitlements.
- A physical iPhone running iOS 15.1 or newer with Developer Mode enabled and
  its UDID registered to the development team.

`NETestAppMapping` only takes effect in a **Development-provisioned** build.
Ad Hoc, TestFlight, App Store, and other Distribution-provisioned builds ignore
it. The resulting development IPA can only be installed on devices included in
its development provisioning profile.

## 1. Register identifiers and capabilities

Create these explicit identifiers in Certificates, Identifiers & Profiles:

- App: `app.drover.ios.lab`
- Packet Tunnel extension: `app.drover.ios.lab.PacketTunnel`
- App Group: `group.app.drover.ios.lab`

Enable **Network Extensions > Packet Tunnel** and **App Groups** for the app and
extension identifiers, and attach the App Group to both. If these placeholder
identifiers are not available to the team, replace them consistently in:

- `project.yml`
- both `.entitlements` files
- `Shared/LabConstants.swift`
- `DroverLab/Info.plist`
- `Config/WhatsAppPerAppVPN.mobileconfig.template`

The UUID `1D4D3F5C-8948-4B6D-9D3C-62C9C22E181B` must remain identical between
the `NETestAppMapping` dictionary and the profile's `VPNUUID`.

## 2. Build the patched native XCFramework

Run this from the repository root on macOS. Use a fresh detached checkout so the
patch and lockfile are the exact inputs tracked by this repository. The build
script creates a device slice plus a universal Apple Silicon/Intel Simulator
slice at `ios/Generated/tun2proxy.xcframework`.

```sh
mkdir -p .work/ios-native
git clone https://github.com/tun2proxy/tun2proxy.git .work/ios-native/tun2proxy
git -C .work/ios-native/tun2proxy checkout --detach fc77ca3182b3a63b84266bb0a5d24c096e022765
rm -rf ios/Generated/tun2proxy.xcframework
./native/build-apple.sh .work/ios-native/tun2proxy
```

Do not substitute the upstream prebuilt XCFramework: it does not contain this
repository's strategy gate, bounded lab trace, lifecycle fixes, or pinned
dependency graph. Keep `--no-default-features`; enabling the upstream proxy
feature changes both behavior and the dependency-license set.

Before using the extension, verify that the patched native lifecycle treats iOS
as an embedded mobile target: it must not start tun2proxy's standalone-process
emergency `std::process::exit` timer. The Swift provider never calls `exit()`.
The provider also waits for the native ready callback before completing
`startTunnel`; a connected status therefore means the packet loop is initialized.

## 3. Generate the Xcode project

```sh
cd ios
xcodegen generate --spec project.yml
open DroverLab.xcodeproj
```

An unsigned Simulator build suitable for CI is:

```sh
xcodebuild \
  -project DroverLab.xcodeproj \
  -scheme DroverLab \
  -configuration Debug \
  -sdk iphonesimulator \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Select the developer team for both targets. Confirm that automatic signing
creates Development profiles for both bundle IDs and that the entitlements in
the signed app and extension match the files in this directory.

Build and run `DroverLab` on the registered physical iPhone. A simulator cannot
exercise the real Packet Tunnel Provider path used by this experiment.

After both targets sign and run successfully, the tester can export a private
Development IPA with the certificate already stored in their Mac keychain and
their Apple account signed in to Xcode:

```sh
./export-development-ipa.sh YOUR10CHARTEAMID
```

The script uses Xcode automatic signing and `-allowProvisioningUpdates`; it
never reads or uploads a `.p12` private key. The registered iPhone must already
belong to the team. Keep the resulting IPA private because its embedded
provisioning profile identifies the registered test devices.

## 4. Install the development per-app profile

Copy the template without modifying the tracked file:

```sh
cp Config/WhatsAppPerAppVPN.mobileconfig.template \
   Config/WhatsAppPerAppVPN.mobileconfig
```

If the placeholder bundle identifiers were changed, update the copy. Transfer
the `.mobileconfig` to the test iPhone and install it from Settings. iOS will
show that this is an unsigned development profile; remove it after testing.

The host `Info.plist` maps both official iOS bundle identifiers:

- WhatsApp Messenger: `net.whatsapp.WhatsApp`
- WhatsApp Business: `net.whatsapp.WhatsAppSMB`

Open Drover Lab and tap **Reload**. The profile should appear as a disconnected
packet tunnel. Tap **Start observation**, then open WhatsApp and place one short
test call. Stop observation before sharing the trace. Both on-demand switches in
the template are disabled so opening WhatsApp alone never starts metadata
collection; observation begins only after the tester explicitly taps Start.

## 5. Inspect and share the trace

The extension creates this file inside the shared App Group container:

```text
drover-lab-trace.jsonl
```

Before native observation starts, the provider applies iOS data protection and
marks this file as excluded from device and iCloud backups. It verifies the
backup exclusion again before reporting the tunnel ready; failure aborts the
start instead of collecting metadata into an unprotected file.

Use **Refresh trace** and **Share JSONL** in the host app. The Rust writer is
bounded to 1 MiB and records only the schema marker, monotonic timing, transport,
destination address and port, direction, and packet length. It never records
packet payloads, DNS question names, messages, microphone data, or call audio.
Review the JSONL before sharing it because destination IP addresses are still
network metadata.

The host app also bundles the audited Android/Apple dependency notice set.
Open **Open-source licenses** in Drover Lab to inspect it offline.

## Current PoC limitations

- The tunnel is observe-only and is not expected to unblock a WhatsApp call.
- Observe-only mode preserves the app's original private DNS destination. The
  native core only retains tun2proxy's private-DNS rewrite for the explicitly
  selected Discord strategy.
- Per-app selection is development-only unless the device and WhatsApp are
  managed by an MDM service.
- The provider follows tun2proxy's current iOS 15+ integration approach: after
  applying network settings it resolves the assigned address to its `utun`
  interface and finds the corresponding open descriptor with Darwin
  `getsockopt`. NetworkExtension does not expose that descriptor as a public
  Swift property, so this path must be treated as experimental and retested on
  every supported iOS release.
- The app cannot be signed or archived on Windows. Build the native library and
  Xcode targets on macOS.
- An IPA exported with a Development profile needs Developer Mode and a
  registered device. Prefer letting the tester select their own team and build
  locally; never send a certificate private key or `.p12` through chat.
