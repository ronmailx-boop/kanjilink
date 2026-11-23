# Kanjilink: Project State

## Current Focus
MVP v0.1 built and tested in the cloud (Robolectric). Next: Ron installs the debug APK from GitHub Actions
on his phone and checks the lock screen, widget and notifications on a real device.

## Decisions
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

## Next
- [ ] Test on a real phone (Samsung/Xiaomi lock screen, notification look, widget)
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
