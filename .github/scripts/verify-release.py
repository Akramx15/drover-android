#!/usr/bin/env python3
"""Fail closed unless both Drover Android release APKs are publishable."""

from __future__ import annotations

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


EXPECTED_FILES = {
    "universal": "Drover-for-Android-v0.2.0-universal.apk",
    "arm64": "Drover-for-Android-v0.2.0-arm64-v8a.apk",
}
EXPECTED_ABIS = {
    "universal": {"arm64-v8a", "armeabi-v7a", "x86_64"},
    "arm64": {"arm64-v8a"},
}
EXPECTED_APPLICATION_ID = "app.drover.android"
EXPECTED_VERSION_CODE = "2"
EXPECTED_VERSION_NAME = "0.2.0"
EXPECTED_MIN_SDK = "29"
EXPECTED_TARGET_SDK = "36"
LICENSE_ASSET_ENTRY = "assets/third_party_licenses.txt"
EXPECTED_LICENSE_PACKAGE_COUNT = 172
MINIMUM_LOAD_ALIGNMENT = 0x4000
EXPECTED_ELF_MACHINES = {
    "arm64-v8a": "AArch64",
    "armeabi-v7a": "ARM",
    "x86_64": "Advanced Micro Devices X86-64",
}
EXPECTED_JNI_SYMBOLS = {
    "Java_com_github_shadowsocks_bg_Tun2proxy_run",
    "Java_com_github_shadowsocks_bg_Tun2proxy_stop",
}


def command(*args: str) -> str:
    result = subprocess.run(
        args,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"Command failed ({result.returncode}): {' '.join(args)}\n{result.stdout}"
        )
    return result.stdout


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verify_manifest(aapt2: str, apk: Path) -> None:
    badging = command(aapt2, "dump", "badging", str(apk))
    package_line = next(
        (line for line in badging.splitlines() if line.startswith("package: ")),
        "",
    )
    application_id = re.search(r"\bname='([^']+)'", package_line)
    version_code = re.search(r"\bversionCode='([^']+)'", package_line)
    version_name = re.search(r"\bversionName='([^']+)'", package_line)
    if application_id is None or application_id.group(1) != EXPECTED_APPLICATION_ID:
        raise RuntimeError(f"{apk.name}: unexpected application ID in {package_line!r}")
    if version_name is None or version_name.group(1) != EXPECTED_VERSION_NAME:
        raise RuntimeError(f"{apk.name}: unexpected version name in {package_line!r}")
    if version_code is None or version_code.group(1) != EXPECTED_VERSION_CODE:
        raise RuntimeError(f"{apk.name}: unexpected version code in {package_line!r}")

    sdk_match = re.search(r"(?m)^minSdkVersion:'([^']+)'$", badging)
    target_match = re.search(r"(?m)^targetSdkVersion:'([^']+)'$", badging)
    if sdk_match is None or sdk_match.group(1) != EXPECTED_MIN_SDK:
        raise RuntimeError(f"{apk.name}: expected minSdk {EXPECTED_MIN_SDK}")
    if target_match is None or target_match.group(1) != EXPECTED_TARGET_SDK:
        raise RuntimeError(f"{apk.name}: expected targetSdk {EXPECTED_TARGET_SDK}")


def verify_signature(apksigner: str, apk: Path) -> str:
    report = command(apksigner, "verify", "--verbose", "--print-certs", str(apk))
    if not re.search(r"Verified using v[23] scheme[^:]*:\s*true", report):
        raise RuntimeError(f"{apk.name}: neither APK Signature Scheme v2 nor v3 verified")
    if re.search(r"CN\s*=\s*Android Debug", report, flags=re.IGNORECASE):
        raise RuntimeError(f"{apk.name}: debug signing certificate is not publishable")
    match = re.search(
        r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]{64})",
        report,
    )
    if match is None:
        raise RuntimeError(f"{apk.name}: missing signing-certificate SHA-256 digest")
    return match.group(1).lower()


def native_entries(archive: zipfile.ZipFile) -> list[str]:
    return [
        name
        for name in archive.namelist()
        if re.fullmatch(r"lib/[^/]+/[^/]+\.so", name)
    ]


def validate_license_bundle(data: bytes, source: str) -> str:
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError as error:
        raise RuntimeError(f"{source}: bundled license asset is not UTF-8") from error

    # Git may present text with platform-native newlines in an existing working
    # tree. Marker validation is textual; the exact byte-for-byte comparison
    # between the checked-in asset and each APK remains enforced separately.
    normalized_text = text.replace("\r\n", "\n").replace("\r", "\n")

    expected_header = f"Package count: {EXPECTED_LICENSE_PACKAGE_COUNT}"
    if expected_header not in normalized_text:
        raise RuntimeError(f"{source}: missing {expected_header!r}")
    actual_count = len(re.findall(r"(?m)^Package: ", normalized_text))
    if actual_count != EXPECTED_LICENSE_PACKAGE_COUNT:
        raise RuntimeError(
            f"{source}: expected {EXPECTED_LICENSE_PACKAGE_COUNT} license sections, "
            f"found {actual_count}"
        )
    for required in (
        "Package: tun2proxy 0.8.3",
        "Package: ipstack 1.0.2-dev",
        "Drover for Android\nSource: repository root LICENSE",
        "Copyright (c) 2026 Akramx15",
        "Rust Standard Library 1.97.1",
        "Copyright notices for The Rust Standard Library",
        "Apache License",
    ):
        if required not in normalized_text:
            raise RuntimeError(f"{source}: bundled license asset is missing {required!r}")
    if re.search(r"(?m)^Package: socks5-impl\b", normalized_text):
        raise RuntimeError(f"{source}: disabled socks5-impl appeared in the bundle")
    return hashlib.sha256(data).hexdigest()


def verify_license_asset(apk: Path, expected_data: bytes) -> str:
    with zipfile.ZipFile(apk) as archive:
        try:
            actual_data = archive.read(LICENSE_ASSET_ENTRY)
        except KeyError as error:
            raise RuntimeError(
                f"{apk.name}: missing {LICENSE_ASSET_ENTRY}"
            ) from error
    expected_hash = validate_license_bundle(expected_data, "checked-in license asset")
    actual_hash = validate_license_bundle(actual_data, apk.name)
    if actual_data != expected_data:
        raise RuntimeError(
            f"{apk.name}: bundled license asset differs from the checked-in asset "
            f"(expected {expected_hash}, found {actual_hash})"
        )
    return actual_hash


def verify_native_layout(
    readelf: str,
    apk: Path,
    expected_abis: set[str],
) -> None:
    with zipfile.ZipFile(apk) as archive, tempfile.TemporaryDirectory() as temp:
        entries = native_entries(archive)
        present_abis = {entry.split("/")[1] for entry in entries}
        if present_abis != expected_abis:
            raise RuntimeError(
                f"{apk.name}: expected ABIs {sorted(expected_abis)}, "
                f"found {sorted(present_abis)}"
            )

        expected_engine_entries = {
            f"lib/{abi}/libtun2proxy.so" for abi in expected_abis
        }
        if set(entries) != expected_engine_entries:
            raise RuntimeError(
                f"{apk.name}: expected native entries {sorted(expected_engine_entries)}, "
                f"found {sorted(entries)}"
            )

        temp_root = Path(temp)
        for entry in entries:
            extracted = temp_root / entry
            extracted.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(entry) as source, extracted.open("wb") as target:
                shutil.copyfileobj(source, target)

            program_headers = command(readelf, "-lW", str(extracted))
            alignments = []
            for line in program_headers.splitlines():
                fields = line.split()
                if fields and fields[0] == "LOAD":
                    try:
                        alignments.append(int(fields[-1], 0))
                    except ValueError as error:
                        raise RuntimeError(
                            f"{apk.name}: cannot parse ELF alignment for {entry}: {line}"
                        ) from error
            if not alignments:
                raise RuntimeError(f"{apk.name}: no ELF LOAD segments found in {entry}")
            if min(alignments) < MINIMUM_LOAD_ALIGNMENT:
                rendered = ", ".join(hex(value) for value in alignments)
                raise RuntimeError(
                    f"{apk.name}: {entry} has LOAD alignment below 16 KiB: {rendered}"
                )

            abi = entry.split("/")[1]
            elf_header = command(readelf, "-hW", str(extracted))
            machine_match = re.search(r"(?m)^\s*Machine:\s*(.+?)\s*$", elf_header)
            expected_machine = EXPECTED_ELF_MACHINES[abi]
            if machine_match is None or machine_match.group(1) != expected_machine:
                actual = machine_match.group(1) if machine_match else "missing"
                raise RuntimeError(
                    f"{apk.name}: {entry} expected ELF machine "
                    f"{expected_machine!r}, found {actual!r}"
                )

            dynamic_symbols = command(readelf, "--dyn-syms", "-W", str(extracted))
            missing_symbols = {
                symbol for symbol in EXPECTED_JNI_SYMBOLS if symbol not in dynamic_symbols
            }
            if missing_symbols:
                raise RuntimeError(
                    f"{apk.name}: {entry} missing JNI symbols {sorted(missing_symbols)}"
                )


def verify_archive_alignment(zipalign: str, apk: Path) -> None:
    command(zipalign, "-c", "-P", "16", "-v", "4", str(apk))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--universal", required=True, type=Path)
    parser.add_argument("--arm64", required=True, type=Path)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--aapt2", required=True)
    parser.add_argument("--zipalign", required=True)
    parser.add_argument("--readelf", required=True)
    parser.add_argument("--expected-cert-sha256", required=True)
    parser.add_argument("--license-asset", required=True, type=Path)
    parser.add_argument("--metadata-out", required=True, type=Path)
    args = parser.parse_args()

    if not args.license_asset.is_file():
        raise RuntimeError(f"Missing checked-in license asset: {args.license_asset}")
    expected_license_data = args.license_asset.read_bytes()
    validate_license_bundle(expected_license_data, str(args.license_asset))

    apks = {"universal": args.universal, "arm64": args.arm64}
    certificates: dict[str, str] = {}
    hashes: dict[str, str] = {}
    license_hashes: dict[str, str] = {}

    for distribution, apk in apks.items():
        if not apk.is_file():
            raise RuntimeError(f"Missing release APK: {apk}")
        if apk.name != EXPECTED_FILES[distribution]:
            raise RuntimeError(
                f"Unexpected asset name {apk.name!r}; "
                f"expected {EXPECTED_FILES[distribution]!r}"
            )
        verify_manifest(args.aapt2, apk)
        certificates[distribution] = verify_signature(args.apksigner, apk)
        verify_archive_alignment(args.zipalign, apk)
        verify_native_layout(args.readelf, apk, EXPECTED_ABIS[distribution])
        license_hashes[distribution] = verify_license_asset(
            apk, expected_license_data
        )
        hashes[distribution] = sha256(apk)

    if len(set(certificates.values())) != 1:
        raise RuntimeError("The two APKs are signed by different certificates")
    if len(set(license_hashes.values())) != 1:
        raise RuntimeError("The two APKs contain different license bundles")
    expected_certificate = args.expected_cert_sha256.strip().lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_certificate):
        raise RuntimeError("Expected signing-certificate SHA-256 is not 64 hex digits")
    actual_certificate = certificates["universal"]
    if actual_certificate != expected_certificate:
        raise RuntimeError(
            "Signing-certificate SHA-256 does not match the checked-in release identity: "
            f"expected {expected_certificate}, found {actual_certificate}"
        )

    args.metadata_out.parent.mkdir(parents=True, exist_ok=True)
    args.metadata_out.write_text(
        "\n".join(
            [
                f"UNIVERSAL_SHA256={hashes['universal']}",
                f"ARM64_SHA256={hashes['arm64']}",
                f"CERT_SHA256={actual_certificate}",
                f"LICENSES_SHA256={license_hashes['universal']}",
                "",
            ]
        ),
        encoding="utf-8",
    )
    print("Release APK verification passed.")
    print(f"Universal SHA-256: {hashes['universal']}")
    print(f"ARM64 SHA-256: {hashes['arm64']}")
    print(f"Signing certificate SHA-256: {actual_certificate}")
    print(f"Bundled licenses SHA-256: {license_hashes['universal']}")


if __name__ == "__main__":
    main()
