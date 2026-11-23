package app.vplusstudio.kanjilink.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/** Spaced-repetition state for one kanji (a light SM-2 variant). */
data class Card(
    val due: Long,
    val intervalDays: Double,
    val ease: Double,
    val reps: Int,
    val introducedDay: Long,
)

/**
 * All study logic. Progress lives only on the device (filesDir/cards.json + prefs).
 * Calls are synchronized because the worker, the notification actions and the UI share it.
 */
object Study {
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE

    private fun file(context: Context) = File(context.filesDir, "cards.json")

    private fun today(): Long = LocalDate.now().toEpochDay()

    @Synchronized
    fun cards(context: Context): MutableMap<String, Card> {
        val f = file(context)
        if (!f.exists()) return mutableMapOf()
        return try {
            val o = JSONObject(f.readText())
            val map = mutableMapOf<String, Card>()
            o.keys().forEach { k ->
                val a = o.getJSONArray(k)
                map[k] = Card(a.getLong(0), a.getDouble(1), a.getDouble(2), a.getInt(3), a.getLong(4))
            }
            map
        } catch (e: Exception) {
            mutableMapOf() // corrupt file: start fresh rather than crash
        }
    }

    @Synchronized
    private fun save(context: Context, cards: Map<String, Card>) {
        val o = JSONObject()
        cards.forEach { (k, c) ->
            o.put(k, JSONArray().put(c.due).put(c.intervalDays).put(c.ease).put(c.reps).put(c.introducedDay))
        }
        val tmp = File(context.filesDir, "cards.json.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file(context))
    }

    /** learned/total are for the level being studied now (for example N5: 12/79). */
    data class Stats(val level: Int, val learned: Int, val total: Int, val due: Int, val newToday: Int, val streak: Int)

    @Synchronized
    fun stats(context: Context): Stats {
        val prefs = Prefs(context)
        val path = KanjiRepository.path(context, prefs.startLevel)
        val cards = cards(context)
        val now = System.currentTimeMillis()
        val level = path.firstOrNull { !cards.containsKey(it.char) }?.level ?: path.last().level
        val inLevel = path.filter { it.level == level }
        val learned = inLevel.count { cards.containsKey(it.char) }
        val due = path.count { cards[it.char]?.let { c -> c.due <= now } == true }
        val newToday = cards.values.count { it.introducedDay == today() }
        val streak = if (prefs.lastActiveDay >= today() - 1) prefs.streak else 0
        return Stats(level, learned, inLevel.size, due, newToday, streak)
    }

    /** Kanji currently on the lock screen / widget / notification. */
    @Synchronized
    fun current(context: Context): Kanji {
        val prefs = Prefs(context)
        prefs.currentKanji?.let { c -> KanjiRepository.byChar(context, c)?.let { return it } }
        return advance(context)
    }

    /**
     * Picks the next kanji to show: an overdue card first, then a new kanji if today's
     * allowance is not used up, then a random already-introduced kanji as a light refresher.
     */
    @Synchronized
    fun advance(context: Context): Kanji {
        val prefs = Prefs(context)
        val path = KanjiRepository.path(context, prefs.startLevel)
        val cards = cards(context)
        val now = System.currentTimeMillis()
        val previous = prefs.currentKanji

        val dueCard = path
            .filter { it.char != previous }
            .mapNotNull { k -> cards[k.char]?.takeIf { it.due <= now }?.let { k to it } }
            .minByOrNull { it.second.due }?.first

        val pick = dueCard ?: run {
            val newToday = cards.values.count { it.introducedDay == today() }
            val next = path.firstOrNull { !cards.containsKey(it.char) }
            if (newToday < prefs.newPerDay && next != null) {
                cards[next.char] = Card(now + 4 * 60 * MINUTE, 0.0, 2.5, 0, today())
                save(context, cards)
                next
            } else {
                path.filter { cards.containsKey(it.char) && it.char != previous }.randomOrNull()
                    ?: next ?: path.first()
            }
        }
        prefs.currentKanji = pick.char
        return pick
    }

    /** Days until the next review if the answer is "I know it". */
    private fun nextInterval(old: Card): Double = if (old.intervalDays < 1.0) 1.0 else old.intervalDays * old.ease

    /** True when the kanji has never been answered: show it fully instead of testing. */
    @Synchronized
    fun isNew(context: Context, char: String): Boolean = (cards(context)[char]?.reps ?: 0) == 0

    /** Button captions: when the kanji comes back after "Again" / "I know it". */
    @Synchronized
    fun preview(context: Context, char: String): Pair<String, String> {
        val old = cards(context)[char] ?: Card(0, 0.0, 2.5, 0, today())
        return "10 min" to formatDays(nextInterval(old))
    }

    fun formatDays(days: Double): String {
        val d = days.roundToLong().coerceAtLeast(1)
        return when {
            d == 1L -> "1 day"
            d < 14 -> "$d days"
            d < 60 -> "${(d + 3) / 7} weeks"
            d < 365 -> "${(d + 15) / 30} months"
            else -> "over a year"
        }
    }

    /** Records an answer. known=false means "show it again soon". Returns when it comes back. */
    @Synchronized
    fun answer(context: Context, char: String, known: Boolean): String {
        val cards = cards(context)
        val now = System.currentTimeMillis()
        val old = cards[char] ?: Card(now, 0.0, 2.5, 0, today())
        val updated = if (known) {
            val interval = nextInterval(old)
            old.copy(
                due = now + (interval * DAY).roundToLong(),
                intervalDays = interval,
                ease = min(3.0, old.ease + 0.05),
                reps = old.reps + 1,
            )
        } else {
            old.copy(due = now + 10 * MINUTE, intervalDays = 0.0, ease = max(1.3, old.ease - 0.2), reps = old.reps + 1)
        }
        cards[char] = updated
        save(context, cards)
        touchStreak(context)
        return if (known) formatDays(updated.intervalDays) else "10 min"
    }

    /** Due kanji for an in-app review session, oldest first. */
    @Synchronized
    fun dueList(context: Context): List<Kanji> {
        val prefs = Prefs(context)
        val cards = cards(context)
        val now = System.currentTimeMillis()
        return KanjiRepository.path(context, prefs.startLevel)
            .mapNotNull { k -> cards[k.char]?.takeIf { it.due <= now }?.let { k to it.due } }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun touchStreak(context: Context) {
        val prefs = Prefs(context)
        val t = today()
        when (prefs.lastActiveDay) {
            t -> return
            t - 1 -> prefs.streak = prefs.streak + 1
            else -> prefs.streak = 1
        }
        prefs.lastActiveDay = t
    }
}
