# Kanjilink: Project State

## Current Focus
Preparing the first Google Play release (Ron approved the app, 23.11.2026). Ready in the repo: Pro via Play Billing
(product `kanjilink_pro`, N5 free), signed release AAB (upload key sent to Ron, never committed), website + legal docs
(`site/`, `docs/legal/`), store listing text and images (`store/en-US/`), and Ron's manual steps (`store/PLAY_CONSOLE_STEPS.md`).
Waiting for Ron: contact email + Pro price, GitHub secrets (4 upload-key values + CLOUDFLARE_API_TOKEN), create the app in
Play Console, first manual AAB upload, App content forms, the in-app product, draft-app permission for the service account.
Then: deploy the site, upload the listing via the API (show Ron first), Internal testing draft.

## Decisions
- Revert point for the v0.2 design polish: PR #5 (squash commit 08b0e0e). "Restore the previous design" = revert it.
- Name **Kanjilink**, package `app.vplusstudio.kanjilink` (permanent once created in Play).
- Native Kotlin + Jetpack Compose (lock-screen wallpaper, widget and scheduled notifications need native).
- No server, no account; progress in `filesDir/cards.json` + SharedPreferences.
- English UI first. Markets: US/UK/CA/AU, then FR/ES/PT (meanings partly in KANJIDIC2), then DE.
- Data: KANJIDIC2 (EDRDG, CC BY-SA 4.0) + Waller JLPT levels via kanji-data (MIT). No WaniKani content.
- Basis: verified $6.9K MRR iOS app (TrustMRR `japanese-education-app`), see agents repo report 2026-11-22.

## MVP v0.1
- [x] Data builder `tools/build_kanji_data.py` → 2,211 kanji N5–N1, romaji, okurigana as まな(ぶ)
- [x] Spaced repetition (light SM-2), daily new-kanji limit, streak
- [x] Rotation every 1–6 h (WorkManager): next kanji → notification, lock-screen wallpaper, widget
- [x] Notification with "Again" / "I know it" actions; quiet hours; Android 13 permission request
- [x] Lock-screen wallpaper, 3 themes (Night, Paper, Indigo), Japanese glyph locale
- [x] Home-screen widget (RemoteViews, glyph drawn as bitmap)
- [x] Screens: onboarding, home, review session, settings, sources & licences (EDRDG duty)
- [x] Tests: 5 logic + 3 screenshot (Robolectric), all green; release AAB builds with R8
- [x] GitHub Actions: tests + debug APK + screenshots; signed AAB when secrets exist

## Store release (v1.0)
- [x] Upload key created (PKCS12, alias kanjilink-upload, SHA-256 12:29:A3:…:96:64); sent to Ron as a file, not in git
- [x] Play Billing 8.3.0: one-time `kanjilink_pro`; free = N5 path; Pro = N4–N1; paywall, upgrade card, restore
- [x] Website `site/` for kanjilink.vplusstudio.app (privacy, terms, accessibility) + `wrangler.jsonc` + deploy workflow
- [x] Hebrew legal docs in `docs/legal/` (privacy, terms, cookies, accessibility) – [CONTACT_EMAIL] still a placeholder
- [x] Store listing en-US (`store/en-US/listing.json`), icon 512, feature graphic, 6 screenshots (`tools/make_store_assets.py`)
- [x] Signed release AAB builds locally (fixed lint: androidx.fragment 1.8.8)
- [ ] Ron: email + price, secrets, Play Console steps (store/PLAY_CONSOLE_STEPS.md)
- [ ] Deploy site, upload listing + images via API, Internal testing draft

## Next
- [x] First test on Ron's phone: works (2026-11-23)
- [x] Fix: chip rows wrap (large font); UI forced LTR on RTL phones; one-line page titles
- [x] Answer buttons show the next review time; toast after answering; self-test (hidden meaning) for seen kanji
- [x] Fixes: "1 stroke", one-line stat labels, "xxx radical (no. N)" meanings removed from data
- [x] Large-font fixes: captions "in 10 min" / "in 1 day" wrap instead of being cut, stat label "N5",
      each reading (kana + romaji) kept together, aligned On/Kun column
- [x] Design polish (v0.2, same look): 字 icon from the real Noto Serif JP glyph; bundled Noto Serif JP for all
      kanji; app mark in headers; stats panel with level progress bar; card/panel styling; settings icon;
      settings in grouped panels; review progress bar; widget redesign (light/dark, small 2x2 + wide layout)
- [x] Kanji style setting: Sans (Noto Sans JP, default, closest to handwriting) / Serif (Noto Serif JP, book style);
      applies to app, widget and lock screen. The app mark and launcher icon stay serif.
- [ ] More device checks (Samsung/Xiaomi lock screen, widget)
- [ ] Pro unlock via Play Billing (decide: one-time vs subscription; suggested free = N5)
- [ ] Upload key + repo secrets (ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD)
- [ ] Create the app in Play Console, first manual AAB upload to Internal testing
- [ ] Privacy policy (English for Play + `docs/legal/` per CLAUDE.md), store listing, screenshots
- [ ] Trademark check for "Kanjilink" (USPTO, EUIPO)
- [ ] Stroke order (KanjiVG, CC BY-SA 3.0) and example words (JMdict) – v0.2
- [ ] Translate meanings/UI: FR, ES, PT

## Notes
- Local tests: Robolectric's runtime jar is fetched by Gradle (`copyRobolectricJars`). In the Claude cloud
  Maven Central may answer 429; download the jar from Google's mirror into `app/build/robolectric-jars/`
  and run with `-x copyRobolectricJars`.
