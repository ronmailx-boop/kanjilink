package app.vplusstudio.kanjilink.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.vplusstudio.kanjilink.data.Study

/** Handles "Again" / "I know it" from the notification without opening the app. */
class AnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val char = intent.getStringExtra(EXTRA_KANJI) ?: return
        val known = intent.getBooleanExtra(EXTRA_KNOWN, true)
        val pending = goAsync()
        Thread {
            try {
                Study.answer(context, char, known)
                Outputs.cancelNotification(context)
                val next = Study.advance(context)
                Outputs.refreshAll(context, next, notify = false)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_KANJI = "kanji"
        const val EXTRA_KNOWN = "known"
    }
}
