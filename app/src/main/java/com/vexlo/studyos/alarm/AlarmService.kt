package com.vexlo.studyos.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.vexlo.studyos.MainActivity
import com.vexlo.studyos.R
import com.vexlo.studyos.data.AlarmEntity
import com.vexlo.studyos.data.AppDatabase
import com.vexlo.studyos.data.prefsFlow
import com.vexlo.studyos.domain.Logic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Foreground service that plays the alarm sound, vibrates and shows Snooze / Dismiss actions. */
class AlarmService : Service() {
    companion object {
        const val ACTION_DISMISS = "com.vexlo.studyos.DISMISS"
        const val ACTION_SNOOZE = "com.vexlo.studyos.SNOOZE"
        private const val NOTIF_ID = 4242
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var wake: PowerManager.WakeLock? = null
    private var stopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                shutdown()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
                scope.launch {
                    val mins = withContext(Dispatchers.IO) { applicationContext.prefsFlow().first().snoozeMin }
                    if (id > 0) AlarmScheduler.snooze(applicationContext, id, mins)
                    shutdown()
                }
                return START_NOT_STICKY
            }
        }
        if (intent != null) handleFire(intent) else shutdown()
        return START_NOT_STICKY
    }

    private fun handleFire(intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        stopRinging()
        goForeground(build("Vexlo StudyOS", "Alarm", id, kind == "alarm" || kind == "snooze", 10))

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vexlo:alarm").apply { acquire(2 * 60 * 1000L) }

        scope.launch {
            var title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Vexlo StudyOS"
            var text = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT) ?: ""
            var sound: String? = null
            var alarm: AlarmEntity? = null
            var snoozeMin = 10
            var vibrate = true
            withContext(Dispatchers.IO) {
                val pr = applicationContext.prefsFlow().first()
                snoozeMin = pr.snoozeMin
                vibrate = pr.vibrate
                if (id > 0) {
                    val dao = AppDatabase.get(applicationContext).dao()
                    alarm = dao.getAlarm(id)
                    alarm?.let { a ->
                        title = a.label
                        sound = a.soundUri
                        text = describe(a, dao.alarmsOnce(), dao.timetableOnce(), dao.coursesOnce())
                    }
                }
            }
            val n = build(title, text, id, kind == "alarm" || kind == "snooze", snoozeMin)
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, n)

            // "Session begins" notice that stays in the shade after the alarm is dismissed.
            if (alarm?.type == "STUDY" || alarm?.type == "REVISION") {
                Notifications.reminder(applicationContext, 5000 + id.toInt(), "Session started: $title", text)
            }

            val short = kind == "timer" || kind == "test"
            startRinging(sound, short, vibrate)
            stopJob?.cancel()
            stopJob = launch {
                delay(if (short) 20_000L else 90_000L)
                shutdown()
            }
        }
    }

    private fun describe(a: AlarmEntity, alarms: List<AlarmEntity>, tt: List<com.vexlo.studyos.data.TimetableEntity>, courses: List<com.vexlo.studyos.data.CourseEntity>): String {
        val slot = Logic.daySlots(Logic.today(), alarms, tt, courses).firstOrNull { it.alarm.id == a.id }
        return when (a.type) {
            "WAKE" -> "Good morning! Time to wake up - your study day starts at 5:30."
            "STUDY" -> "Study session begins now: ${slot?.title ?: a.label}. Plan: ${Logic.fmt(slot?.plannedMin ?: 120)} of focused work."
            "BREAK" -> "Break time. Step away from the screen, stretch and drink water."
            "LUNCH" -> "Lunch break. Eat well and rest before the afternoon session."
            "END" -> "Focused study is over. Open the app and log today's hours."
            "REVISION" -> "Optional light revision: ${slot?.title ?: "review your notes"}."
            else -> a.label
        }
    }

    private fun build(title: String, text: String, id: Long, canSnooze: Boolean, snoozeMin: Int): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        val dismiss = PendingIntent.getService(
            this, 1, Intent(this, AlarmService::class.java).setAction(ACTION_DISMISS), flags
        )
        val b = NotificationCompat.Builder(this, Notifications.CH_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Dismiss", dismiss)
        if (canSnooze && id > 0) {
            val snooze = PendingIntent.getService(
                this, 2,
                Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_ID, id),
                flags
            )
            b.addAction(0, "Snooze $snoozeMin min", snooze)
        }
        return b.build()
    }

    private fun goForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun startRinging(sound: String?, short: Boolean, vibrate: Boolean) {
        stopRinging()
        val custom = sound?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val ok = custom != null && play(custom)
        if (!ok) {
            val def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (def != null) play(def)
        }
        val vib = if (vibrate) getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator else null
        vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), if (short) -1 else 0))
    }

    private fun play(uri: Uri): Boolean = try {
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, uri)
            isLooping = true
            prepare()
            start()
        }
        true
    } catch (_: Exception) {
        player?.release()
        player = null
        false
    }

    private fun stopRinging() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
    }

    private fun shutdown() {
        stopJob?.cancel()
        stopRinging()
        wake?.let { if (it.isHeld) it.release() }
        wake = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopRinging()
        wake?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }
}
