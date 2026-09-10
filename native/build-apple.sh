#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <clean-tun2proxy-checkout> [output-xcframework]" >&2
  exit 2
fi

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "The Apple core must be built on macOS with Xcode installed." >&2
  exit 2
fi

readonly UPSTREAM_COMMIT="fc77ca3182b3a63b84266bb0a5d24c096e022765"
readonly RUST_VERSION="1.97.1"
readonly SOURCE_DATE_EPOCH_VALUE="1787541405"
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly SOURCE_ROOT="$(cd "$1" && pwd)"
readonly OUTPUT_PATH="${2:-$REPO_ROOT/ios/Generated/tun2proxy.xcframework}"
readonly TARGET_ROOT="$SOURCE_ROOT/target-drover-apple"
readonly HEADERS_DIR="$REPO_ROOT/ios/Tun2proxyHeaders"

if [[ "$(git -C "$SOURCE_ROOT" rev-parse HEAD)" != "$UPSTREAM_COMMIT" ]]; then
  echo "Expected tun2proxy commit $UPSTREAM_COMMIT." >&2
  exit 1
fi
if [[ -n "$(git -C "$SOURCE_ROOT" status --porcelain)" ]]; then
  echo "The tun2proxy checkout must be clean before applying the Drover patch." >&2
  exit 1
fi
if [[ -e "$OUTPUT_PATH" ]]; then
  echo "Output already exists; remove this generated path first: $OUTPUT_PATH" >&2
  exit 1
fi

git -C "$SOURCE_ROOT" apply --check "$SCRIPT_DIR/tun2proxy-drover.patch"
git -C "$SOURCE_ROOT" apply "$SCRIPT_DIR/tun2proxy-drover.patch"
cp "$SCRIPT_DIR/Cargo.lock" "$SOURCE_ROOT/Cargo.lock"

rustup toolchain install "$RUST_VERSION" --profile minimal --component rustfmt --no-self-update
rustup target add --toolchain "$RUST_VERSION" \
  aarch64-apple-ios \
  aarch64-apple-ios-sim \
  x86_64-apple-ios

export SOURCE_DATE_EPOCH="$SOURCE_DATE_EPOCH_VALUE"
export CARGO_TARGET_DIR="$TARGET_ROOT"
export IPHONEOS_DEPLOYMENT_TARGET="15.1"
printf -v CARGO_ENCODED_RUSTFLAGS '%s\x1f%s\x1f%s' \
  '-Cdebuginfo=1' \
  "--remap-path-prefix=$SOURCE_ROOT=/src/tun2proxy" \
  "--remap-path-prefix=$TARGET_ROOT=/build/target"
export CARGO_ENCODED_RUSTFLAGS

pushd "$SOURCE_ROOT" >/dev/null
cargo "+$RUST_VERSION" fetch --locked
cargo "+$RUST_VERSION" fmt --all -- --check
cargo "+$RUST_VERSION" test --frozen --no-default-features --lib drover_tests -- --test-threads=1
cargo "+$RUST_VERSION" test --frozen --no-default-features --lib lab_trace -- --test-threads=1
cargo "+$RUST_VERSION" build --frozen --no-default-features --release --lib --target aarch64-apple-ios
cargo "+$RUST_VERSION" build --frozen --no-default-features --release --lib --target aarch64-apple-ios-sim
cargo "+$RUST_VERSION" build --frozen --no-default-features --release --lib --target x86_64-apple-ios
popd >/dev/null

mkdir -p "$(dirname "$OUTPUT_PATH")"
lipo -create \
  "$TARGET_ROOT/aarch64-apple-ios-sim/release/libtun2proxy.a" \
  "$TARGET_ROOT/x86_64-apple-ios/release/libtun2proxy.a" \
  -output "$TARGET_ROOT/libtun2proxy-ios-simulator.a"
xcodebuild -create-xcframework \
  -library "$TARGET_ROOT/aarch64-apple-ios/release/libtun2proxy.a" \
  -headers "$HEADERS_DIR" \
  -library "$TARGET_ROOT/libtun2proxy-ios-simulator.a" \
  -headers "$HEADERS_DIR" \
  -output "$OUTPUT_PATH"

echo "Created $OUTPUT_PATH"
