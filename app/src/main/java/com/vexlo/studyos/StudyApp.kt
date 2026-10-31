package com.vexlo.studyos

import android.app.Application
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.alarm.DailyWorker
import com.vexlo.studyos.alarm.Notifications
import com.vexlo.studyos.data.AppDatabase
import com.vexlo.studyos.data.seedIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(this@StudyApp).dao()
            seedIfEmpty(dao)
            // Re-arm on every app start (covers force-stop and restore-from-backup).
            AlarmScheduler.scheduleAll(this@StudyApp, dao.alarmsOnce())
            DailyWorker.enqueue(this@StudyApp)
        }
    }
}
