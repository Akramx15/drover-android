# Native engine

Drover for Android bundles a Direct-only build of `tun2proxy` for three Android
ABIs. The build is pinned to the following inputs:

- Repository: <https://github.com/tun2proxy/tun2proxy>
- Commit: `fc77ca3182b3a63b84266bb0a5d24c096e022765`
- Crate version: `0.8.3`
- `ipstack` TCP-drop fix: `0f95edc89f23c6700e858eeb5120dd7f6dd1a1c7`
  from <https://github.com/narrowlink/ipstack>
- Rust: `1.97.1` (see `rust-toolchain.toml`)
- Android NDK: `26.1.10909125`
- Android API: `29`
- Targets: `aarch64-linux-android`, `armv7-linux-androideabi`, and
  `x86_64-linux-android`

`tun2proxy-drover.patch` contains the complete source delta. `Cargo.lock` pins
the dependency graph. The release build always uses `--frozen` and
`--no-default-features`; the latter excludes the SOCKS/HTTP proxy stack and its
`socks5-impl` dependency.

## Drover patch

The patch intentionally stays narrow:

1. It sends Drover's `00`, `01`, 50 ms compatibility sequence before the first
   matching 74-byte Discord UDP discovery packet for a virtual source socket.
2. It tracks that source across destination-specific `ipstack` sessions. Its
   last-seen timestamp is refreshed at most once every 30 seconds per session,
   instead of locking and writing the shared map for every datagram.
3. It preserves the configured network lifetimes. The Android app passes a
   120-second UDP timeout; the unchanged tun2proxy TCP default is 600 seconds.
4. It fixes the Android Tokio runtime at two asynchronous worker threads. The
   default runtime would otherwise scale its pool with the device CPU count.
   A one-worker candidate was measured but not accepted because full traffic
   stability was not demonstrated; two is the conservative minimum for this
   pre-release. The blocking pool is capped at one on-demand thread with a
   one-second idle lifetime rather than Tokio's large default cap.
5. It compiles traffic counters and their locks out of the Direct-only mobile
   build. The C callback symbol remains as a compatibility no-op. Enabling the
   `traffic-stats` feature restores the original counters.
6. It reports readiness through the stable JNI bridge only after the TUN,
   `ipstack`, trackers, and session task set are initialized. The app therefore
   never reports `RUNNING` or opens Discord before the packet loop is ready.
7. It owns TCP and UDP session tasks in a Tokio `JoinSet`. Completed task
   records are reaped as `select` events while the tunnel runs; the empty set
   is guarded, so this adds no background polling or busy loop. Shutdown still
   aborts and joins outstanding tasks while the runtime is alive, then drops
   `ipstack`.
   The pinned upstream `ipstack` fix replaces its two blocking TCP-drop waits
   with a non-blocking signal and task abort, preventing temporary replacement
   workers and shutdown deadlocks. A generation-owned RAII registry keeps the
   old cancellation token registered through complete runtime teardown; an
   already-cancelled replacement waits on a condition variable, so packet
   loops cannot overlap and an old cleanup cannot erase a new token. Android
   does not use tun2proxy's standalone-process emergency-exit timer.
8. It honors `SOURCE_DATE_EPOCH` and remapped source paths for reproducible
   release metadata.

## Rebuild all Android ABIs

The PowerShell script works under `pwsh` on Windows x64 and Linux x64. It
expects NDK `26.1.10909125` to be installed. When `rustup` is available it
installs the pinned Rust toolchain, `rustfmt`, and all three Android targets;
otherwise those exact components must already exist.

Start from a clean detached checkout of the pinned upstream commit:

```powershell
$ScratchRoot = Join-Path ([System.IO.Path]::GetTempPath()) 'drover-native-build'
$Tun2ProxyRoot = Join-Path $ScratchRoot 'tun2proxy'
git clone https://github.com/tun2proxy/tun2proxy.git $Tun2ProxyRoot
git -C $Tun2ProxyRoot checkout --detach fc77ca3182b3a63b84266bb0a5d24c096e022765

# Run this from the Drover for Android repository root.
pwsh ./native/build-android.ps1 `
  -SourceDir $Tun2ProxyRoot `
  -NdkRoot $env:ANDROID_NDK_ROOT
```

On Windows, omit `-NdkRoot` when the requested NDK is installed at
`%LOCALAPPDATA%\Android\Sdk\ndk\26.1.10909125`. On CI, install that NDK first
and pass `ANDROID_NDK_ROOT` explicitly.

The script performs formatting and host unit checks, applies the patch, copies
the pinned lockfile, and builds the following runtime files:

```text
app/src/main/jniLibs/arm64-v8a/libtun2proxy.so
app/src/main/jniLibs/armeabi-v7a/libtun2proxy.so
app/src/main/jniLibs/x86_64/libtun2proxy.so
```

It also extracts unstripped DWARF data before stripping the shipped libraries:

```text
app/build/native-symbols/arm64-v8a/libtun2proxy.so.dbg
app/build/native-symbols/armeabi-v7a/libtun2proxy.so.dbg
app/build/native-symbols/x86_64/libtun2proxy.so.dbg
```

`app/build/` is ignored by Git. Keep these symbol files as private CI
artifacts for native profiling; do not attach them to the public APK release.
Use `-SkipHostTests` only when the same patched source and feature combinations
have already passed in an earlier CI job.

## Verify the libraries

Use the NDK copy of `llvm-readelf`. Each `LOAD` row must report alignment
`0x4000`, and the machine must match the ABI (`AArch64`, `ARM`, or `X86-64`).

```powershell
$ReadElf = Join-Path $env:ANDROID_NDK_ROOT 'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf'
Get-ChildItem ./app/src/main/jniLibs/*/libtun2proxy.so | ForEach-Object {
  Write-Host "== $($_.Directory.Name) =="
  & $ReadElf -h $_.FullName | Select-String 'Class:|Machine:|Type:'
  & $ReadElf -lW $_.FullName | Select-String '^\s*LOAD\s'
  & $ReadElf --dyn-syms -W $_.FullName |
    Select-String 'Java_com_github_shadowsocks_bg_Tun2proxy_(run|stop)'
  Get-FileHash -Algorithm SHA256 $_.FullName
}
```

Windows users should replace `linux-x86_64/bin/llvm-readelf` with
`windows-x86_64/bin/llvm-readelf.exe`.

The verified release-library SHA-256 values produced by the pinned Windows x64
build are:

```text
arm64-v8a   3f1afb680a9174d4fa2a3817d86d1c730cd1b43fa45005386a1e2975a993f0cd
armeabi-v7a fc5b5637b047595c6561acbd3dc78f86fc4bc000d0424bdc1861b7a096a46992
x86_64      a66c4bf12352ad005e5119cc8e9306834ee7e6ca9b1de0ae3c97ef0d8e7becd3
```
