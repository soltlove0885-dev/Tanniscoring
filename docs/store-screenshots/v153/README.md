# Play Store screenshots — SCORECORE 1.5.5

Phone intro screenshots (portrait **1080×1920**). Inner frames mimic the **real Compose OLED UI** (`CourtColors` / `WearCourtColors`: true black, soft court green `#64B574`, serve gold `#E0B84A`) with Korean chrome from `values-ko`.

> Regenerated as faithful Compose mocks (Pillow) — no Android emulator on the build machine. Not neon lime marketing art.

## Order (upload in this sequence)

| # | File | Caption (outside phone) | Inner screen |
|---|------|-------------------------|--------------|
| 1 | `01-hero.png` | 워치·폰 어디서든 채점 / 탭 = 득점 · 길게 = 취소 | Wear tennis score squares (`선수 A/B`, 노애드, 서브) |
| 2 | `02-phone-watch.png` | 폰은 전광판, 워치는 채점 / 실시간 동기화 · 노애드 | Phone tennis portrait scoreboard (`A 득점` / `B 득점`, 되돌리기…) |
| 3 | `03-sports.png` | 테니스 / 배드민턴 한 앱 / 종목 선택 · KR / EN | `SportPickerScreen` (`종목 선택`, 테니스, 배드민턴, 언어 변경, 설정) |
| 4 | `04-tournament.png` | 4·8명 토너먼트 / 단판 브라켓 | `TournamentBracketScreen` (준결승·결승 cards) |

Also:

- `feature-graphic.png` (1024×500) — listing banner, same OLED palette + watch mock
- `icon-preview.png` (512×512) — raster of adaptive foreground (lime tennis + shuttle) on `#0D1117` background

## Suggested Play listing (KO)

Source: `play-ready/listing-ko.txt`

### Short description (≤80 chars)

워치·폰 어디서든 채점 — 탭=득점·길게=취소

### Full description

스코어코어(SCORECORE)는 Galaxy Watch 등 Wear OS와 폰을 위한 테니스·배드민턴 스코어 앱입니다.

• 워치에서 탭=득점, 길게 누르기=취소  
• 폰만으로도 경기 시작·채점 가능 (워치 선택)  
• 워치 연동 시 폰은 실시간 전광판으로 동기화  
• 테니스(노애드 포함) + 배드민턴 한 앱  
• 첫 실행 한국어 / English  
• 4·8명 단판 토너먼트(폰)

광고 SDK·분석 SDK 없음. 문의: soltlove0885@gmail.com

버전 1.5.5 (phone vc37 / wear vc40)

## Suggested Play listing (EN — global)

Source: `play-ready/listing-en.txt`

### Short description

Score on watch or phone — tap=point, long-press=undo

### Full description

SCORECORE is a tennis & badminton scoring app for Wear OS (Galaxy Watch and more) and phone.

• Score on the watch or phone: tap = point, long-press = undo  
• Phone can start and score matches alone — watch is optional  
• Optional watch sync; phone doubles as live scoreboard when paired  
• Tennis (including No-Ad) + badminton (rally to 21) in one app  
• Korean / English from first launch  
• 4- and 8-player single-elim tournament brackets (phone)

No ads SDK, no analytics SDK. Contact: soltlove0885@gmail.com

Version 1.5.5 (phone vc37 / wear vc40)

## Notes

- Do **not** upload these via this repo automation; upload manually in Play Console if needed.
- Colors follow `app/.../ui/Theme.kt` and `wear/.../ui/WearTheme.kt`, not the launcher lime accent.
- **Positioning memory:** do **not** claim glove / glove-friendly scoring — a normal screen tap is not glove-friendly without capacitive gloves or hardware buttons. Market as **score on watch or phone**; phone-only start/score supported; watch optional.

## Suggested Play listing (KO + EN)

Canonical files: `play-ready/listing-ko.txt`, `play-ready/listing-en.txt`

### Short (KO)

워치·폰 어디서든 채점 — 탭=득점·길게=취소

### Short (EN)

Score on watch or phone — tap=point · long-press=undo

### Notes

Phone can start/score alone; watch optional. Version **1.5.6** (phone vc41 / wear vc42).
