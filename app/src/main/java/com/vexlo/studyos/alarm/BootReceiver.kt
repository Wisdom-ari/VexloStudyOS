package com.vexlo.studyos.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vexlo.studyos.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms every alarm after reboot, app update, clock change or time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Notifications.createChannels(app)
                AlarmScheduler.scheduleAll(app, AppDatabase.get(app).dao().alarmsOnce())
                DailyWorker.enqueue(app)
            } finally {
                pending.finish()
            }
        }
    }
}
