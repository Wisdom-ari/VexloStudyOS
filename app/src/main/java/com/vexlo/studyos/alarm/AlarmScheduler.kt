package com.vexlo.studyos.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vexlo.studyos.MainActivity
import com.vexlo.studyos.data.AlarmEntity
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules alarms with the native AlarmManager. setAlarmClock() is used because it is the most
 * reliable API on Android 14: it fires in Doze, is exempt from exact-alarm restrictions and can
 * start the foreground alarm service. Every fire re-schedules the next weekly occurrence.
 */
object AlarmScheduler {
    const val ACTION_FIRE = "com.vexlo.studyos.ALARM_FIRE"
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_KIND = "kind" // alarm | snooze | timer | test
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"

    private const val SNOOZE_BASE = 100_000
    private const val TIMER_CODE = 900_001
    private const val TEST_CODE = 900_002

    val zone: ZoneId = ZoneId.of("Africa/Kampala")

    fun nextTrigger(a: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now(zone)): ZonedDateTime? {
        if (a.daysMask == 0) return null
        for (i in 0..7) {
            val day = from.toLocalDate().plusDays(i.toLong())
            val bit = (a.daysMask shr (day.dayOfWeek.value - 1)) and 1
            if (bit == 0) continue
            val t = day.atTime(a.hour, a.minute).atZone(zone)
            if (t.isAfter(from)) return t
        }
        return null
    }

    private fun am(ctx: Context): AlarmManager = ctx.getSystemService(AlarmManager::class.java)

    private fun firePi(ctx: Context, code: Int, fill: Intent.() -> Unit): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("vexlo://alarm/$code")
            fill()
        }
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun showPi(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun setClock(ctx: Context, atMs: Long, pi: PendingIntent) {
        val mgr = am(ctx)
        try {
            mgr.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, showPi(ctx)), pi)
        } catch (_: SecurityException) {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun schedule(ctx: Context, a: AlarmEntity) {
        cancel(ctx, a.id)
        if (!a.enabled) return
        val t = nextTrigger(a) ?: return
        setClock(ctx, t.toInstant().toEpochMilli(), firePi(ctx, a.id.toInt()) {
            putExtra(EXTRA_ID, a.id)
            putExtra(EXTRA_KIND, "alarm")
        })
    }

    fun scheduleAll(ctx: Context, alarms: List<AlarmEntity>) = alarms.forEach { schedule(ctx, it) }

    fun cancel(ctx: Context, id: Long) {
        am(ctx).cancel(firePi(ctx, id.toInt()) { putExtra(EXTRA_ID, id) })
        am(ctx).cancel(firePi(ctx, SNOOZE_BASE + id.toInt()) { putExtra(EXTRA_ID, id) })
    }

    fun snooze(ctx: Context, id: Long, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        setClock(ctx, at, firePi(ctx, SNOOZE_BASE + id.toInt()) {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_KIND, "snooze")
        })
    }

    fun scheduleTimer(ctx: Context, atMs: Long, title: String, text: String) {
        setClock(ctx, atMs, firePi(ctx, TIMER_CODE) {
            putExtra(EXTRA_KIND, "timer")
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        })
    }

    fun cancelTimer(ctx: Context) {
        am(ctx).cancel(firePi(ctx, TIMER_CODE) {})
    }

    /** One-shot test alarm so the student can verify sound, notification and permissions. */
    fun scheduleTest(ctx: Context, seconds: Int = 20) {
        setClock(ctx, System.currentTimeMillis() + seconds * 1000L, firePi(ctx, TEST_CODE) {
            putExtra(EXTRA_KIND, "test")
            putExtra(EXTRA_TITLE, "Test alarm")
            putExtra(EXTRA_TEXT, "Vexlo StudyOS alarms are working on this phone.")
        })
    }
}
