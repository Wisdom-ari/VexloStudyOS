package com.vexlo.studyos.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vexlo.studyos.R

object Notifications {
    const val CH_ALARM = "vexlo_alarm"
    const val CH_REMINDER = "vexlo_reminder"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        // Sound is played by AlarmService (so custom sounds work), the channel itself is silent.
        val alarm = NotificationChannel(CH_ALARM, "Study alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Wake-up, study session, break and revision alarms"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val reminder = NotificationChannel(CH_REMINDER, "Session and exam reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Session-start notices and final revision reminders"
        }
        nm.createNotificationChannel(alarm)
        nm.createNotificationChannel(reminder)
    }

    fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    fun reminder(ctx: Context, id: Int, title: String, text: String) {
        if (!canPost(ctx)) return
        val open = android.app.PendingIntent.getActivity(
            ctx, 0, android.content.Intent(ctx, com.vexlo.studyos.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}
