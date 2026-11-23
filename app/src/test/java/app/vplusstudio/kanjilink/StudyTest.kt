package app.vplusstudio.kanjilink

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.vplusstudio.kanjilink.data.KanjiRepository
import app.vplusstudio.kanjilink.data.Prefs
import app.vplusstudio.kanjilink.data.Study
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyTest {
    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        Prefs(ctx).apply { startLevel = 5; newPerDay = 3; currentKanji = null }
    }

    @Test
    fun dataLoadsAllLevels() {
        val all = KanjiRepository.all(ctx)
        assertEquals(2211, all.size)
        assertEquals(79, all.count { it.level == 5 })
        assertTrue(KanjiRepository.dataVersion.isNotEmpty())
        assertEquals(2211, KanjiRepository.path(ctx, 5).size) // N5 start = whole path N5..N1
        assertEquals(2211 - 79, KanjiRepository.path(ctx, 4).size)
        assertEquals("まな(ぶ)", KanjiRepository.byChar(ctx, "学")!!.kun.first())
    }

    @Test
    fun newKanjiRespectDailyLimitThenRefresherIsShown() {
        val seen = (1..3).map { Study.advance(ctx).char }
        assertEquals(3, seen.toSet().size) // three different new kanji, N5 order
        assertEquals("日", seen[0])
        assertEquals(3, Study.stats(ctx).newToday)
        val fourth = Study.advance(ctx)
        assertTrue(fourth.char in seen) // allowance used: shows an introduced kanji
        val st = Study.stats(ctx)
        assertEquals(5, st.level)
        assertEquals(3, st.learned)
        assertEquals(79, st.total)
    }

    @Test
    fun answersScheduleReviewsAndStreak() {
        val k = Study.advance(ctx)
        Study.answer(ctx, k.char, known = true)
        val card = Study.cards(ctx)[k.char]!!
        assertTrue(card.due > System.currentTimeMillis() + 23 * 3_600_000L)
        assertEquals(1, Study.stats(ctx).streak)

        Study.answer(ctx, k.char, known = false)
        val again = Study.cards(ctx)[k.char]!!
        assertTrue(again.due < System.currentTimeMillis() + 11 * 60_000L)
        assertTrue(again.ease < card.ease)
        assertEquals(1, Study.stats(ctx).streak) // same day: no double count
    }

    @Test
    fun dueCardComesFirstAndNotTheSameTwiceInARow() {
        val a = Study.advance(ctx)
        Study.answer(ctx, a.char, known = false) // due in 10 min, not yet
        val b = Study.advance(ctx)
        assertNotEquals(a.char, b.char)
    }

    @Test
    fun previewMatchesAnswerAndGrows() {
        val k = Study.advance(ctx)
        assertTrue(Study.isNew(ctx, k.char))
        assertEquals("10 min" to "1 day", Study.preview(ctx, k.char))
        assertEquals("1 day", Study.answer(ctx, k.char, known = true))
        assertTrue(!Study.isNew(ctx, k.char))
        val next = Study.preview(ctx, k.char).second
        assertEquals(next, Study.answer(ctx, k.char, known = true)) // 1 day * 2.55 -> "3 days"
        assertEquals("3 days", next)
        assertEquals("10 min", Study.answer(ctx, k.char, known = false))
    }

    @Test
    fun radicalBookkeepingMeaningsRemoved() {
        val one = KanjiRepository.byChar(ctx, "一")!!
        assertEquals(listOf("one"), one.meanings)
    }

    @Test
    fun freeUsersStudyN5OnlyProGetsEverything() {
        val p = Prefs(ctx)
        p.isPro = false
        p.startLevel = 3 // ignored when not Pro
        assertEquals(79, Study.studyPath(ctx).size)
        assertTrue(Study.studyPath(ctx).all { it.level == 5 })
        p.isPro = true
        p.startLevel = 5
        assertEquals(2211, Study.studyPath(ctx).size)
        p.startLevel = 3
        assertEquals(367 + 367 + 1232, Study.studyPath(ctx).size)
        p.isPro = false
        p.startLevel = 5
    }

    @Test
    fun freeLevelCompleteAfterAllN5() {
        val p = Prefs(ctx).apply { isPro = false; newPerDay = 200 }
        assertTrue(!Study.freeLevelComplete(ctx))
        repeat(79) { Study.advance(ctx) }
        assertTrue(Study.freeLevelComplete(ctx))
        p.isPro = true
        assertTrue(!Study.freeLevelComplete(ctx))
        p.isPro = false
    }

    @Test
    fun readingLineSkipsRepeatedForms() {
        assertEquals("スイ sui · みず mizu", KanjiRepository.byChar(ctx, "水")!!.readingLine)
    }

    @Test
    fun reviewUnlockGivesProWithoutPurchase() {
        val p = Prefs(ctx)
        p.playPro = false
        p.reviewUnlock = true
        assertTrue(p.isPro)
        assertEquals(2211, Study.studyPath(ctx).size)
        p.reviewUnlock = false
        assertTrue(!p.isPro)
    }

    @Test
    fun kanjiStyleDefaultsToSans() {
        assertEquals(0, Prefs(ctx).kanjiStyle)
    }

    @Test
    fun quietHoursWrapMidnight() {
        val p = Prefs(ctx).apply { quietStart = 22; quietEnd = 8 }
        assertTrue(p.isQuietHour(23))
        assertTrue(p.isQuietHour(3))
        assertTrue(!p.isQuietHour(12))
    }
}
