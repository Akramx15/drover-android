# tun2proxy Android direct-only dependency licenses

Generated locally on 2026-09-04 from the sibling tun2proxy checkout's Cargo.toml and Cargo.lock. The resolved scope is the aarch64-linux-android target with default features disabled and Cargo normal plus build edges:

~~~text
cargo tree --target aarch64-linux-android --no-default-features --locked -e normal,build
~~~

The inventory was resolved with Cargo 1.97.1 on the x86_64-pc-windows-msvc host. It includes host-side build dependencies, but excludes dev dependencies, target-inapplicable packages, and disabled optional features. All 169 third-party artifacts resolve from the crates.io registry; there are no Git or additional path dependencies. The final column uses Cargo's declared repository, falling back to the declared homepage for udp-stream.

- Manifest SHA-256: 03C6F9EB7E67ACC906226B91C69547B069606863DA2D7F3EEC37AC3819B594DE
- Lockfile SHA-256: 74061572B129E82AA3E862ECF5FDA098C4C35CEBA97CA18B4A0CC3C8BCB49583
- Resolved package/version nodes: 170 total (tun2proxy plus 169 third-party nodes)
- Unknown license entries: none
- Copyleft license entries: none
- socks5-impl 0.9.6 (GPL-3.0-or-later) is not in this direct-only graph.
- Policy note: tun 0.8.14 is WTFPL-only. The legacy MIT/Apache-2.0 and Unlicense/MIT spellings are declared dual licenses but are not modern SPDX expressions.

| Package | Version | License | Repository / source |
|---|---:|---|---|
| ahash | 0.8.12 | MIT OR Apache-2.0 | https://github.com/tkaitchuck/ahash |
| aho-corasick | 1.1.5 | Unlicense OR MIT | https://github.com/BurntSushi/aho-corasick |
| android_log-sys | 0.3.2 | MIT OR Apache-2.0 | https://github.com/rust-mobile/android_log-sys-rs |
| android_logger | 0.15.1 | MIT OR Apache-2.0 | https://github.com/rust-mobile/android_logger-rs |
| android_system_properties | 0.1.6 | MIT OR Apache-2.0 | https://github.com/nical/android_system_properties |
| anstream | 1.0.0 | MIT OR Apache-2.0 | https://github.com/rust-cli/anstyle.git |
| anstyle | 1.0.14 | MIT OR Apache-2.0 | https://github.com/rust-cli/anstyle.git |
| anstyle-parse | 1.0.0 | MIT OR Apache-2.0 | https://github.com/rust-cli/anstyle.git |
| anstyle-query | 1.1.5 | MIT OR Apache-2.0 | https://github.com/rust-cli/anstyle.git |
| arrayvec | 0.7.8 | MIT OR Apache-2.0 | https://github.com/bluss/arrayvec |
| async-trait | 0.1.92 | MIT OR Apache-2.0 | https://github.com/dtolnay/async-trait |
| autocfg | 1.5.1 | Apache-2.0 OR MIT | https://github.com/cuviper/autocfg |
| base64easy | 0.1.7 | MIT OR Apache-2.0 | https://github.com/ssrlive/base64easy |
| bitflags | 2.13.1 | MIT OR Apache-2.0 | https://github.com/bitflags/bitflags |
| block-buffer | 0.10.4 | MIT OR Apache-2.0 | https://github.com/RustCrypto/utils |
| bytes | 1.12.1 | MIT | https://github.com/tokio-rs/bytes |
| cfg_aliases | 0.2.2 | MIT | https://github.com/katharostech/cfg_aliases |
| cfg-if | 1.0.4 | MIT OR Apache-2.0 | https://github.com/rust-lang/cfg-if |
| chacha20 | 0.10.2 | MIT OR Apache-2.0 | https://github.com/RustCrypto/stream-ciphers |
| chrono | 0.4.45 | MIT OR Apache-2.0 | https://github.com/chronotope/chrono |
| cidr | 0.3.2 | MIT | https://github.com/stbuehler/rust-cidr |
| clap | 4.6.6 | MIT OR Apache-2.0 | https://github.com/clap-rs/clap |
| clap_builder | 4.6.6 | MIT OR Apache-2.0 | https://github.com/clap-rs/clap |
| clap_derive | 4.6.4 | MIT OR Apache-2.0 | https://github.com/clap-rs/clap |
| clap_lex | 1.1.0 | MIT OR Apache-2.0 | https://github.com/clap-rs/clap |
| colorchoice | 1.0.5 | MIT OR Apache-2.0 | https://github.com/rust-cli/anstyle.git |
| combine | 4.6.8 | MIT | https://github.com/Marwes/combine |
| cpufeatures | 0.2.17 | MIT OR Apache-2.0 | https://github.com/RustCrypto/utils |
| critical-section | 1.2.0 | MIT OR Apache-2.0 | https://github.com/rust-embedded/critical-section |
| crypto-common | 0.1.7 | MIT OR Apache-2.0 | https://github.com/RustCrypto/traits |
| ctrlc2 | 4.0.0 | MIT/Apache-2.0 | https://github.com/ssrlive/ctrlc2.git |
| daemonize | 0.5.0 | MIT/Apache-2.0 | https://github.com/knsd/daemonize |
| data-encoding | 2.11.1 | MIT | https://github.com/ia0/data-encoding |
| digest | 0.10.7 | MIT OR Apache-2.0 | https://github.com/RustCrypto/traits |
| digest_auth | 0.3.1 | MIT | https://git.ondrovo.com/packages/digest_auth_rs |
| displaydoc | 0.2.7 | MIT OR Apache-2.0 | https://github.com/yaahc/displaydoc |
| dotenvy | 0.15.7 | MIT | https://github.com/allan2/dotenvy |
| env_filter | 0.1.4 | MIT OR Apache-2.0 | https://github.com/rust-cli/env_logger |
| env_filter | 2.0.0 | MIT OR Apache-2.0 | https://github.com/rust-cli/env_logger |
| env_logger | 0.11.11 | MIT OR Apache-2.0 | https://github.com/rust-cli/env_logger |
| errno | 0.3.14 | MIT OR Apache-2.0 | https://github.com/lambda-fairy/rust-errno |
| etherparse | 0.20.3 | MIT OR Apache-2.0 | https://github.com/JulianSchmid/etherparse |
| foldhash | 0.2.0 | Zlib | https://github.com/orlp/foldhash |
| form_urlencoded | 1.2.2 | MIT OR Apache-2.0 | https://github.com/servo/rust-url |
| futures | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-channel | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-core | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-executor | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-io | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-macro | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-sink | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-task | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| futures-util | 0.3.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/futures-rs |
| generic-array | 0.14.7 | MIT | https://github.com/fizyk20/generic-array.git |
| getrandom | 0.2.17 | MIT OR Apache-2.0 | https://github.com/rust-random/getrandom |
| getrandom | 0.4.3 | MIT OR Apache-2.0 | https://github.com/rust-random/getrandom |
| hashbrown | 0.17.1 | MIT OR Apache-2.0 | https://github.com/rust-lang/hashbrown |
| hashlink | 0.12.1 | MIT OR Apache-2.0 | https://github.com/djc/hashlink |
| heck | 0.5.0 | MIT OR Apache-2.0 | https://github.com/withoutboats/heck |
| hex | 0.4.3 | MIT OR Apache-2.0 | https://github.com/KokaKiwi/rust-hex |
| hickory-proto | 0.26.2 | MIT OR Apache-2.0 | https://github.com/hickory-dns/hickory-dns |
| httparse | 1.10.1 | MIT OR Apache-2.0 | https://github.com/seanmonstar/httparse |
| iana-time-zone | 0.1.65 | MIT OR Apache-2.0 | https://github.com/strawlab/iana-time-zone |
| icu_collections | 2.1.1 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_locale_core | 2.1.1 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_normalizer | 2.1.1 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_normalizer_data | 2.1.1 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_properties | 2.1.2 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_properties_data | 2.1.2 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| icu_provider | 2.1.1 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| idna | 1.1.0 | MIT OR Apache-2.0 | https://github.com/servo/rust-url/ |
| idna_adapter | 1.2.1 | Apache-2.0 OR MIT | https://github.com/hsivonen/idna_adapter |
| ipnet | 2.12.1 | MIT OR Apache-2.0 | https://github.com/krisprice/ipnet |
| ipstack | 1.0.1 | Apache-2.0 | https://github.com/narrowlink/ipstack |
| is_terminal_polyfill | 1.70.2 | MIT OR Apache-2.0 | https://github.com/polyfill-rs/is_terminal_polyfill |
| itoa | 1.0.18 | MIT OR Apache-2.0 | https://github.com/dtolnay/itoa |
| jiff | 0.2.35 | Unlicense OR MIT | https://github.com/BurntSushi/jiff |
| jiff-core | 0.1.0 | Unlicense OR MIT | https://github.com/BurntSushi/jiff |
| jni | 0.22.4 | MIT OR Apache-2.0 | https://github.com/jni-rs/jni-rs |
| jni-macros | 0.22.4 | MIT OR Apache-2.0 | https://github.com/jni-rs/jni-rs |
| jni-sys | 0.4.1 | MIT OR Apache-2.0 | https://github.com/jni-rs/jni-sys |
| jni-sys-macros | 0.4.1 | MIT OR Apache-2.0 | https://github.com/jni-rs/jni-sys |
| libc | 0.2.189 | MIT OR Apache-2.0 | https://github.com/rust-lang/libc |
| libloading | 0.9.0 | ISC | https://github.com/nagisa/rust_libloading/ |
| linux-raw-sys | 0.12.1 | Apache-2.0 WITH LLVM-exception OR Apache-2.0 OR MIT | https://github.com/sunfishcode/linux-raw-sys |
| litemap | 0.8.3 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| lock_api | 0.4.14 | MIT OR Apache-2.0 | https://github.com/Amanieu/parking_lot |
| log | 0.4.34 | MIT OR Apache-2.0 | https://github.com/rust-lang/log |
| md-5 | 0.10.6 | MIT OR Apache-2.0 | https://github.com/RustCrypto/hashes |
| memchr | 2.8.3 | Unlicense OR MIT | https://github.com/BurntSushi/memchr |
| memoffset | 0.9.1 | MIT | https://github.com/Gilnaa/memoffset |
| mio | 1.2.3 | MIT | https://github.com/tokio-rs/mio |
| nix | 0.31.3 | MIT | https://github.com/nix-rust/nix |
| num-traits | 0.2.19 | MIT OR Apache-2.0 | https://github.com/rust-num/num-traits |
| once_cell | 1.21.4 | MIT OR Apache-2.0 | https://github.com/matklad/once_cell |
| parking_lot | 0.12.5 | MIT OR Apache-2.0 | https://github.com/Amanieu/parking_lot |
| parking_lot_core | 0.9.12 | MIT OR Apache-2.0 | https://github.com/Amanieu/parking_lot |
| percent-encoding | 2.3.2 | MIT OR Apache-2.0 | https://github.com/servo/rust-url/ |
| pin-project-lite | 0.2.17 | Apache-2.0 OR MIT | https://github.com/taiki-e/pin-project-lite |
| portable-atomic | 1.15.0 | Apache-2.0 OR MIT | https://github.com/taiki-e/portable-atomic |
| potential_utf | 0.1.6 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| ppv-lite86 | 0.2.21 | MIT OR Apache-2.0 | https://github.com/cryptocorrosion/cryptocorrosion |
| proc-macro2 | 1.0.107 | MIT OR Apache-2.0 | https://github.com/dtolnay/proc-macro2 |
| quote | 1.0.47 | MIT OR Apache-2.0 | https://github.com/dtolnay/quote |
| rand | 0.10.2 | MIT OR Apache-2.0 | https://github.com/rust-random/rand |
| rand | 0.8.8 | MIT OR Apache-2.0 | https://github.com/rust-random/rand |
| rand_chacha | 0.3.1 | MIT OR Apache-2.0 | https://github.com/rust-random/rand |
| rand_core | 0.10.1 | MIT OR Apache-2.0 | https://github.com/rust-random/rand_core |
| rand_core | 0.6.4 | MIT OR Apache-2.0 | https://github.com/rust-random/rand |
| regex | 1.13.1 | MIT OR Apache-2.0 | https://github.com/rust-lang/regex |
| regex-automata | 0.4.18 | MIT OR Apache-2.0 | https://github.com/rust-lang/regex |
| regex-syntax | 0.8.11 | MIT OR Apache-2.0 | https://github.com/rust-lang/regex |
| rustc_version | 0.4.1 | MIT OR Apache-2.0 | https://github.com/djc/rustc-version-rs |
| rustix | 1.1.4 | Apache-2.0 WITH LLVM-exception OR Apache-2.0 OR MIT | https://github.com/bytecodealliance/rustix |
| same-file | 1.0.6 | Unlicense/MIT | https://github.com/BurntSushi/same-file |
| scopeguard | 1.2.0 | MIT OR Apache-2.0 | https://github.com/bluss/scopeguard |
| semver | 1.0.28 | MIT OR Apache-2.0 | https://github.com/dtolnay/semver |
| serde | 1.0.229 | MIT OR Apache-2.0 | https://github.com/serde-rs/serde |
| serde_core | 1.0.229 | MIT OR Apache-2.0 | https://github.com/serde-rs/serde |
| serde_derive | 1.0.229 | MIT OR Apache-2.0 | https://github.com/serde-rs/serde |
| serde_json | 1.0.151 | MIT OR Apache-2.0 | https://github.com/serde-rs/json |
| sha2 | 0.10.9 | MIT OR Apache-2.0 | https://github.com/RustCrypto/hashes |
| shlex | 2.0.1 | MIT OR Apache-2.0 | https://github.com/comex/rust-shlex |
| signal-hook-registry | 1.4.8 | MIT OR Apache-2.0 | https://github.com/vorner/signal-hook |
| simd_cesu8 | 1.2.0 | Apache-2.0 OR MIT | https://github.com/seancroach/simd_cesu8 |
| simdutf8 | 0.1.5 | MIT OR Apache-2.0 | https://github.com/rusticstuff/simdutf8 |
| slab | 0.4.12 | MIT | https://github.com/tokio-rs/slab |
| smallvec | 1.16.0 | MIT OR Apache-2.0 | https://github.com/servo/rust-smallvec |
| socket2 | 0.6.5 | MIT OR Apache-2.0 | https://github.com/rust-lang/socket2 |
| stable_deref_trait | 1.2.1 | MIT OR Apache-2.0 | https://github.com/storyyeller/stable_deref_trait |
| strsim | 0.11.1 | MIT | https://github.com/rapidfuzz/strsim-rs |
| syn | 2.0.119 | MIT OR Apache-2.0 | https://github.com/dtolnay/syn |
| syn | 3.0.4 | MIT OR Apache-2.0 | https://github.com/dtolnay/syn |
| synstructure | 0.13.2 | MIT | https://github.com/mystor/synstructure |
| terminal_size | 0.4.4 | MIT OR Apache-2.0 | https://github.com/eminence/terminal-size |
| thiserror | 2.0.20 | MIT OR Apache-2.0 | https://github.com/dtolnay/thiserror |
| thiserror-impl | 2.0.20 | MIT OR Apache-2.0 | https://github.com/dtolnay/thiserror |
| tinystr | 0.8.4 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| tinyvec | 1.13.2 | Zlib OR Apache-2.0 OR MIT | https://github.com/Lokathor/tinyvec |
| tinyvec_macros | 0.1.1 | MIT OR Apache-2.0 OR Zlib | https://github.com/Soveu/tinyvec_macros |
| tokio | 1.53.1 | MIT | https://github.com/tokio-rs/tokio |
| tokio-macros | 2.7.2 | MIT | https://github.com/tokio-rs/tokio |
| tokio-util | 0.7.19 | MIT | https://github.com/tokio-rs/tokio |
| tproxy-config | 7.0.7 | MIT | https://github.com/ssrlive/tproxy-config |
| tracing | 0.1.44 | MIT | https://github.com/tokio-rs/tracing |
| tracing-core | 0.1.36 | MIT | https://github.com/tokio-rs/tracing |
| tun | 0.8.14 | WTFPL | https://github.com/meh/rust-tun |
| tun2proxy | 0.8.3 | MIT | https://github.com/tun2proxy/tun2proxy |
| typenum | 1.20.1 | MIT OR Apache-2.0 | https://github.com/paholg/typenum |
| udp-stream | 0.0.12 | MIT | https://github.com/narrowlink/udp-stream |
| unicase | 2.9.0 | MIT OR Apache-2.0 | https://github.com/seanmonstar/unicase |
| unicode-ident | 1.0.24 | (MIT OR Apache-2.0) AND Unicode-3.0 | https://github.com/dtolnay/unicode-ident |
| url | 2.5.8 | MIT OR Apache-2.0 | https://github.com/servo/rust-url |
| utf8_iter | 1.0.4 | Apache-2.0 OR MIT | https://github.com/hsivonen/utf8_iter |
| utf8parse | 0.2.2 | Apache-2.0 OR MIT | https://github.com/alacritty/vte |
| version_check | 0.9.5 | MIT/Apache-2.0 | https://github.com/SergioBenitez/version_check |
| walkdir | 2.5.0 | Unlicense/MIT | https://github.com/BurntSushi/walkdir |
| winapi-util | 0.1.11 | Unlicense OR MIT | https://github.com/BurntSushi/winapi-util |
| windows-link | 0.2.1 | MIT OR Apache-2.0 | https://github.com/microsoft/windows-rs |
| windows-sys | 0.61.2 | MIT OR Apache-2.0 | https://github.com/microsoft/windows-rs |
| writeable | 0.6.4 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| yoke | 0.8.3 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| yoke-derive | 0.8.2 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zerocopy | 0.8.56 | BSD-2-Clause OR Apache-2.0 OR MIT | https://github.com/google/zerocopy |
| zerofrom | 0.1.8 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zerofrom-derive | 0.1.7 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zerotrie | 0.2.5 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zerovec | 0.11.8 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zerovec-derive | 0.11.6 | Unicode-3.0 | https://github.com/unicode-org/icu4x |
| zmij | 1.0.23 | MIT | https://github.com/dtolnay/zmij |
