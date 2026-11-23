package app.vplusstudio.kanjilink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.vplusstudio.kanjilink.data.KanjiRepository
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.ui.Home
import app.vplusstudio.kanjilink.ui.KanjilinkTheme
import app.vplusstudio.kanjilink.ui.Onboarding
import app.vplusstudio.kanjilink.work.KanjiRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders screens and the wallpaper to build/screenshots for a visual check. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/screenshots").apply { mkdirs() }

    /** Draws the activity's content view (Compose's own capture times out under Robolectric). */
    private fun capture(name: String) {
        compose.waitForIdle()
        val v = compose.activity.findViewById<View>(android.R.id.content)
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        save(b, name)
    }

    private fun save(b: Bitmap, name: String) = File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test
    fun wallpapers() {
        val k = KanjiRepository.all(ctx).first { it.char == "学" }
        KanjiRenderer.paletteNames.forEachIndexed { i, n -> save(KanjiRenderer.wallpaper(k, 1080, 2340, i, KanjiRenderer.kanjiTypeface(ctx)), "wallpaper-$n") }
    }

    /** Launcher icon (circle mask) and both widget sizes, light and dark. */
    @Test
    fun iconAndWidgets() = renderIconAndWidgets("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun iconAndWidgetsNight() = renderIconAndWidgets("night")

    private fun renderIconAndWidgets(tag: String) {
        val icon = ctx.getDrawable(R.mipmap.ic_launcher)!!
        val ib = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        val ic = Canvas(ib)
        val path = android.graphics.Path().apply { addCircle(216f, 216f, 216f, android.graphics.Path.Direction.CW) }
        ic.clipPath(path)
        icon.setBounds(0, 0, 432, 432)
        icon.draw(ic)
        save(ib, "icon")

        val k = KanjiRepository.all(ctx).first { it.char == "学" }
        Study.answer(ctx, k.char, true)
        app.vplusstudio.kanjilink.data.Prefs(ctx).currentKanji = k.char
        for ((name, wide, wDp) in listOf(Triple("small", false, 170), Triple("wide", true, 340))) {
            val rv = app.vplusstudio.kanjilink.widget.KanjiWidgetProvider.build(ctx, k, wide)
            val parent = android.widget.FrameLayout(ctx)
            val v = rv.apply(ctx, parent)
            val d = ctx.resources.displayMetrics.density
            val w = (wDp * d).toInt(); val h = (170 * d).toInt()
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b))
            save(b, "widget-$name-$tag")
        }
    }

    @Test
    fun onboarding() {
        compose.setContent { KanjilinkTheme { Onboarding { _, _ -> } } }
        capture("onboarding")
    }

    /** Ron's phone: Hebrew locale, large font, dark mode. Chips must wrap, text must stay LTR. */
    @Test
    @Config(qualifiers = "iw-ldrtl-w240dp-h891dp-night-xxhdpi", fontScale = 1.6f)
    fun settingsHebrewPhoneLargeFont() {
        compose.setContent {
            KanjilinkTheme {
                app.vplusstudio.kanjilink.ui.Settings(
                    app.vplusstudio.kanjilink.ui.SettingsState(true, 3, 22, 8, true, 0, 5, 5), {}, {}, {},
                )
            }
        }
        capture("settings-he-large-font")
    }

    @Test
    @Config(qualifiers = "iw-ldrtl-w411dp-h891dp-night-xxhdpi", fontScale = 1.6f)
    fun onboardingHebrewPhoneLargeFont() {
        compose.setContent { KanjilinkTheme { Onboarding { _, _ -> } } }
        capture("onboarding-he-large-font")
    }

    @Test
    fun homeSelfTest() {
        val k = KanjiRepository.all(ctx).first { it.char == "一" }
        compose.setContent {
            KanjilinkTheme {
                Home(k, Study.Stats(5, 2, 79, 0, 2, 0), lockOn = false, revealed = false, isNew = false,
                    preview = "10 min" to "1 day", {}, {}, {}, {}, {})
            }
        }
        capture("home-self-test")
    }

    /** Large font on a narrow screen: captions and readings must wrap, not be cut. */
    @Test
    @Config(qualifiers = "w260dp-h891dp-night-xxhdpi")
    fun homeNarrow() {
        val k = KanjiRepository.all(ctx).first { it.char == "一" }
        compose.setContent {
            KanjilinkTheme {
                Home(k, Study.Stats(5, 2, 79, 0, 2, 1), lockOn = false, revealed = true, isNew = true,
                    preview = "10 min" to "1 day", {}, {}, {}, {}, {})
            }
        }
        capture("home-narrow")
    }

    @Test
    fun settingsKanjiStyle() {
        compose.setContent {
            KanjilinkTheme {
                app.vplusstudio.kanjilink.ui.Settings(
                    app.vplusstudio.kanjilink.ui.SettingsState(true, 3, 22, 8, true, 0, 5, 5, kanjiStyle = 0), {}, {}, {},
                )
            }
        }
        capture("settings")
    }

    @Test
    fun homeSerif() {
        app.vplusstudio.kanjilink.ui.KanjiFontState.style = 1
        try {
            val k = KanjiRepository.all(ctx).first { it.char == "国" }
            compose.setContent {
                KanjilinkTheme {
                    Home(k, Study.Stats(5, 12, 79, 3, 2, 4), lockOn = true, revealed = true, isNew = false,
                        preview = "10 min" to "3 days", {}, {}, {}, {}, {})
                }
            }
            capture("home-serif")
        } finally {
            app.vplusstudio.kanjilink.ui.KanjiFontState.style = 0
        }
    }

    @Test
    fun home() {
        val k = KanjiRepository.all(ctx).first { it.char == "学" }
        compose.setContent {
            KanjilinkTheme {
                Home(k, Study.Stats(5, 12, 79, 3, 2, 4), lockOn = true, revealed = true, isNew = false,
                    preview = "10 min" to "3 days", {}, {}, {}, {}, {})
            }
        }
        capture("home")
    }
}
