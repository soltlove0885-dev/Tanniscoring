# Play Store screenshots — Tanniscoring 1.5.3

Phone intro screenshots (portrait **1080×1920**). Inner frames mimic the **real Compose OLED UI** (`CourtColors` / `WearCourtColors`: true black, soft court green `#64B574`, serve gold `#E0B84A`) with Korean chrome from `values-ko`.

> Regenerated as faithful Compose mocks (Pillow) — no Android emulator on the build machine. Not neon lime marketing art.

## Order (upload in this sequence)

| # | File | Caption (outside phone) | Inner screen |
|---|------|-------------------------|--------------|
| 1 | `01-hero.png` | 글러브 끼고도 되는 스코어 워치 / 탭 = 득점 · 길게 = 취소 | Wear tennis score squares (`선수 A/B`, 노애드, 서브) |
| 2 | `02-phone-watch.png` | 폰은 전광판, 워치는 채점 / 실시간 동기화 · 노애드 | Phone tennis portrait scoreboard (`A 득점` / `B 득점`, 되돌리기…) |
| 3 | `03-sports.png` | 테니스 / 배드민턴 한 앱 / 종목 선택 · KR / EN | `SportPickerScreen` (`종목 선택`, 테니스, 배드민턴, 언어 변경, 설정) |
| 4 | `04-tournament.png` | 4·8명 토너먼트 / 단판 브라켓 | `TournamentBracketScreen` (준결승·결승 cards) |

Also:

- `feature-graphic.png` (1024×500) — listing banner, same OLED palette + watch mock
- `icon-preview.png` (512×512) — raster of adaptive foreground (lime tennis + shuttle) on `#0D1117` background

## Suggested Play listing (KO)

### Short description (≤80 chars)

글러브 끼고도 되는 스코어 워치 — 탭=득점·길게=취소, 폰 전광판 동기화

### Full description

테니스코어링(Tanniscoring)은 Galaxy Watch 등 Wear OS와 폰을 위한 테니스·배드민턴 스코어 앱입니다.

• 글러브 끼고도 되는 채점: 워치에서 탭=득점, 길게 누르기=취소  
• 폰은 실시간 전광판, 워치는 채점 권한 — MessageClient로 동기화  
• 테니스(노애드 포함) + 배드민턴(21점 랠리) 한 앱  
• 첫 실행부터 한국어 / English  
• 4명·8명 단판 토너먼트 브라켓(폰)

광고 SDK·분석 SDK 없음. 문의: soltlove0885@gmail.com

버전 1.5.3 (phone vc33 / wear vc34)

## Notes

- Do **not** upload these via this repo automation; upload manually in Play Console if needed.
- Colors follow `app/.../ui/Theme.kt` and `wear/.../ui/WearTheme.kt`, not the launcher lime accent.
