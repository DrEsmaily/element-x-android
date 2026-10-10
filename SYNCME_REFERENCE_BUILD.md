# SyncMe build reference — 2026-10-10

Baseline app source: commit 836883110d1dbeb1f4463004bcd825658c4f9f86 (GitHub Actions build 234, run 38019790151). The user-tested no-optimization rebuild is run 38071418162 on branch syncme-234-arm64-pristine-parity-20261010.

## Stable build parameters

ARM64 APK (arm64-v8a); Android Gplay Release; JDK 21; Gradle assembleGplayRelease; -PuseLegacyPackaging=false; release optimization.enable=false (no R8 shrinking/obfuscation). Verify APK native .so files are stored without ZIP compression. Keep SDK and call dependencies unchanged. Compare actual unzipped APK sizes, not GitHub artifact ZIP sizes. This is a user-reported low-lag baseline on Poco F3 and F5, not proof of a root cause.

## Approved new change

The existing DM avatar indicator remains 10dp, at the same place. Explicit Online is green #2BCB74; Away becomes orange #FFA726. Offline/Unknown show no dot. Uses the existing event-driven presenceStates; no extra polling or network changes. Needs testing on device.

## Source modifications versus original Element X

- Product branding: SyncMe icons, title, onboarding and login design, initial syncme.ir server, dark cobalt palette, notifications and app footer.
- Home: chats-focused layout, concealed Spaces UI, custom tags, favorites/filters and horizontal chips, room-list presentations.
- Tags: custom creation and management, multiple tags per room, selected-filter persistence and scroll alignment, context-menu assignment and limits.
- Presence: direct-message status tracking, online/away/offline/last-seen text, presence event flow and visible-avatar indicator.
- Messaging UI: reaction display, bubbles, emoji width/wrapping, file alignment, modal action sheets, Back behavior and navigation transitions.
- Media: upload progress and cancellation experience, original/compressed quality selection, media preview/player background changes.
- Settings/accounts: account switcher and session limit, menu alignment, developer options via five taps, default preferences.
- Rooms: directory searching, public-room/admin details and room/member interface changes.
- Diagnostics/performance: startup traces, network/certificate timing investigations, diagnostics export, room-open responsiveness experiments.
- CI: ARM64 builds, previous compression experiments, checkpoints and compatibility probes.

## Caveats and history

The source snapshot and Git commit history are authoritative. Earlier work and unresolved regressions are detailed in .github/SYNCME_CONTINUATION_MASTER.txt. Some older experiments were not retained or were not verified. In particular large-upload retries, notifications, calling and Poco F5 lag require validation on device; do not equate compile success with a fix. The older master document's R8-enabled build preference is superseded by these reference settings. Compare against upstream and confirm behavior before calling any subsequent branch stable.

## Future workflow

Use THIS branch as the prospective baseline once GitHub Actions succeeds and user approves the Away-dot behavior. Keep native compression off, R8 off, ARM64 output, SDK unchanged unless expressly requested. Record source SHA, run URL, changes and observed device behavior on each subsequent build.
