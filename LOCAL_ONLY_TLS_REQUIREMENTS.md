# Local-only TLS verification — implementation gate

Base: SyncMe reference from 234; leave the approved baseline untouched.

## Verified dependency path
Matrix SDK 26.10.05-nightly -> reqwest 0.13 / rustls -> rustls-platform-verifier -> Android Java CertificateVerifier -> CertPathValidator / RevocationChecker / URICertStore. The last component may fetch CRLs from certificate authorities outside the SyncMe network.

## Required code change before release
A customized, version-matched Matrix Rust SDK AAR is necessary unless the exact Kotlin FFI exposes a compatible verifier hook. Use rustls WebPKI local certificate verification and trusted roots rather than Android platform verifier, and configure reqwest in the Rust layer. Retain chain, expiration, hostname, signature and key-usage checks. Do not call disableSslVerification or install permissive trust managers. Ensure all Matrix SDK network clients and relevant outbound requests use the intended verifier. Remove external CRL cleartext allowances only AFTER removing the CRL-calling verifier. Other services need independent auditing.

## Acceptance tests — none passed yet
1. Build and link the customized AAR on ARM64; verify matching ABI/API for calling component.
2. Offline validation of trusted valid server cert succeeds.
3. Expired, wrong-host, unknown CA, malformed chain are rejected.
4. Capture device/process DNS/HTTP/HTTPS flows and verify zero outbound CRL or OCSP lookups during cold launch, pagination, media and calling; check devices with empty caches.
5. Repeat with international connectivity completely blocked; confirm startup and conversations open without main-thread stalls.
6. Check TLS renewal compatibility, cert pin/roots maintenance and multi-device behavior.

## Critical caveat
Deleting CRL domain entries from network_security_config.xml alone is not a fix and can cause legitimate TLS connections to fail. Build settings (R8=false; uncompressed .so) are unrelated to revocation policy. This branch is experimental; do not promote it to stable until verified.

## Verified implementation constraints
The current Gradle build uses the published SDK AAR for releaseImplementation even if a local AAR is present: existing Gradle local-AAR substitution is DEBUG-only. A new Rust AAR must be substituted in Release and for the element-call-matrix dependency as well. FFI ClientBuilder exposes disabling SSL validation but no WebPKI selection; do not disable TLS validation. The Rust ClientBuilder accepts a custom reqwest client, but the Kotlin FFI does not export that hook. The upstream Matrix issue #6319 confirms Android CRL fetches began with reqwest 0.13 and rustls-platform-verifier. Source modification must configure a custom rustls WebPkiServerVerifier without CRLs, preserving hostname, chain and expiry verification. Obtain exact corresponding matrix-rust-sdk revision; use matrix-rust-components-kotlin scripts/build.sh -p SDK_SOURCE -m sdk -t aarch64-linux-android -r and verify FFI/API compatibility. Deleting domains in network_security_config is insufficient before replacing the verifier. No functional patch or AAR has been built yet.
