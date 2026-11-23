package app.vplusstudio.kanjilink.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import app.vplusstudio.kanjilink.MainActivity
import app.vplusstudio.kanjilink.R
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.work.KanjiRenderer

class KanjiWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Thread {
            try { update(context, manager, ids) } finally { pending.finish() }
        }.start()
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val k = Study.current(context)
            val size = (96 * context.resources.displayMetrics.density).toInt()
            val glyph = KanjiRenderer.glyph(k, size, Color.WHITE)
            val open = PendingIntent.getActivity(
                context, 10, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            for (id in ids) {
                val v = RemoteViews(context.packageName, R.layout.widget_kanji)
                v.setImageViewBitmap(R.id.widget_glyph, glyph)
                v.setContentDescription(R.id.widget_glyph, k.char)
                v.setTextViewText(R.id.widget_meaning, k.meaningLine)
                v.setTextViewText(R.id.widget_reading, k.readingLine)
                v.setOnClickPendingIntent(R.id.widget_root, open)
                manager.updateAppWidget(id, v)
            }
        }
    }
}
