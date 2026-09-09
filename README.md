# Tanniscoring (테니스코어링) MVP

Android 폰 + Wear OS 테니스 스코어 앱 MVP.

이 ZIP은 GitHub 저장소 [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring) 용으로 패키징되었습니다.  
**Do not clone** — Android Studio에서 이 폴더/ZIP을 직접 Open 하세요.

| | |
|---|---|
| Version | `0.1.0` |
| Phone applicationId | `com.tanniscoring.app` |
| Wear applicationId | `com.tanniscoring.wear` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, SyncJson DTOs
:app      Phone (Compose) — scoring authority + MessageClient listener
:wear     Wear OS (Wear Compose) — large A/B buttons, sends events
```

### Sync model (on-device only)

**Phone is the source of truth.**

1. Wear taps A/B/Undo → `MessageClient` → path `/tanniscoring/event`
2. Phone `MatchViewModel` applies event via `:shared` engine
3. Phone broadcasts full `MatchStateDto` → path `/tanniscoring/state`
4. Wear UI updates from that state

No backend, no API keys. Uses Google Play Services **Wearable Data Layer**.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring (0/15/30/40, deuce, AD, games, sets, best-of-3/5, undo) + unit tests |
| `:app` | Start match, live scoreboard, point/undo buttons, receives Wear events |
| `:wear` | Large A/B buttons, compact score, undo, sends events to phone |

---

## Open in Android Studio

1. Unzip `Tanniscoring.zip`
2. **File → Open** the `Tanniscoring` folder
3. Android Studio **Ladybug+** (AGP 8.7 / Kotlin 2.0 / Gradle 8.9)
4. If `gradle/wrapper/gradle-wrapper.jar` is missing, Studio will offer to create the wrapper — accept, or run:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
5. Wait for Gradle Sync
6. Create/select an Android phone emulator (API 26+) and a Wear OS emulator (API 30+)

---

## Run phone + Wear

1. Start a **Phone** emulator and a **Wear OS** emulator
2. Pair them: Wear emulator → **… → Pair with companion** (or Android Studio Device Manager pairing),  
   or on device: Bluetooth + Wear OS companion app
3. Run configuration **`app`** on the phone
4. Run configuration **`wear`** on the watch
5. On the phone: enter player names → choose 3판 2선승 / 5판 3선승 → **경기 시작**
6. On the watch: tap **A** / **B** — phone scoreboard should update live
7. Phone can also score without the watch (local buttons)

### Verify sync checklist

- [ ] Phone starts match → Wear shows names/score (after first state push; tap a point on phone to force broadcast)
- [ ] Wear A button → phone points for A increase
- [ ] Wear Undo → phone undoes last point
- [ ] Disconnect Wear → phone still scores locally; status shows 워치 미연결

> Emulators without Google Play / Wearable API may log sync warnings — scoring on the phone still works offline.

---

## Scoring rules (MVP)

- Points: 0 → 15 → 30 → 40
- Deuce / Advantage / Game
- Set: first to 6 games, win by 2; at 6-6 → tiebreak to 7 (win by 2)
- Match: best-of-3 (default) or best-of-5
- Undo: stack-based last point restore

Unit tests: `:shared` → `TennisScoringEngineTest` (deuce, AD, game, set, match, undo, JSON roundtrip).

```bash
./gradlew :shared:test
```

---

## Project layout

```
Tanniscoring/
├── app/                 # Phone
├── wear/                # Wear OS
├── shared/              # Pure Kotlin + JUnit 5
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
└── README.md
```

---

## Known gaps / later

- iOS / watchOS companion — **not in this MVP** (planned later)
- No cloud account, no Firebase, no API keys
- Tiebreak UI on Wear is compact (numeric points)
- No persistence across process death (in-memory engine)
- App icons use system placeholders
- Wear cannot start a match by itself (phone starts; Wear only scores)
- Full gradle-wrapper.jar may need Studio to generate on first open

---

## License / packaging

Packaged as zip for import into `soltlove0885-dev/Tanniscoring`.  
MVP for local demo and Android Studio import.
