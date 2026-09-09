# Tanniscoring (테니스코어링)

Android 폰 + Wear OS 테니스 스코어 앱.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)  
**Do not clone** for local ZIP import — Android Studio에서 폴더를 직접 Open 하세요.

| | |
|---|---|
| Version | `0.4.0` (versionCode 8) |
| applicationId (phone **and** wear) | `com.tanniscoring.app` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## What's new in 0.4.0

- **Unified package**: phone and wear both use `com.tanniscoring.app`. Cross-package MessageClient between `com.tanniscoring.app` and `com.tanniscoring.wear` was failing in the field (Galaxy Watch Ultra); same applicationId fixes Data Layer routing.
- **Classic companion embed**: phone `wearApp(project(":wear"))` embeds the Wear APK inside the phone AAB/APK.
- Keep **Wear-primary scoring** (0.3.4 UX): watch starts match, tap A/B, long-press undo; phone is scoreboard.
- Removed separate “install wear from Play” button — companion installs with the phone app.

## Upgrade from ≤0.3.4

1. **Uninstall** the old watch app `com.tanniscoring.wear` from the Galaxy Watch.
2. Install the new **phone** app `0.4.0` (AAB/APK). The Wear APK is embedded and installs as companion with the same id `com.tanniscoring.app`.
3. Open **테니스코어링** from the watch launcher (not Play “other package”).
4. Start a match on the watch → phone scoreboard updates live.

## What's new in 0.3.4

- Wear-primary scoring, phone live scoreboard, MessageClient both ways

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, SyncJson DTOs
:wear     Wear OS — scoring authority + MessageClient (applicationId = com.tanniscoring.app)
:app      Phone — scoreboard + wearApp(:wear) embed (same applicationId)
```

### Sync model (on-device only)

**Wear is the source of truth.** Phone and Wear share **`com.tanniscoring.app`**, which is required for Wearable Data Layer delivery.

1. Wear starts match → owns [TennisScoringEngine]
2. Wear tap A/B / long-press undo → engine updates → MessageClient `PATH_STATE` → phone nodes
3. Phone `WearableListenerService` / MessageClient listener → UI scoreboard updates
4. Phone optional A/B/Undo → MessageClient `PATH_EVENT` → Wear applies scoring
5. On Wear open / phone connect: Wear resends state; phone can send `PATH_REQUEST_STATE`

No backend, no API keys. Uses Google Play Services **Wearable Data Layer (MessageClient)**.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring (0/15/30/40, deuce, AD, games, sets, best-of-3/5, TB server rotation, undo) + unit tests |
| `:wear` | Start match, large A/B taps, long-press undo, haptics, broadcast state |
| `:app` | Live scoreboard, 「워치에서 득점 중」, optional mirror controls; embeds wear via `wearApp` |

---

## Open in Android Studio

1. Unzip or open the `Tanniscoring` folder
2. **File → Open**
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
2. Pair them: Wear emulator → **… → Pair with companion** (or Device Manager pairing)
3. Install/run **`app`** on the phone (embeds wear) — or run **`wear`** on the watch for debug
4. On the watch launcher: open **테니스코어링** → **경기 시작** → tap **A** / **B** (long-press = undo)
5. Phone scoreboard updates live — 「워치에서 득점 중」
6. Phone mirror buttons optional (send events to Wear)

### Verify sync checklist

- [ ] Old `com.tanniscoring.wear` uninstalled from watch
- [ ] Phone + wear both report package `com.tanniscoring.app`
- [ ] Wear starts match alone
- [ ] Wear A/B → phone points update in real time
- [ ] Wear long-press → undo on both
- [ ] Phone reconnect / REQUEST_STATE → Wear resends
- [ ] Kill wear process → reopen restores current match (prefs)
- [ ] Finished matches appear under phone **최근 경기**

> Emulators without Google Play / Wearable API may log sync warnings — Wear still scores locally.

---

## Scoring rules (MVP)

- Points: 0 → 15 → 30 → 40
- Deuce / Advantage / Game
- Set: first to 6 games, win by 2; at 6-6 → tiebreak to 7 (win by 2)
- Match: best-of-3 (default) or best-of-5
- Server changes after each completed game
- Tiebreak: first point by due server; switch after 1st point, then every 2 points; after TB, first-point receiver serves next
- Undo: stack-based last action restore

```bash
./gradlew :shared:test
```

---

## Project layout

```
Tanniscoring/
├── app/                 # Phone scoreboard (+ wearApp embed)
├── wear/                # Wear scoring authority
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
- History is local SharedPreferences only (max ~20 entries on phone)
- Doubles partner serve order within a team not modeled (side-level server only)

---

## License / packaging

Packaged for `soltlove0885-dev/Tanniscoring`. Local demo and Android Studio import.
