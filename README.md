# SCORECORE (스코어코어) 1.5.6 — package com.tanniscoring.app

Android phone + Wear OS multi-sport score app (Tennis + Badminton), bilingual KO/EN.

GitHub: [`soltlove0885-dev/Tanniscoring`](https://github.com/soltlove0885-dev/Tanniscoring)

| | |
|---|---|
| Version | **1.5.6** (versionCode phone **41** / wear **42**) |
| applicationId (phone **and** wear) | `com.tanniscoring.app` |
| minSdk | Phone 26 / Wear 30 |
| UI | Jetpack Compose + Wear Compose (Korean / English) |

---

## Product (simple)

- **First launch**: language picker (한국어 / English) — AppCompat per-app locales (`AppCompatDelegate.setApplicationLocales`); change later from home
- **Home**: choose sport — **Tennis** or **Badminton**
- **Phone can score alone**: 「Start match」 / 「경기 시작」 starts a full match on the phone — **no watch required**
- **Wear OS watch is optional**: when connected, wrist scoring (tap = point, long-press = undo) syncs with the phone
- **Phone MatchScreen / BadmintonMatchScreen**: tap A/B = point, long-press = undo (portrait + landscape)
- **Server indicator**: color highlight + **tennis ball** (tennis) / **shuttlecock** (badminton) above serving side (phone + Wear)
- **Tennis**: No-Ad + tournament (phone)
- **Badminton**: rally to 21, win by 2, cap at 30; rally winner serves
- **Reconnect**: refresh / re-request Wear state when using a companion watch
- Real-time sync via **MessageClient** + **WearableListenerService** when a watch is paired
- Same `applicationId` on phone + wear; Wear ships as a **separate Wear OS AAB** (no `wearApp` embed — embed duplicates wear versionCode on Play)

### 1.5.6

- Phone-owned match start for tennis + badminton (idle CTA 「경기 시작」 / Start match) — score with zero watch paired
- Watch remains optional companion sync when connected
- KO/EN copy: remove “must start on watch” framing; phone can score alone
- Store SHORT (KO/EN): watch·phone scoring anywhere — tap=point · long-press=undo (no glove)
- Phone vc41 / wear vc42 (separate codes; no wearApp embed)

### 1.5.5

- Premium SCORECORE launcher icon from user mock (neon crossed rackets) — phone + Wear adaptive foreground PNGs
- Play assets: `icon-neon-preview.png` (1024) + `hi-res-icon-neon.png` (512)

### 1.5.4

- Fix clipped KO/EN text on phone + Wear (auto-size labels, tighter padding, scroll on pickers / round watch)
- Phone scoring always available when a match is live: tap A/B = point, long-press = undo (portrait + landscape), synced to Wear without disabling controls when watch is connected
- Keep 1.4.1 requestId/sequence dedupe + mirror debounce (no double-count)
- Wear language picker / sport picker remain scrollable; AppCompat locales unchanged

### 1.5.1

- Fix KO↔EN language switch (AppCompatActivity + per-app locales recreate)
- Launcher icon: tennis ball + shuttlecock (phone + Wear)
- Server icons on scoreboard (tennis ball / shuttlecock) phone + Wear

### 1.5.0

- Bilingual first-run + in-app language change
- Sport picker (Tennis / Badminton)
- Badminton scoring on phone + Wear
- Rename reconnect label

### Prior

- **1.4.1**: Fix phone scoring double-apply + open wear app
- **1.4.0**: Wear square score boxes; No-Ad; End via long-press title
- **1.3.1**: removed serve-speed; tournament name reset; Wear end → idle

---

## Badminton rules

- Rally point scoring
- First to **21**, must win by **2**
- From 20-20 continue until +2 **or** a side reaches **30** (cap; 29-all next point wins)
- Rally winner serves next

---

## Architecture

```
:shared   Pure Kotlin — Tennis + Badminton engines, MatchState DTOs, Tournament, SyncJson
:wear     Wear OS — optional wrist scoring + MessageClient (applicationId = com.tanniscoring.app)
:app      Phone — can start/score alone; live scoreboard + tournament + language/sport pickers
```

**Phone can own scoring** (local engines). Wear is an optional companion for wrist scoring; matches started on Wear still drive the phone scoreboard.

---

## Build

```bash
./gradlew :shared:test
./gradlew :app:bundleRelease :wear:bundleRelease
```

Packaged AABs:

- `tanniscoring-app-vc41-1.5.6.aab`
- `tanniscoring-wear-vc42-1.5.6.aab`

Packaged for `soltlove0885-dev/Tanniscoring`.
