# Tanniscoring (테니스코어링)

Android 폰 + Wear OS 테니스 스코어 앱.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)  
**Do not clone** for local ZIP import — Android Studio에서 폴더를 직접 Open 하세요.

| | |
|---|---|
| Version | `0.3.4` |
| Phone applicationId | `com.tanniscoring.app` |
| Wear applicationId | `com.tanniscoring.wear` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## What's new in 0.3.4

- **Wear-primary scoring**: 워치가 채점 권한(authority). 경기 중 워치 A/B 탭으로 득점, 롱프레스로 되돌리기.
- **Phone = live scoreboard**: 폰은 `MatchStateDto`를 받아 대형 스코어보드 표시. 「워치에서 득점 중」 배지.
- **MessageClient only (cross-package)**: `com.tanniscoring.app` ≠ `com.tanniscoring.wear` 이라 DataClient PutDataItem은 패키지 간 동기화되지 않음. 상태/이벤트는 전부 MessageClient + WearableListenerService.
- Wear 유휴 게이트 제거: 폰에서 경기 시작을 기다리지 않음. 워치에서 **경기 시작** (선수 A/B · best of 3 기본값).
- 폰 미러 버튼(A/B/되돌리기)은 선택적으로 워치에 `PATH_EVENT` 전송.

## What's new in 0.3

- 타이브레이크 서버 로테이션, Wear/폰 TB UI, 테니스 볼 아이콘

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, SyncJson DTOs
:wear     Wear OS — scoring authority + MessageClient broadcast
:app      Phone — scoreboard display + optional mirror events
```

### Sync model (on-device only)

**Wear is the source of truth.** Phone and Wear use **different applicationIds**, so prefer **MessageClient** for all cross-device payloads (DataClient same-package tricks will not help).

1. Wear starts match → owns [TennisScoringEngine]
2. Wear tap A/B / long-press undo → engine updates → MessageClient `PATH_STATE` → all connected phone nodes
3. Phone `WearableListenerService` / MessageClient listener → UI scoreboard updates immediately
4. Phone optional A/B/Undo → MessageClient `PATH_EVENT` → Wear applies scoring
5. On Wear open / phone connect: Wear resends state; phone can send `PATH_REQUEST_STATE`

No backend, no API keys. Uses Google Play Services **Wearable Data Layer (MessageClient)**.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring (0/15/30/40, deuce, AD, games, sets, best-of-3/5, TB server rotation, undo) + unit tests |
| `:wear` | Start match, large A/B taps, long-press undo, haptics, broadcast state |
| `:app` | Live scoreboard, 「워치에서 득점 중」, optional mirror controls, Wear install |

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
3. Run configuration **`wear`** on the watch
4. Run configuration **`app`** on the phone
5. On the watch: **경기 시작** → tap **A** / **B** (long-press = undo)
6. Phone scoreboard updates live — 「워치에서 득점 중」
7. Phone mirror buttons optional (send events to Wear)

### Verify sync checklist

- [ ] Wear starts match alone (no “start on phone” stuck screen)
- [ ] Wear A/B → phone points update in real time
- [ ] Wear long-press → undo on both
- [ ] Phone reconnect / REQUEST_STATE → Wear resends
- [ ] Different applicationIds still sync via MessageClient
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
├── app/                 # Phone scoreboard
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
