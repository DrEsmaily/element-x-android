# SyncMe Presence — Android-only release candidate

Base: verified optimized release #180, commit `0fb41eb1b2bab88211f37cbe990cdcf660412b20`.
Work branch: `syncme-181-presence-from-180`.
No changes to Synapse, Nextcloud, Talk, server config or the production branch.

## Behavior
- Client GETs the standard Matrix v3 presence status endpoint, using its current account's bearer token.
- Unknown, HTTP errors, denied access and disabled presence never produce a green dot or fake online count.
- Direct chats: English Online, Away, Offline or time-accurate Last seen label.
- Recent DM rows: green dot only for server-confirmed Online status (bounded to 30 recent users, one refresh per minute).
- Group header/details: member count. Verified online count only when all JOIN members are cached and the group has at most 30 members; otherwise member count only.
- Group member list: online indicator for up to 30 loaded members.
- Own activity: selected account in app foreground reports Online; when another account is selected or app is backgrounded, reports Offline. Sync and push services remain running.
- Settings > Last Seen & Online: Show My Activity / Appear Offline. Choice is local to the Matrix session, persisted under its private session file directory.
- Appear Offline is client-side only, not per-contact last-seen protection. It can be overridden by other Matrix clients/devices; cross-client privacy requires server enforcement.

## QA checklist before distributing
1. Compile the optimized ARM64 release APK with GitHub Actions.
2. Run `UserPresenceTest` unit tests (Unknown never counted, timestamp never fabricated).
3. Test login and logout, switch between three accounts, process background/foreground, force-stop and network loss.
4. Test two devices for the same account and Matrix presence aggregation.
5. Test private chat header/profiles, 2-user and 20-user groups, 100+ user group (member count only).
6. Verify old fixes: message and media sending, upload progress, notification delivery, voice/video calls, room-open latency, tags and account switching.
7. Verify online privacy text reflects client-only limitations. No modifications to Synapse.

Compilation alone does not certify real-device behavior.