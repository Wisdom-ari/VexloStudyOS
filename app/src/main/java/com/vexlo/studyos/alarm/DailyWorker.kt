package com.vexlo.studyos.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vexlo.studyos.data.AppDatabase
import com.vexlo.studyos.data.PrefKeys
import com.vexlo.studyos.data.prefsFlow
import com.vexlo.studyos.data.updatePrefs
import com.vexlo.studyos.domain.Logic
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Safety net that runs about every 6 hours: re-arms all alarms (in case the OEM battery manager
 * cleared them) and posts the exam-preparation and final-revision reminders once per day.
 */
class DailyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val dao = AppDatabase.get(app).dao()
        AlarmScheduler.scheduleAll(app, dao.alarmsOnce())

        val prefs = app.prefsFlow().first()
        val today = Logic.today()
        val iso = today.toString()
        if (prefs.lastReminder == iso) return Result.success()

        val exam = Logic.parse(prefs.examDate, today)
        val prep = Logic.parse(prefs.prepStart, today)
        val daysLeft = ChronoUnit.DAYS.between(today, exam)

        var posted = false
        if (today == prep) {
            Notifications.reminder(app, 6001, "Exam preparation starts today", "$daysLeft days to your first exam. Stick to the timetable.")
            posted = true
        } else if (daysLeft in 0..7) {
            val topics = dao.topicsOnce().filter { it.weak && !it.done }.take(3).joinToString { it.title }
            val extra = if (topics.isNotEmpty()) " Weak topics: $topics." else " Revise your checklist and weak topics."
            val head = if (daysLeft == 0L) "Exam day - good luck!" else "Final revision: $daysLeft day(s) to the exams"
            Notifications.reminder(app, 6002, head, "Do a mock paper today.$extra")
            posted = true
        }
        if (posted) app.updatePrefs { this[PrefKeys.LAST_REMINDER] = iso }
        return Result.success()
    }

    companion object {
        fun enqueue(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<DailyWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("vexlo_daily", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
