# Tanniscoring (테니스코어링) 1.3.0

Android 폰 + Wear OS 테니스 스코어 앱.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)

| | |
|---|---|
| Version | **1.3.0** (versionCode phone **19** / wear **20**) |
| applicationId (phone **and** wear) | `com.tanniscoring.app` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (한국어) |

---

## Product (simple)

- **Wear OS watch is primary**: start match on watch → **tap A/B = point**, **long-press = undo**
- **Phone is live scoreboard** (optional A/B/undo that send events to Wear)
- **Tournament mode (1.2.0+)**: phone creates 4/8-player single-elimination bracket; tap a match to start on Wear; winner advances automatically
- **Serve speed MVP (1.3.0)**: phone **스피드 온** → CameraX motion-peak estimate (추정 km/h), 1st/2nd/fault session, gold flash on landscape OLED scoreboard; optional small text on Wear
- **1.1.0+ UI**: OLED dark court palette; phone **landscape = huge scoreboard-only**; clear **server highlight**; **keep screen on** during active match (phone + wear)
- Real-time sync via **MessageClient** + **WearableListenerService**
- Same `applicationId` on phone + wear; phone embeds wear with `wearApp(project(":wear"))` (secondary — Galaxy Watch auto-install often fails)
- Phone idle screen: **토너먼트** / **워치 앱 열기** / **워치에 설치**

---

## Serve speed UX (1.3.0)

1. During an active match, toggle **스피드 온** (landscape corner or portrait controls)
2. Grant camera permission; optional preview overlay (upper frame = serve band)
3. After a point (or match start), next motion peak in the upper/far band → **1st 서브** + **추정 N km/h** gold pulse
4. No return within ~3.8s, or return-like mid-frame motion without a score change → **폴트** → wait for **2nd**
5. On point scored (from Wear/phone) → clear flash, reset to 1st for the next server
6. Tap calibration chip to cycle **네트 근처 표준 거리** ↔ **베이스라인 먼 거리** (scale factor only)

**Honest accuracy:** UI always says **추정 km/h** / 「카메라 모션 추정 · 레이더 아님」. This is pixels/time × calibration — not radar or ball-tracking CV.

### Limitations / next steps

- Motion-energy MVP only (no TFLite / no true ball track); false positives from players/shadows possible
- Calibration is preset scale factors — on-court measured calibration UI is the next step
- Fault / no-return heuristics are time + region based, not bounce/net detection
- Wear shows a small `1st · 추정 N` line when phone pushes `/tanniscoring/serve`

---

## Tournament UX

1. Phone idle → **토너먼트** → choose **4 or 8** players, default best-of **1 / 3 / 5**, enter names → **대진표 생성**
2. Bracket shows rounds (8강→준결승→결승 or 준결승→결승). Tap a **준비** match to send `START` to Wear and open the phone scoreboard
3. Optional: tap the format chip on a match card to override best-of for that match only (cycles 1→3→5)
4. Wear scores as usual; when the match ends, the phone advances the winner into the next bracket slot
5. From the scoreboard use **대진표로** to return to the bracket; **진행 중 스코어보드** reopens the live match
6. Wear still only scores the active match (phone selection drives who plays)

---

## Clean install from scratch

1. **Uninstall old phone app** `com.tanniscoring.app` (any previous version).
2. **Uninstall old Wear package** `com.tanniscoring.wear` from the Galaxy Watch / Wear OS device if present.
3. Install **1.3.0** phone APK/AAB (`tanniscoring-app-1.3.0.*`). Wear may auto-install via embed; if not, use phone **워치에 설치** or watch Play.
4. On the phone idle screen tap **워치 앱 열기**, or open **테니스코어링** from the **watch launcher**.
5. Single match: watch **경기 시작** → phone scoreboard. Tournament: phone **토너먼트** → select match → watch scores.

### Verify

- [ ] Phone package = `com.tanniscoring.app`
- [ ] Wear package = `com.tanniscoring.app` (not `com.tanniscoring.wear`)
- [ ] Watch: 경기 시작 → tap A/B → phone points update
- [ ] Watch: long-press → undo on both
- [ ] Tournament: create 4-player bracket → play semi → winner appears in final
- [ ] Phone optional A/B/undo still works (events → Wear)
- [ ] 스피드 온 → camera preview → motion shows 추정 km/h flash (not claimed as radar)

---

## Architecture

```
:shared   Pure Kotlin — TennisScoringEngine, MatchState, ServeSessionEngine, TournamentBracket, SyncJson
:wear     Wear OS — scoring authority + MessageClient (applicationId = com.tanniscoring.app)
:app      Phone — scoreboard + tournament + serve camera MVP + wearApp(:wear) embed
```

**Wear is the source of truth for scoring.** Phone owns tournament bracket + serve session.

1. Wear starts match (or phone tournament sends START) → Wear owns scoring engine
2. Wear tap A/B / long-press undo → MessageClient `PATH_STATE` → phone
3. Phone listener → scoreboard UI (+ tournament advance on match over; serve session reset on point)
4. Phone optional A/B/Undo → MessageClient `PATH_EVENT` → Wear
5. Phone serve flash → MessageClient `PATH_SERVE` → Wear small text
6. On connect / open: Wear resends state; phone can `PATH_REQUEST_STATE`

No backend. Google Play Services Wearable Data Layer only.

---

## Modules

| Module | Role |
|--------|------|
| `:shared` | Scoring rules + serve session + tournament bracket + unit tests |
| `:wear` | Start match, large A/B taps, long-press undo, broadcast state, show serve estimate |
| `:app` | Live scoreboard, tournament, CameraX serve MVP; embeds wear |

---

## Open in Android Studio

1. **File → Open** the `Tanniscoring` folder
2. Android Studio Ladybug+ (AGP 8.7 / Kotlin 2.0 / Gradle 8.9)
3. Gradle Sync → phone emulator (API 26+) + Wear OS emulator (API 30+), paired

```bash
./gradlew :shared:test
./gradlew :app:bundleRelease :app:assembleRelease
./gradlew :wear:bundleRelease :wear:assembleRelease
```

---

## Scoring rules (MVP)

- Points: 0 → 15 → 30 → 40 · Deuce / Advantage / Game
- Set: first to 6, win by 2; 6-6 → tiebreak to 7 (win by 2)
- Match: best-of-1, best-of-3 (default), or best-of-5
- Server rotates after each game; TB first-point then every 2; after TB receiver serves next
- Undo: stack-based last action restore

---

## Project layout

```
Tanniscoring/
├── app/      # Phone scoreboard + tournament + serve camera (+ wearApp embed)
├── wear/     # Wear scoring authority
├── shared/   # Pure Kotlin + JUnit
└── README.md
```

## Known gaps

- No iOS / watchOS
- No cloud / Firebase
- History / tournament persistence is local SharedPreferences only
- Serve speed is motion-estimate only (see Serve speed UX)

Packaged for `soltlove0885-dev/Tanniscoring`.
