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
        KanjiRenderer.paletteNames.forEachIndexed { i, n -> save(KanjiRenderer.wallpaper(k, 1080, 2340, i), "wallpaper-$n") }
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
    fun home() {
        val k = KanjiRepository.all(ctx).first { it.char == "学" }
        compose.setContent {
            KanjilinkTheme {
                Home(k, Study.Stats(5, 12, 79, 3, 2, 4), lockOn = true, {}, {}, {}, {})
            }
        }
        capture("home")
    }
}
