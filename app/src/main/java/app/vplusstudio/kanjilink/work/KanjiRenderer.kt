package app.vplusstudio.kanjilink.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import app.vplusstudio.kanjilink.R
import app.vplusstudio.kanjilink.data.Kanji
import java.util.Locale

/** Draws a kanji card into a bitmap, for the lock-screen wallpaper and the widget. */
object KanjiRenderer {
    data class Palette(val background: Int, val ink: Int, val accent: Int, val muted: Int)

    val palettes = listOf(
        Palette(Color.rgb(17, 18, 22), Color.rgb(244, 241, 234), Color.rgb(214, 77, 64), Color.rgb(160, 158, 152)), // Night
        Palette(Color.rgb(245, 240, 229), Color.rgb(28, 27, 25), Color.rgb(190, 50, 40), Color.rgb(110, 105, 98)), // Paper
        Palette(Color.rgb(20, 36, 52), Color.rgb(236, 242, 247), Color.rgb(242, 176, 72), Color.rgb(150, 170, 188)), // Indigo
    )
    val paletteNames = listOf("Night", "Paper", "Indigo")

    @Volatile private var kanjiTypeface: Typeface? = null

    /** Noto Serif JP Bold, bundled; falls back to the system font if it cannot load. */
    fun kanjiTypeface(context: Context): Typeface =
        kanjiTypeface ?: (runCatching { ResourcesCompat.getFont(context, R.font.noto_serif_jp_bold) }.getOrNull()
            ?: Typeface.DEFAULT_BOLD).also { kanjiTypeface = it }

    private fun paint(color: Int, sizePx: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizePx
        textAlign = Paint.Align.CENTER
        // Japanese glyph shapes, even when the phone's language is Chinese.
        textLocale = Locale.JAPAN
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    /**
     * Lock-screen layout: the clock sits in the top third on most phones, so the kanji is
     * drawn slightly below the centre, with readings and meaning under it.
     */
    fun wallpaper(k: Kanji, width: Int, height: Int, theme: Int, kanjiFace: Typeface = Typeface.DEFAULT): Bitmap {
        val p = palettes[theme.coerceIn(palettes.indices)]
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(p.background)
        val cx = width / 2f
        val unit = width / 100f

        val kanjiPaint = paint(p.ink, unit * 46f).apply { typeface = kanjiFace }
        val kanjiBaseline = height * 0.60f
        c.drawText(k.char, cx, kanjiBaseline, kanjiPaint)

        val levelPaint = paint(p.accent, unit * 3.6f, bold = true).apply { letterSpacing = 0.2f }
        c.drawText("JLPT N${k.level}", cx, kanjiBaseline - unit * 46f, levelPaint)

        val meaningPaint = paint(p.ink, unit * 6f, bold = true)
        var y = kanjiBaseline + unit * 14f
        y = drawWrapped(c, k.meaningLine, meaningPaint, cx, y, (width * 0.84f).toInt())

        val readingPaint = paint(p.muted, unit * 4.2f)
        drawWrapped(c, k.readingLine, readingPaint, cx, y + unit * 3f, (width * 0.84f).toInt())

        val brand = paint(p.muted, unit * 2.8f).apply { letterSpacing = 0.15f }
        c.drawText("KANJILINK", cx, height * 0.93f, brand)
        return bmp
    }

    /** Square glyph image for the widget (RemoteViews cannot set a text locale). */
    fun glyph(k: Kanji, sizePx: Int, color: Int, kanjiFace: Typeface = Typeface.DEFAULT): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = paint(color, sizePx * 0.86f).apply { typeface = kanjiFace }
        val fm = paint.fontMetrics
        val baseline = sizePx / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(k.char, sizePx / 2f, baseline, paint)
        return bmp
    }

    private fun drawWrapped(c: Canvas, text: String, paint: TextPaint, cx: Float, top: Float, width: Int): Float {
        val left = TextPaint(paint).apply { textAlign = Paint.Align.LEFT }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, left, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .build()
        c.save()
        c.translate(cx - width / 2f, top)
        layout.draw(c)
        c.restore()
        return top + layout.height
    }
}
