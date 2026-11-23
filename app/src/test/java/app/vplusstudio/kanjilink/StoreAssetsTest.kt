package app.vplusstudio.kanjilink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.vplusstudio.kanjilink.data.KanjiRepository
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.ui.Home
import app.vplusstudio.kanjilink.ui.KanjilinkTheme
import app.vplusstudio.kanjilink.ui.Paywall
import app.vplusstudio.kanjilink.ui.Settings
import app.vplusstudio.kanjilink.ui.SettingsState
import app.vplusstudio.kanjilink.work.KanjiRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Raw images for the Google Play listing, at 1080x1920 (360x640 dp at xxhdpi), into build/store/raw.
 * tools/make_store_assets.py adds captions and writes store/en-US/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class StoreAssetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/store/raw").apply { mkdirs() }

    private fun save(b: Bitmap, name: String) = File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private fun capture(name: String) {
        compose.waitForIdle()
        val v = compose.activity.findViewById<View>(android.R.id.content)
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        save(b, name)
    }

    private fun k(c: String) = KanjiRepository.all(ctx).first { it.char == c }

    @Test
    fun icon512() {
        // Full-bleed square: Play applies its own rounded mask. The adaptive icon shows the middle 72 of 108 dp.
        val size = 512
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.parseColor("#C8402F"))
        val fg = ctx.getDrawable(R.drawable.ic_launcher_foreground)!!
        val full = size * 108 / 72
        val off = (full - size) / 2
        fg.setBounds(-off, -off, size + off, size + off)
        fg.draw(c)
        save(b, "icon-512")
    }

    @Test
    fun lockScreen() { save(KanjiRenderer.wallpaper(k("学"), 1080, 1920, 0, KanjiRenderer.kanjiTypeface(ctx, 0)), "lock") }

    @Test
    fun homeScreen() {
        compose.setContent {
            KanjilinkTheme {
                Home(k("学"), Study.Stats(5, 12, 79, 3, 2, 4), lockOn = true, revealed = true, isNew = false,
                    preview = "10 min" to "3 days", {}, {}, {}, {}, {})
            }
        }
        capture("home")
    }

    @Test
    fun selfTest() {
        compose.setContent {
            KanjilinkTheme {
                Home(k("国"), Study.Stats(5, 13, 79, 2, 3, 4), lockOn = true, revealed = false, isNew = false,
                    preview = "10 min" to "1 day", {}, {}, {}, {}, {})
            }
        }
        capture("self-test")
    }

    @Test
    fun settings() {
        compose.setContent {
            KanjilinkTheme { Settings(SettingsState(true, 3, 22, 8, true, 0, 5, 5), {}, {}, {}) }
        }
        capture("settings")
    }

    @Test
    fun paywall() {
        compose.setContent { KanjilinkTheme { Paywall(isPro = false, price = "$4.99", available = true, {}, {}, {}) } }
        capture("paywall")
    }

    @Test
    fun widgets() {
        Study.answer(ctx, "水", true)
        for ((name, wide) in listOf("widget-small" to false, "widget-wide" to true)) {
            val rv = app.vplusstudio.kanjilink.widget.KanjiWidgetProvider.build(ctx, k("水"), wide)
            val v = rv.apply(ctx, android.widget.FrameLayout(ctx))
            val d = ctx.resources.displayMetrics.density
            val w = ((if (wide) 330 else 160) * d).toInt(); val h = (160 * d).toInt()
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b))
            save(b, name)
        }
    }
}
