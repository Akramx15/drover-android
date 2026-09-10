#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <Apple-Team-ID> [export-directory]" >&2
  exit 2
fi
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "A Development-signed IPA must be archived with Xcode on macOS." >&2
  exit 2
fi

readonly TEAM_ID="$1"
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly PROJECT_PATH="$SCRIPT_DIR/DroverLab.xcodeproj"
readonly FRAMEWORK_PATH="$SCRIPT_DIR/Generated/tun2proxy.xcframework"
readonly ARCHIVE_PATH="$SCRIPT_DIR/build/DroverLab-development.xcarchive"
readonly EXPORT_PATH="${2:-$SCRIPT_DIR/build/development-export}"
readonly EXPORT_OPTIONS="$(mktemp -t drover-export-options)"

cleanup() {
  rm -f "$EXPORT_OPTIONS"
}
trap cleanup EXIT

if [[ ! "$TEAM_ID" =~ ^[A-Z0-9]{10}$ ]]; then
  echo "Apple Team ID must contain exactly 10 uppercase letters or digits." >&2
  exit 2
fi
if [[ ! -d "$FRAMEWORK_PATH" ]]; then
  echo "Build $FRAMEWORK_PATH first with native/build-apple.sh." >&2
  exit 1
fi
if [[ -e "$ARCHIVE_PATH" || -e "$EXPORT_PATH" ]]; then
  echo "Refusing to overwrite an existing archive or export directory." >&2
  echo "Archive: $ARCHIVE_PATH" >&2
  echo "Export:  $EXPORT_PATH" >&2
  exit 1
fi
if [[ "$(xcodegen --version)" != "Version: 2.46.0" && "$(xcodegen --version)" != "2.46.0" ]]; then
  echo "XcodeGen 2.46.0 is required." >&2
  exit 1
fi

pushd "$SCRIPT_DIR" >/dev/null
xcodegen generate --spec project.yml
popd >/dev/null

mkdir -p "$(dirname "$ARCHIVE_PATH")"
xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme DroverLab \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM="$TEAM_ID" \
  CODE_SIGN_STYLE=Automatic \
  archive

plutil -create xml1 "$EXPORT_OPTIONS"
plutil -insert method -string debugging "$EXPORT_OPTIONS"
plutil -insert signingStyle -string automatic "$EXPORT_OPTIONS"
plutil -insert teamID -string "$TEAM_ID" "$EXPORT_OPTIONS"
plutil -insert manageAppVersionAndBuildNumber -bool NO "$EXPORT_OPTIONS"

xcodebuild \
  -exportArchive \
  -archivePath "$ARCHIVE_PATH" \
  -exportPath "$EXPORT_PATH" \
  -exportOptionsPlist "$EXPORT_OPTIONS" \
  -allowProvisioningUpdates

ipa_count="$(find "$EXPORT_PATH" -maxdepth 1 -type f -name '*.ipa' | wc -l | tr -d '[:space:]')"
if [[ "$ipa_count" != "1" ]]; then
  echo "Expected exactly one IPA in $EXPORT_PATH." >&2
  exit 1
fi
ipa_file="$(find "$EXPORT_PATH" -maxdepth 1 -type f -name '*.ipa' -print -quit)"
shasum -a 256 "$ipa_file"
echo "Created Development-signed IPA: $ipa_file"
echo "Keep it private: its embedded provisioning profile identifies registered test devices."
