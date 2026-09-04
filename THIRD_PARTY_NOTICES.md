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

The bundled native binary is built in direct-only mode with Cargo's
`--no-default-features` option. It does not contain tun2proxy's optional
SOCKS/HTTP proxy implementation or the GPL-licensed `socks5-impl` dependency.

The native binary also includes permissively licensed Rust dependencies from
the pinned upstream revision. Their exact versions are fixed by the
accompanying `native/Cargo.lock` file. `DEPENDENCY_LICENSES.md` records the
resolved Android direct-build graph and the license expression reported by
each package.
