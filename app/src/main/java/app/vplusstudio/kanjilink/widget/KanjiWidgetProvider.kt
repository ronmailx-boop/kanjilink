package app.vplusstudio.kanjilink.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import app.vplusstudio.kanjilink.MainActivity
import app.vplusstudio.kanjilink.R
import app.vplusstudio.kanjilink.data.Kanji
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.work.KanjiRenderer

/**
 * Home-screen widget. Small (2x2): level, kanji, meaning, reading. Wide (from about 4 cells):
 * kanji left, details right. Colours follow the phone's light/dark mode (values-night).
 */
class KanjiWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { update(context, manager, ids) }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        async { update(context, manager, intArrayOf(id)) }

    private fun async(block: () -> Unit) {
        val pending = goAsync()
        Thread { try { block() } finally { pending.finish() } }.start()
    }

    companion object {
        private const val WIDE_MIN_DP = 220

        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val k = Study.current(context)
            for (id in ids) {
                val views = if (Build.VERSION.SDK_INT >= 31) {
                    RemoteViews(
                        mapOf(
                            SizeF(110f, 60f) to build(context, k, wide = false),
                            SizeF(WIDE_MIN_DP.toFloat(), 60f) to build(context, k, wide = true),
                        ),
                    )
                } else {
                    val minW = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                    build(context, k, wide = minW >= WIDE_MIN_DP)
                }
                manager.updateAppWidget(id, views)
            }
        }

        internal fun build(context: Context, k: Kanji, wide: Boolean): RemoteViews {
            // Drawn white; the layout tints it with @color/widget_ink, so it follows light/dark mode.
            val size = ((if (wide) 76 else 64) * context.resources.displayMetrics.density).toInt()
            val glyph = KanjiRenderer.glyph(k, size, Color.WHITE, KanjiRenderer.kanjiTypeface(context))
            val open = PendingIntent.getActivity(
                context, 10, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return RemoteViews(context.packageName, if (wide) R.layout.widget_kanji_wide else R.layout.widget_kanji).apply {
                setImageViewBitmap(R.id.widget_glyph, glyph)
                setContentDescription(R.id.widget_glyph, k.char)
                setTextViewText(R.id.widget_level, if (wide) "JLPT N${k.level}" else "N${k.level}")
                setTextViewText(R.id.widget_meaning, if (wide) k.meaningLine else k.meanings.first())
                setTextViewText(R.id.widget_reading, if (wide) k.readingLine else shortReading(k))
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }

        /** Small widget: first on and first kun reading in romaji ("gaku · mana(bu)"). */
        private fun shortReading(k: Kanji): String =
            listOfNotNull(k.onRomaji.firstOrNull(), k.kunRomaji.firstOrNull()).joinToString(" · ")
    }
}
