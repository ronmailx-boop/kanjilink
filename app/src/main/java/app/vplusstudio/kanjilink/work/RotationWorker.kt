package app.vplusstudio.kanjilink.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.vplusstudio.kanjilink.data.Prefs
import app.vplusstudio.kanjilink.data.Study
import java.util.concurrent.TimeUnit

/** Every N hours: move to the next kanji and refresh notification, lock screen and widget. */
class RotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val next = Study.advance(applicationContext)
        Outputs.refreshAll(applicationContext, next, notify = true)
        return Result.success()
    }

    companion object {
        private const val NAME = "kanji-rotation"

        /** Call after onboarding and whenever the interval changes. */
        fun schedule(context: Context, replace: Boolean = false) {
            val hours = Prefs(context).intervalHours.toLong().coerceAtLeast(1)
            val req = PeriodicWorkRequestBuilder<RotationWorker>(hours, TimeUnit.HOURS)
                .setInitialDelay(hours, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}
