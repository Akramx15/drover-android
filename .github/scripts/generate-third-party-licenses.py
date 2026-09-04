#!/usr/bin/env python3
"""Generate the license bundle embedded in Drover's two APKs."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from html.parser import HTMLParser
from pathlib import Path


LICENSE_FILE = re.compile(
    r"^(?:licen[sc]e|copying|notice|copyright|unlicense)",
    re.IGNORECASE,
)
ANDROID_TARGET = "aarch64-linux-android"
RUST_VERSION = "1.97.1"
# The inventory is intentionally generated on the supported Windows build host.
# Linux release runners omit these host-only build dependencies. Keeping them in
# the bundle is harmless over-compliance, while this small allow-list lets CI
# prove that every dependency used on either supported host is covered.
ALLOWED_HOST_SPECIFIC_EXTRAS = {
    ("winapi-util", "0.1.11"),
    ("windows-link", "0.2.1"),
    ("windows-sys", "0.52.0"),
    ("windows-targets", "0.52.6"),
    ("windows_x86_64_msvc", "0.52.6"),
}


class RustCopyrightTextExtractor(HTMLParser):
    """Keep all legal wording while turning Rust's notice HTML into plain text."""

    BLOCK_TAGS = {
        "article",
        "body",
        "br",
        "details",
        "div",
        "h1",
        "h2",
        "h3",
        "h4",
        "li",
        "p",
        "pre",
        "section",
        "summary",
        "table",
        "tr",
        "ul",
    }

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_starttag(
        self, tag: str, attrs: list[tuple[str, str | None]]
    ) -> None:
        del attrs
        if tag == "li":
            self.parts.append("\n- ")
        elif tag in self.BLOCK_TAGS:
            self.parts.append("\n")
        elif tag in {"td", "th"}:
            self.parts.append("\t")

    def handle_endtag(self, tag: str) -> None:
        if tag in self.BLOCK_TAGS or tag in {"td", "th"}:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        self.parts.append(data)


def mit_notice(holder: str) -> str:
    return f"""MIT License

{holder}

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""


# A few crates declare a license but omit the workspace-level license file from
# their published .crate archive. These notices come from the exact VCS commit
# recorded in each archive's .cargo_vcs_info.json. tproxy-config and tun do not
# have a license file in that commit; their declared author/license metadata is
# paired with the canonical license text here.
LICENSE_OVERRIDES = {
    ("daemonize", "0.5.0"): mit_notice("Copyright (c) 2016 Fedor Gogolev"),
    ("etherparse", "0.20.3"): mit_notice("Copyright (c) 2024 Julian Schmid"),
    ("jni", "0.22.4"): mit_notice(
        "Copyright (c) 2016 Prevoty, Inc. and jni-rs contributors"
    ),
    ("jni-macros", "0.22.4"): mit_notice(
        "Copyright (c) 2016 Prevoty, Inc. and jni-rs contributors"
    ),
    ("jni-sys-macros", "0.4.1"): mit_notice(
        "Copyright (c) 2015 The rust-jni-sys Developers"
    ),
    ("tproxy-config", "7.0.7"): mit_notice(
        "Copyright (c) @ssrlive"
    ),
    (
        "tun",
        "0.8.14",
    ): """DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
Version 2, December 2004

Copyleft (ↄ) meh. <meh@schizofreni.co> | http://meh.schizofreni.co

Everyone is permitted to copy and distribute verbatim or modified copies of
this license document, and changing it is allowed as long as the name is
changed.

DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. You just DO WHAT THE FUCK YOU WANT TO.
""",
}


def command(*args: str) -> str:
    result = subprocess.run(
        args,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"Command failed ({result.returncode}): {' '.join(args)}\n"
            f"{result.stdout}{result.stderr}"
        )
    return result.stdout


def inventory_rows(path: Path) -> list[tuple[str, str, str, str]]:
    rows: list[tuple[str, str, str, str]] = []
    pattern = re.compile(r"^\| ([^|]+) \| ([^|]+) \| ([^|]+) \| ([^|]+) \|$")
    for line in path.read_text(encoding="utf-8").splitlines():
        match = pattern.match(line)
        if match and match.group(1) != "Package":
            values = [value.strip() for value in match.groups()]
            # The human-readable inventory annotates Git packages with their
            # locked revision after the Cargo semver. Metadata uses semver only.
            values[1] = values[1].split(maxsplit=1)[0]
            rows.append(tuple(values))
    if len(rows) != 172:
        raise RuntimeError(f"Expected 172 locked package rows, found {len(rows)}")
    return rows


def package_metadata(
    cargo: str, manifest: Path
) -> tuple[dict[tuple[str, str], Path], dict[tuple[str, str], str]]:
    metadata = json.loads(
        command(
            cargo,
            "metadata",
            "--format-version",
            "1",
            "--locked",
            "--offline",
            "--no-default-features",
            "--manifest-path",
            str(manifest),
        )
    )
    roots: dict[tuple[str, str], Path] = {}
    licenses: dict[tuple[str, str], str] = {}
    for package in metadata["packages"]:
        key = (package["name"], package["version"])
        root = Path(package["manifest_path"]).resolve().parent
        previous = roots.setdefault(key, root)
        if previous != root:
            raise RuntimeError(f"Ambiguous package source for {key}: {previous}, {root}")
        licenses[key] = package.get("license") or ""
    return roots, licenses


def resolved_tree_keys(cargo: str, manifest: Path) -> set[tuple[str, str]]:
    output = command(
        cargo,
        "tree",
        "--target",
        ANDROID_TARGET,
        "--no-default-features",
        "--locked",
        "--offline",
        "-e",
        "normal,build",
        "--prefix",
        "none",
        "--format",
        "{p}",
        "--manifest-path",
        str(manifest),
    )
    keys: set[tuple[str, str]] = set()
    for line in output.splitlines():
        match = re.match(r"^(\S+) v(\S+)", line)
        if match:
            keys.add((match.group(1), match.group(2)))
    if not keys:
        raise RuntimeError("Cargo returned an empty direct-only dependency tree")
    return keys


def verify_inventory(
    rows: list[tuple[str, str, str, str]],
    licenses: dict[tuple[str, str], str],
    tree_keys: set[tuple[str, str]],
) -> None:
    inventory_keys = {(name, version) for name, version, _, _ in rows}
    if len(inventory_keys) != len(rows):
        raise RuntimeError("Dependency inventory contains duplicate package/version rows")

    uncovered = tree_keys - inventory_keys
    if uncovered:
        raise RuntimeError(
            f"Dependency inventory omits current-host graph nodes: {sorted(uncovered)}"
        )
    unexpected_extras = (inventory_keys - tree_keys) - ALLOWED_HOST_SPECIFIC_EXTRAS
    if unexpected_extras:
        raise RuntimeError(
            "Dependency inventory has unexplained graph nodes: "
            f"{sorted(unexpected_extras)}"
        )

    for name, version, declared_license, _ in rows:
        key = (name, version)
        actual_license = licenses.get(key)
        if actual_license is None:
            raise RuntimeError(f"Cargo metadata omitted inventory package {key}")
        if actual_license != declared_license:
            raise RuntimeError(
                f"License metadata drift for {name} {version}: "
                f"inventory {declared_license!r}, Cargo {actual_license!r}"
            )


def normalize_notice(raw: bytes, source: Path) -> str:
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as error:
        raise RuntimeError(f"License file is not UTF-8: {source}") from error
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    return "\n".join(line.rstrip() for line in text.splitlines()).strip() + "\n"


def rust_standard_library_notice(rustc: str) -> str:
    version = command(rustc, "--version").strip()
    if not re.match(rf"^rustc {re.escape(RUST_VERSION)}\b", version):
        raise RuntimeError(f"Expected Rust {RUST_VERSION}, found {version!r}")
    sysroot = Path(command(rustc, "--print", "sysroot").strip())
    source = sysroot / "share" / "doc" / "rust" / "COPYRIGHT-library.html"
    if not source.is_file():
        raise RuntimeError(f"Rust standard-library notice is missing: {source}")
    try:
        html = source.read_text(encoding="utf-8")
    except UnicodeDecodeError as error:
        raise RuntimeError(f"Rust standard-library notice is not UTF-8: {source}") from error

    parser = RustCopyrightTextExtractor()
    parser.feed(html)
    parser.close()
    lines: list[str] = []
    previous_blank = True
    for raw_line in "".join(parser.parts).splitlines():
        line = re.sub(r"[ \t]+", " ", raw_line).strip()
        if line:
            lines.append(line)
            previous_blank = False
        elif not previous_blank:
            lines.append("")
            previous_blank = True
    text = "\n".join(lines).strip() + "\n"
    for required in (
        "Copyright notices for The Rust Standard Library",
        "The Rust Standard Library is dual-licensed under Apache 2.0 and MIT terms.",
        "Out-of-tree dependencies",
    ):
        if required not in text:
            raise RuntimeError(f"Rust standard-library notice omitted {required!r}")
    return text


def render(
    rows: list[tuple[str, str, str, str]],
    roots: dict[tuple[str, str], Path],
    project_license: str,
    rust_notice: str,
) -> str:
    sections = [
        "Drover for Android — bundled open-source licenses",
        "",
        "Generated deterministically from native/Cargo.lock and",
        "DEPENDENCY_LICENSES.md. Do not edit this generated file by hand.",
        "",
        f"Package count: {len(rows)}",
        f"Rust standard library: {RUST_VERSION} notices included",
        "The package's declared expression is shown before its preserved",
        "LICENSE, NOTICE, COPYING, COPYRIGHT, or approved override text.",
        "",
    ]
    separator = "=" * 78
    sections.extend(
        [
            separator,
            "Drover for Android",
            "Source: repository root LICENSE",
            "",
            "--- project license ---",
            project_license.rstrip(),
            "",
            separator,
            f"Rust Standard Library {RUST_VERSION}",
            "Source: rustc sysroot/share/doc/rust/COPYRIGHT-library.html",
            "",
            "--- official notice (HTML converted to readable plain text) ---",
            rust_notice.rstrip(),
            "",
        ]
    )
    for name, version, declared_license, source_url in rows:
        key = (name, version)
        root = roots.get(key)
        if root is None:
            raise RuntimeError(f"Cargo metadata omitted inventory package {name} {version}")
        files = sorted(
            (
                child
                for child in root.iterdir()
                if child.is_file() and LICENSE_FILE.match(child.name)
            ),
            key=lambda child: (child.name.casefold(), child.name),
        )
        notices: list[tuple[str, str]] = [
            (child.name, normalize_notice(child.read_bytes(), child)) for child in files
        ]
        if not notices:
            override = LICENSE_OVERRIDES.get(key)
            if override is None:
                raise RuntimeError(
                    f"{name} {version} has no packaged license file or reviewed override"
                )
            notices = [("reviewed license override", normalize_notice(override.encode(), root))]

        sections.extend(
            [
                separator,
                f"Package: {name} {version}",
                f"Declared license: {declared_license}",
                f"Source: {source_url}",
                "",
            ]
        )
        for label, notice in notices:
            sections.extend([f"--- {label} ---", notice.rstrip(), ""])
    return "\n".join(sections).rstrip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inventory", required=True, type=Path)
    parser.add_argument("--tun2proxy-manifest", required=True, type=Path)
    parser.add_argument("--project-license", required=True, type=Path)
    parser.add_argument("--cargo", default="cargo")
    parser.add_argument("--rustc", default="rustc")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    rows = inventory_rows(args.inventory)
    roots, licenses = package_metadata(args.cargo, args.tun2proxy_manifest)
    verify_inventory(
        rows,
        licenses,
        resolved_tree_keys(args.cargo, args.tun2proxy_manifest),
    )
    if not args.project_license.is_file():
        raise RuntimeError(f"Project license is missing: {args.project_license}")
    generated = render(
        rows,
        roots,
        normalize_notice(args.project_license.read_bytes(), args.project_license),
        rust_standard_library_notice(args.rustc),
    )

    if args.check:
        if not args.output.is_file():
            raise RuntimeError(f"Generated license asset is missing: {args.output}")
        actual = args.output.read_text(encoding="utf-8")
        if actual != generated:
            raise RuntimeError(
                "Bundled third-party license asset is stale; regenerate it with this script"
            )
        print(f"Verified {args.output} ({len(rows)} packages)")
        return

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(generated, encoding="utf-8", newline="\n")
    print(f"Wrote {args.output} ({len(rows)} packages, {len(generated)} characters)")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # Keep CI diagnostics concise and deterministic.
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
