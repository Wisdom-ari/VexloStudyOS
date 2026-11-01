package com.vexlo.studyos

import android.app.Application
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.alarm.DailyWorker
import com.vexlo.studyos.alarm.Notifications
import com.vexlo.studyos.data.AppDatabase
import com.vexlo.studyos.data.PrefKeys
import com.vexlo.studyos.data.prefsFlow
import com.vexlo.studyos.data.seedIfEmpty
import com.vexlo.studyos.data.seedPapers
import com.vexlo.studyos.data.updatePrefs
import kotlinx.coroutines.flow.first
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
            if (!prefsFlow().first().papersSeeded) {
                seedPapers(dao)
                updatePrefs { this[PrefKeys.PAPERS_SEEDED] = true }
            }
            // Re-arm on every app start (covers force-stop and restore-from-backup).
            AlarmScheduler.scheduleAll(this@StudyApp, dao.alarmsOnce())
            DailyWorker.enqueue(this@StudyApp)
        }
    }
}
