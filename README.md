# Tanniscoring (테니스코어링)

Android 폰 + Wear OS 테니스 스코어 앱.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)  
**Do not clone** for local ZIP import — Android Studio에서 폴더를 직접 Open 하세요.

| | |
|---|---|
| Version | `0.3.0` |
| Phone applicationId | `com.tanniscoring.app` |
| Wear applicationId | `com.tanniscoring.wear` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## What's new in 0.3

- **타이브레이크 서버 로테이션**: 첫 포인트 후 교대, 이후 2포인트마다 교대 (홀수 합계 후 스위치). TB 종료 후 첫 수신자가 다음 세트 서브
- **Wear 서버 토글**: 점수판 롱프레스 또는 **서브** 버튼 → 폰으로 `TOGGLE_SERVER` 이벤트 (폰이 권한 보유). Undo 동기화 유지
- **타이브레이크 UI**: 폰 `TB · 타이브레이크`, 워치 `TB` / `타이브레이크` 표시
- **앱 아이콘**: 테니스 볼 테마 벡터/어댑티브 아이콘 (폰·워치)
- 버전 `0.3.0`

## What's new in 0.2 (MVP polish)

- **서버 표시**: 현재 서브 사이드 표시 (폰/워치). 게임 종료 시 자동 교대, 탭으로 수동 변경
- **복식**: 단식/복식 선택, 팀 이름 필드
- **저장**: SharedPreferences JSON으로 현재 경기 복원 + 최근 종료 경기 목록
- **Wear UX**: 득점 햅틱, 서버 마커, 미시작 시 안내 문구, 더 큰 버튼
- **Phone UX**: 경기 종료 / 새 경기, 세트 기록, 되돌리기 유지

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, SyncJson DTOs
:app      Phone (Compose) — scoring authority + MessageClient + prefs
:wear     Wear OS (Wear Compose) — large A/B buttons, sends events
```

### Sync model (on-device only)

**Phone is the source of truth.**

1. Wear taps A/B/Undo/서브 → `MessageClient` → path `/tanniscoring/event`
2. Phone `MatchViewModel` applies event via `:shared` engine
3. Phone broadcasts full `MatchStateDto` (점수·서버·이름·모드·활성여부·TB) → `/tanniscoring/state`
4. Wear UI updates from that state

No backend, no API keys. Uses Google Play Services **Wearable Data Layer**.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring (0/15/30/40, deuce, AD, games, sets, best-of-3/5, TB server rotation, undo) + unit tests |
| `:app` | Start (singles/doubles), scoreboard, server toggle, end/new match, history, Wear sync |
| `:wear` | Large A/B buttons, server marker/toggle, haptics, TB badge, idle message |

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
3. Run configuration **`app`** on the phone
4. Run configuration **`wear`** on the watch
5. On the phone: 단식/복식 · 이름 · 3판/5판 → **경기 시작**
6. On the watch: tap **A** / **B** — phone scoreboard updates live (haptic on watch)
7. Phone can score without the watch

### Verify sync checklist

- [ ] Phone starts match → Wear shows names/score/server
- [ ] Wear A button → phone points for A increase (+ haptic)
- [ ] Wear Undo → phone undoes last point
- [ ] Wear **서브** / long-press scoreboard → server toggles on phone + wear
- [ ] Game win → server marker switches sides
- [ ] 6-6 tiebreak → TB badge; server rotates after 1st then every 2 points
- [ ] Kill phone app / process death → reopen restores current match
- [ ] Finished matches appear under **최근 경기**
- [ ] Disconnect Wear → phone still scores locally

> Emulators without Google Play / Wearable API may log sync warnings — scoring on the phone still works offline.

---

## Scoring rules (MVP)

- Points: 0 → 15 → 30 → 40
- Deuce / Advantage / Game
- Set: first to 6 games, win by 2; at 6-6 → tiebreak to 7 (win by 2)
- Match: best-of-3 (default) or best-of-5
- Server changes after each completed game
- Tiebreak: first point by due server; switch after 1st point, then every 2 points; after TB, first-point receiver serves next
- Undo: stack-based last action restore (points / server toggle)

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
- Wear cannot start a match by itself (phone starts; Wear only scores / toggles server)
- History is local SharedPreferences only (max ~20 entries)
- Doubles partner serve order within a team not modeled (side-level server only)

---

## License / packaging

Packaged for `soltlove0885-dev/Tanniscoring`. Local demo and Android Studio import.
