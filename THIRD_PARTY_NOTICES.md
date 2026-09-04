# Third-party notices

## tun2proxy 0.8.3

Source: https://github.com/tun2proxy/tun2proxy  
Pinned commit: `fc77ca3182b3a63b84266bb0a5d24c096e022765`

MIT License

Copyright (c) @ssrlive, B. Blechschmidt and contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

The bundled Android native binaries are built in direct-only mode with Cargo's
`--no-default-features` option. It does not contain tun2proxy's optional
SOCKS/HTTP proxy implementation or the GPL-licensed `socks5-impl` dependency.

The native binary also includes permissively licensed Rust dependencies from
the pinned upstream revision. Their exact versions are fixed by the
accompanying `native/Cargo.lock` file. `DEPENDENCY_LICENSES.md` records the
172-node audited cross-host direct-build notice set and each package's declared
license expression. This deliberately includes five Windows-only build nodes
that are absent on the Linux release runner, so both supported build hosts are
covered.

The complete license, notice, copying, and copyright text preserved from that
locked graph is bundled in both APKs as
`assets/third_party_licenses.txt`. Users can read it without network access by
opening **Open-source licenses** in the app. The release workflow regenerates
the bundle from the patched, locked source and fails if the checked-in asset or
either APK differs. The generator's few reviewed overrides cover published
crate archives that declare a license but omit their repository-level license
file; they are documented in
`.github/scripts/generate-third-party-licenses.py`.

Because Rust's standard library is linked statically into the native engine,
the same asset also includes the official standard-library copyright and
third-party notices extracted from Rust 1.97.1's
`COPYRIGHT-library.html`. The exact Rust version is enforced during generation.

The `ipstack` dependency is pinned to upstream commit
`0f95edc89f23c6700e858eeb5120dd7f6dd1a1c7`, which contains the upstream
non-blocking TCP-session drop fix, rather than to a floating branch.
