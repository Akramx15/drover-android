# Native engine

The bundled `libtun2proxy.so` is built from:

- Repository: https://github.com/tun2proxy/tun2proxy
- Commit: `fc77ca3182b3a63b84266bb0a5d24c096e022765`
- Crate version: `0.8.3`
- Target: `aarch64-linux-android`
- Android NDK: `26.1.10909125`

Apply `tun2proxy-drover.patch` to that commit and use the included
`Cargo.lock`, then run Cargo for the `aarch64-linux-android` release target.
Copy the resulting `libtun2proxy.so` to
`app/src/main/jniLibs/arm64-v8a/`.

The patch has four focused changes:

1. It tracks the first datagram observed for each Discord UDP source endpoint
   and sends the Drover `00`, `01`, 50 ms sequence when that datagram is 74
   bytes and direct forwarding is selected. A source endpoint is kept alive
   for twice tun2proxy's UDP idle timeout; this is the closest socket identity
   available through an Android TUN interface.
2. It disables a standalone-process emergency-exit timer on Android, where
   the library is embedded in the app process.
3. It makes the SOCKS/HTTP proxy stack optional and keeps the Quest build in
   direct-only mode, so the bundled binary excludes `socks5-impl`.
4. It honors `SOURCE_DATE_EPOCH` and supports reproducible release metadata.

## Rebuild on Windows

Requirements: Rust stable, Android target `aarch64-linux-android`, and Android
NDK 26.1.10909125. From a sibling checkout of tun2proxy:

```powershell
git checkout fc77ca3182b3a63b84266bb0a5d24c096e022765
git apply ..\drover-quest\native\tun2proxy-drover.patch
Copy-Item ..\drover-quest\native\Cargo.lock .\Cargo.lock -Force
rustup target add aarch64-linux-android

$TaskNdk = "$env:LOCALAPPDATA\Android\Sdk\ndk\26.1.10909125"
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = "$TaskNdk\toolchains\llvm\prebuilt\windows-x86_64\bin\aarch64-linux-android29-clang.cmd"
$env:CC_aarch64_linux_android = $env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER
$env:AR_aarch64_linux_android = "$TaskNdk\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-ar.exe"
$NativeRoot = (Get-Location).Path
$TargetRoot = "$NativeRoot\target-android"
$TaskUserRoot = [Environment]::GetFolderPath("UserProfile")
$env:SOURCE_DATE_EPOCH = "1787541405"
$RemapFlags = @(
    "-Clink-arg=-Wl,-z,max-page-size=16384"
    "-Cdebuginfo=0"
    "-Cstrip=symbols"
    "--remap-path-prefix=$TaskUserRoot=/build/user"
    "--remap-path-prefix=$NativeRoot=/src/tun2proxy"
    "--remap-path-prefix=$TargetRoot=/build/target"
) -join "`u{1f}"
$env:CARGO_ENCODED_RUSTFLAGS = $RemapFlags
$env:CARGO_TARGET_DIR = $TargetRoot

cargo fmt --all -- --check
cargo test --no-default-features --lib drover_tests -- --test-threads=1
cargo build --frozen --no-default-features --target aarch64-linux-android --release --lib
Copy-Item "$TargetRoot\aarch64-linux-android\release\libtun2proxy.so" ..\drover-quest\app\src\main\jniLibs\arm64-v8a\libtun2proxy.so -Force
```

`--no-default-features` is required for the bundled Direct-only build. It
excludes tun2proxy's SOCKS/HTTP proxy implementation and its GPL-licensed
`socks5-impl` dependency; Drover Quest does not use those proxy modes.
Use `--frozen` for a release so Cargo cannot silently change the pinned lockfile.
