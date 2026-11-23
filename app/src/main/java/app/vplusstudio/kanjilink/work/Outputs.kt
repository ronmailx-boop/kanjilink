package app.vplusstudio.kanjilink.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.vplusstudio.kanjilink.MainActivity
import app.vplusstudio.kanjilink.R
import app.vplusstudio.kanjilink.data.Kanji
import app.vplusstudio.kanjilink.data.Prefs
import app.vplusstudio.kanjilink.widget.KanjiWidgetProvider
import java.time.LocalTime

/** Pushes the current kanji to every surface: notification, lock screen and widget. */
object Outputs {
    const val CHANNEL = "kanji"
    private const val NOTIFICATION_ID = 1

    fun refreshAll(context: Context, kanji: Kanji, notify: Boolean) {
        val prefs = Prefs(context)
        if (notify && prefs.notificationsOn && !prefs.isQuietHour(LocalTime.now().hour)) showNotification(context, kanji)
        if (prefs.lockWallpaperOn) setLockWallpaper(context, kanji)
        updateWidgets(context)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = context.getString(R.string.channel_desc)
            ch.setSound(null, null)
            ch.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
    }

    fun showNotification(context: Context, k: Kanji) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${k.char}  ·  ${k.meaningLine}")
            .setContentText(k.readingLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${k.readingLine}\nJLPT N${k.level} · ${k.strokes} strokes"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.again), answerIntent(context, k.char, false))
            .addAction(0, context.getString(R.string.know_it), answerIntent(context, k.char, true))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        } catch (e: SecurityException) {
            Log.w("Kanjilink", "notification permission revoked", e)
        }
    }

    fun cancelNotification(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun answerIntent(context: Context, char: String, known: Boolean): PendingIntent {
        val i = Intent(context, AnswerReceiver::class.java)
            .putExtra(AnswerReceiver.EXTRA_KANJI, char)
            .putExtra(AnswerReceiver.EXTRA_KNOWN, known)
        return PendingIntent.getBroadcast(context, if (known) 1 else 2, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Returns false when the phone refused (some launchers block lock-screen wallpapers). */
    fun setLockWallpaper(context: Context, k: Kanji): Boolean = try {
        val wm = WallpaperManager.getInstance(context)
        val dm = context.resources.displayMetrics
        val w = minOf(dm.widthPixels, dm.heightPixels)
        val h = maxOf(dm.widthPixels, dm.heightPixels)
        val bmp = KanjiRenderer.wallpaper(k, w, h, Prefs(context).wallpaperTheme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        } else {
            wm.setBitmap(bmp)
        }
        bmp.recycle()
        true
    } catch (e: Exception) {
        Log.w("Kanjilink", "lock wallpaper failed", e)
        false
    }

    fun updateWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, KanjiWidgetProvider::class.java))
        if (ids.isNotEmpty()) KanjiWidgetProvider.update(context, mgr, ids)
    }
}
