# Tanniscoring (테니스코어링) 1.5.0

Android phone + Wear OS multi-sport score app (Tennis + Badminton), bilingual KO/EN.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)

| | |
|---|---|
| Version | **1.5.0** (versionCode phone **27** / wear **28**) |
| applicationId (phone **and** wear) | `com.tanniscoring.app` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (Korean / English) |

---

## Product (simple)

- **First launch**: language picker (한국어 / English) — persisted via AppCompat per-app locales; change later from home/settings
- **Home**: choose sport — **Tennis** or **Badminton**
- **Wear OS watch is primary**: start match on watch → **tap score box = point**, **long-press box = undo**
- **Phone is live scoreboard** (optional A/B/undo that send events to Wear)
- **Tennis**: No-Ad (노애드) + tournament (phone) unchanged from 1.4.x
- **Badminton**: rally to 21, win by 2, cap at 30 (recreational / club)
- **Reconnect** (재연결): refresh / re-request Wear state
- Real-time sync via **MessageClient** + **WearableListenerService** (DTO includes `sport`)
- Same `applicationId` on phone + wear; phone embeds wear with `wearApp(project(":wear"))`

### 1.5.0

- Bilingual first-run + in-app language change
- Sport picker (Tennis / Badminton)
- Badminton scoring on phone + Wear
- Rename 「워치 점수 새로고침」 → 「재연결」 / "Reconnect"

### Prior

- **1.4.1**: Fix phone scoring double-apply + 「워치 앱 열기」 RemoteActivityHelper
- **1.4.0**: Wear square score boxes; No-Ad; End via long-press title
- **1.3.1**: removed serve-speed; tournament name reset; Wear end → idle

---

## Badminton rules

- Rally point scoring
- First to **21**, must win by **2**
- From 20-20 continue until +2 **or** a side reaches **30** (cap; 29-all next point wins)

---

## Architecture

```
:shared   Pure Kotlin — Tennis + Badminton engines, MatchState DTOs, Tournament, SyncJson
:wear     Wear OS — scoring authority + MessageClient (applicationId = com.tanniscoring.app)
:app      Phone — scoreboard + tournament + language/sport pickers + wearApp(:wear)
```

**Wear is the source of truth for scoring.** Phone owns tournament bracket + live scoreboard.

---

## Build

```bash
./gradlew :shared:test
./gradlew :app:bundleRelease :wear:bundleRelease
```

Packaged AABs:

- `tanniscoring-app-vc27-1.5.0.aab`
- `tanniscoring-wear-vc28-1.5.0.aab`

Packaged for `soltlove0885-dev/Tanniscoring`.
