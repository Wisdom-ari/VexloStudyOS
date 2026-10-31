package com.vexlo.studyos.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.vexlo.studyos.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"

        // Ring + notify through the foreground service.
        val svc = Intent(app, AlarmService::class.java).apply { putExtras(intent) }
        try {
            ContextCompat.startForegroundService(app, svc)
        } catch (_: Exception) {
            Notifications.reminder(app, 7000, "Vexlo StudyOS", "An alarm just fired - open the app.")
        }

        // Weekly repeat: schedule the next occurrence of this alarm.
        if (kind == "alarm" && id > 0) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AppDatabase.get(app).dao().getAlarm(id)?.let { AlarmScheduler.schedule(app, it) }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
