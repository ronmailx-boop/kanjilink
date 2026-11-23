package app.vplusstudio.kanjilink.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One kanji from KANJIDIC2 (EDRDG, CC BY-SA 4.0). */
data class Kanji(
    val char: String,
    val level: Int, // JLPT N-level: 5 (easiest) .. 1
    val strokes: Int,
    val on: List<String>,
    val onRomaji: List<String>,
    val kun: List<String>,
    val kunRomaji: List<String>,
    val meanings: List<String>,
) {
    val meaningLine: String get() = meanings.take(3).joinToString(", ")

    /** "ニチ nichi · ひ hi": first two readings of each kind, skipping forms like みず- that repeat みず. */
    val readingLine: String
        get() {
            fun norm(r: String) = r.filter { it != '-' && it != '(' && it != ')' }
            val parts = mutableListOf<String>()
            val seen = mutableSetOf<String>()
            fun add(kana: String, romaji: String) {
                if (seen.add(norm(kana))) parts += "$kana $romaji"
            }
            on.indices.take(2).forEach { add(on[it], onRomaji[it]) }
            kun.indices.filter { norm(kun[it]) !in seen }.take(2).forEach { add(kun[it], kunRomaji[it]) }
            return parts.joinToString(" · ")
        }
}

object KanjiRepository {
    @Volatile private var cache: List<Kanji>? = null
    @Volatile var dataVersion: String = ""
        private set

    fun all(context: Context): List<Kanji> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val text = context.assets.open("kanji.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            dataVersion = root.optJSONObject("source")?.optString("database_version").orEmpty()
            val arr = root.getJSONArray("kanji")
            val list = ArrayList<Kanji>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Kanji(
                    char = o.getString("k"),
                    level = o.getInt("n"),
                    strokes = o.getInt("s"),
                    on = o.getJSONArray("on").strings(),
                    onRomaji = o.getJSONArray("onr").strings(),
                    kun = o.getJSONArray("kun").strings(),
                    kunRomaji = o.getJSONArray("kunr").strings(),
                    meanings = o.getJSONArray("m").strings(),
                )
            }
            cache = list
            return list
        }
    }

    fun byChar(context: Context, c: String): Kanji? = all(context).firstOrNull { it.char == c }

    /** Study path: from the chosen start level up to N1, easiest first. */
    fun path(context: Context, startLevel: Int): List<Kanji> = all(context).filter { it.level <= startLevel }

    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
}
