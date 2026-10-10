# SyncMe — Copyable TLS and freeze diagnostics test plan

The existing app StartupTrace report has a "Copy All" control and retains up to 10 runs, including main-thread watchdog stack samples. The experimental verifier injects privacy-preserving events into these same reports.

## Events to expect
- tls_verify_firstparty: TLS certificate target is syncme.ir or its subdomain (not necessarily a successful connection).
- tls_verify_other_host: certificate verification was invoked for another host; the host string is intentionally not logged.
- tls_verify_thread_main / tls_verify_thread_background: verifier invocation thread.
- tls_chain_validation_begin: Android trust manager chain validation begins.
- tls_chain_rejected: Android trust manager rejected a certificate.
- tls_chain_valid_revocation_offline_skipped: trust-manager validation passed and explicit online CRL/OCSP verification was skipped.
- main_thread_stall_detected_ms_* / stack_* / main_thread_resumed_after_ms_*: a captured UI freeze with Java stack trace.

Do not include usernames, access tokens, message bodies, complete destination URLs, certificate bytes or cookies in diagnostics.

## Device test
1. Open the experimental APK on Poco F3 and F5; do not replace the stable baseline until APK installation and trust tests pass.
2. Close the app completely and cold-start, open chat repeatedly, visit recent conversations, receive a message, send a file and return to home.
3. Open existing in-app startup diagnostics and choose Copy All; paste into ChatGPT. If it is large, paste the latest one or two runs plus the full stack of the stall.
4. Repeat offline internationally with only the SyncMe server reachable, and once with VPN enabled. Include whether DNS resolution was provided internally and approximate freeze time.
5. If reporting is unavailable via the UI, MainActivity supports action io.syncme.EXPORT_STARTUP_TRACE for a direct exported-activity intent; UI implementation already contains Copy All and Clear.
6. A log that contains tls_chain_valid_revocation_offline_skipped indicates the patched verifier was executed, but does NOT prove no external traffic from Android or other app components.

## External traffic proof
Capture device traffic (including DNS) separately from the app with a system-level capture or VPN-based packet monitor. Verify destination addresses and CRL/OCSP attempts for all cold-start and reconnection cases. An app-level certificate verifier cannot observe DNS, raw sockets, system trust-store network paths, WebView, push, call services or other TLS stacks. Capture may be incomplete under device VPN tunneling; distinguish visibility limitations from actual zero traffic. Do not interpret absence of an event in app logs as proof of no connection.

## Acceptance
Before release: successful patched Android verifier AAR build, Release APK resolved against that patched dependency, a positive runtime TLS event in copied log, negative tests for bad certificates, a no-international-network test, and external traffic capture. UI must remain responsive. Store run SHA and test result.
