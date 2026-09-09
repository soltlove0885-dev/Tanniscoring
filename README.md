# Tanniscoring (테니스코어링) 1.0.0

Android 폰 + Wear OS 테니스 스코어 앱.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)

| | |
|---|---|
| Version | **1.0.0** (versionCode **10**) |
| applicationId (phone **and** wear) | `com.tanniscoring.app` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## Product (simple)

- **Wear OS watch is primary**: start match on watch → **tap A/B = point**, **long-press = undo**
- **Phone is live scoreboard only** (optional A/B/undo that send events to Wear)
- Real-time sync via **MessageClient** + **WearableListenerService**
- Same `applicationId` on phone + wear; phone embeds wear with `wearApp(project(":wear"))`

---

## Clean install from scratch (required before 1.0.0)

Old builds used a separate Wear package. Remove leftovers first, then install 1.0.0 once.

1. **Uninstall old phone app** `com.tanniscoring.app` (any previous version).
2. **Uninstall old Wear package** `com.tanniscoring.wear` from the Galaxy Watch / Wear OS device (Settings → Apps, or `adb uninstall com.tanniscoring.wear`).
3. Install **1.0.0** from **Play Internal testing** *or* the signed phone APK/AAB:
   - Play: install the phone app; Wear companion installs with it (embedded).
   - APK: sideload `tanniscoring-app-1.0.0.apk` on the phone (Wear APK is embedded via `wearApp`).
4. Open **테니스코어링** from the **watch launcher** (not an old Play “other package”).
5. On the watch tap **경기 시작** → phone shows the live scoreboard (“워치에서 경기를 시작하세요” until then).

### Verify

- [ ] Phone package = `com.tanniscoring.app`
- [ ] Wear package = `com.tanniscoring.app` (not `com.tanniscoring.wear`)
- [ ] Watch: 경기 시작 → tap A/B → phone points update
- [ ] Watch: long-press → undo on both
- [ ] Phone optional A/B/undo still works (events → Wear)

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, SyncJson DTOs
:wear     Wear OS — scoring authority + MessageClient (applicationId = com.tanniscoring.app)
:app      Phone — scoreboard + wearApp(:wear) embed (same applicationId)
```

**Wear is the source of truth.** Phone and Wear share `com.tanniscoring.app` so the Wearable Data Layer delivers messages.

1. Wear starts match → owns scoring engine
2. Wear tap A/B / long-press undo → MessageClient `PATH_STATE` → phone
3. Phone listener → scoreboard UI
4. Phone optional A/B/Undo → MessageClient `PATH_EVENT` → Wear
5. On connect / open: Wear resends state; phone can `PATH_REQUEST_STATE`

No backend. Google Play Services Wearable Data Layer only.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring rules + unit tests |
| `:wear` | Start match, large A/B taps, long-press undo, broadcast state |
| `:app` | Live scoreboard (“워치에서 경기를 시작하세요”), optional mirror controls; embeds wear |

---

## Open in Android Studio

1. **File → Open** the `Tanniscoring` folder (do not ZIP-import for day-to-day work)
2. Android Studio Ladybug+ (AGP 8.7 / Kotlin 2.0 / Gradle 8.9)
3. Gradle Sync → phone emulator (API 26+) + Wear OS emulator (API 30+), paired

```bash
./gradlew :shared:test
./gradlew :app:bundleRelease :app:assembleRelease
```

---

## Scoring rules (MVP)

- Points: 0 → 15 → 30 → 40 · Deuce / Advantage / Game
- Set: first to 6, win by 2; 6-6 → tiebreak to 7 (win by 2)
- Match: best-of-3 (default) or best-of-5
- Server rotates after each game; TB first-point then every 2; after TB receiver serves next
- Undo: stack-based last action restore

---

## Project layout

```
Tanniscoring/
├── app/      # Phone scoreboard (+ wearApp embed)
├── wear/     # Wear scoring authority
├── shared/   # Pure Kotlin + JUnit
└── README.md
```

## Known gaps

- No iOS / watchOS
- No cloud / Firebase
- History is local SharedPreferences only

Packaged for `soltlove0885-dev/Tanniscoring`.
