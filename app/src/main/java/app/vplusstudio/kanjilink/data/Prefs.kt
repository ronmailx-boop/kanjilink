package app.vplusstudio.kanjilink.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("kanjilink", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = p.getBoolean("onboarded", false)
        set(v) = p.edit().putBoolean("onboarded", v).apply()

    /** JLPT level to start from: 5 = N5 ... 1 = N1. */
    var startLevel: Int
        get() = p.getInt("startLevel", 5)
        set(v) = p.edit().putInt("startLevel", v).apply()

    var newPerDay: Int
        get() = p.getInt("newPerDay", 5)
        set(v) = p.edit().putInt("newPerDay", v).apply()

    var notificationsOn: Boolean
        get() = p.getBoolean("notificationsOn", true)
        set(v) = p.edit().putBoolean("notificationsOn", v).apply()

    /** Hours between kanji changes (lock screen, widget, notification). */
    var intervalHours: Int
        get() = p.getInt("intervalHours", 3)
        set(v) = p.edit().putInt("intervalHours", v).apply()

    var quietStart: Int
        get() = p.getInt("quietStart", 22)
        set(v) = p.edit().putInt("quietStart", v).apply()

    var quietEnd: Int
        get() = p.getInt("quietEnd", 8)
        set(v) = p.edit().putInt("quietEnd", v).apply()

    var lockWallpaperOn: Boolean
        get() = p.getBoolean("lockWallpaperOn", false)
        set(v) = p.edit().putBoolean("lockWallpaperOn", v).apply()

    var wallpaperTheme: Int
        get() = p.getInt("wallpaperTheme", 0)
        set(v) = p.edit().putInt("wallpaperTheme", v).apply()

    /** Kanji typeface: 0 = Sans (gothic, default, closest to handwriting), 1 = Serif (mincho, book style). */
    var kanjiStyle: Int
        get() = p.getInt("kanjiStyle", 0)
        set(v) = p.edit().putInt("kanjiStyle", v).apply()

    /** Pro unlocked (one-time purchase). Cached; Google Play is the source of truth at every start. */
    var isPro: Boolean
        get() = p.getBoolean("isPro", false)
        set(v) = p.edit().putBoolean("isPro", v).apply()

    var currentKanji: String?
        get() = p.getString("currentKanji", null)
        set(v) = p.edit().putString("currentKanji", v).apply()

    var streak: Int
        get() = p.getInt("streak", 0)
        set(v) = p.edit().putInt("streak", v).apply()

    var lastActiveDay: Long
        get() = p.getLong("lastActiveDay", -1)
        set(v) = p.edit().putLong("lastActiveDay", v).apply()

    fun isQuietHour(hour: Int): Boolean =
        if (quietStart == quietEnd) false
        else if (quietStart < quietEnd) hour in quietStart until quietEnd
        else hour >= quietStart || hour < quietEnd
}
