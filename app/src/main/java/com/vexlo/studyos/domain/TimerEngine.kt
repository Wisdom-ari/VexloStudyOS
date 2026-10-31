package com.vexlo.studyos.domain

import android.content.Context
import com.vexlo.studyos.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerUi(
    val phase: String = "IDLE", // IDLE | FOCUS | BREAK
    val running: Boolean = false,
    val remainingSec: Int = 0,
    val totalSec: Int = 0,
    val focusSec: Int = 0,
    val courseId: Long? = null,
    val lastSavedMin: Int = 0
)

/**
 * Pomodoro / custom timer based on wall-clock timestamps (so it stays correct with the screen off)
 * and persisted in SharedPreferences (so it survives the app being killed). An AlarmManager alarm
 * rings when a phase ends. Finished focus time is reported through [onRecord] and becomes a session.
 */
class TimerEngine(
    private val ctx: Context,
    scope: CoroutineScope,
    private val onRecord: (courseId: Long?, seconds: Int) -> Unit
) {
    private val sp = ctx.getSharedPreferences("timer", Context.MODE_PRIVATE)
    private var phase = sp.getString("phase", "IDLE") ?: "IDLE"
    private var running = sp.getBoolean("running", false)
    private var endAt = sp.getLong("endAt", 0L)
    private var pausedMs = sp.getLong("pausedMs", 0L)
    private var totalMs = sp.getLong("totalMs", 0L)
    private var breakMs = sp.getLong("breakMs", 5 * 60_000L)
    private var courseId = sp.getLong("courseId", -1L)
    private var accumMs = sp.getLong("accumMs", 0L)
    private var segStart = sp.getLong("segStart", 0L)
    private var lastSavedMin = 0

    private val _state = MutableStateFlow(TimerUi())
    val state: StateFlow<TimerUi> = _state.asStateFlow()

    init {
        tick()
        scope.launch {
            while (true) {
                delay(500)
                tick()
            }
        }
    }

    private fun save() {
        sp.edit()
            .putString("phase", phase).putBoolean("running", running).putLong("endAt", endAt)
            .putLong("pausedMs", pausedMs).putLong("totalMs", totalMs).putLong("breakMs", breakMs)
            .putLong("courseId", courseId).putLong("accumMs", accumMs).putLong("segStart", segStart)
            .apply()
    }

    private fun armAlarm() {
        val text = if (phase == "FOCUS") "Focus session complete - take a break." else "Break over - back to study."
        AlarmScheduler.scheduleTimer(ctx, endAt, "Vexlo timer", text)
    }

    fun startFocus(course: Long?, focusMin: Int, breakMin: Int) {
        val now = System.currentTimeMillis()
        phase = "FOCUS"
        courseId = course ?: -1L
        totalMs = focusMin * 60_000L
        breakMs = breakMin * 60_000L
        accumMs = 0L
        segStart = now
        endAt = now + totalMs
        running = true
        armAlarm()
        save()
        tick()
    }

    fun startBreak() {
        val now = System.currentTimeMillis()
        phase = "BREAK"
        totalMs = breakMs
        endAt = now + breakMs
        running = true
        armAlarm()
        save()
        tick()
    }

    fun pause() {
        if (!running) return
        val now = System.currentTimeMillis()
        if (phase == "FOCUS") accumMs += (now - segStart)
        pausedMs = (endAt - now).coerceAtLeast(0L)
        running = false
        AlarmScheduler.cancelTimer(ctx)
        save()
        tick()
    }

    fun resume() {
        if (running || phase == "IDLE") return
        val now = System.currentTimeMillis()
        segStart = now
        endAt = now + pausedMs
        running = true
        armAlarm()
        save()
        tick()
    }

    /** End early: save the focus time actually studied (if at least one minute). */
    fun finish() {
        val now = System.currentTimeMillis()
        if (phase == "FOCUS") {
            val ms = accumMs + if (running) now - segStart else 0L
            record(ms)
        }
        reset()
    }

    fun skipBreak() = reset()

    private fun reset() {
        phase = "IDLE"
        running = false
        accumMs = 0L
        pausedMs = 0L
        AlarmScheduler.cancelTimer(ctx)
        save()
        tick()
    }

    private fun record(ms: Long) {
        if (ms >= 60_000L) {
            lastSavedMin = (ms / 60_000L).toInt()
            onRecord(if (courseId >= 0) courseId else null, (ms / 1000L).toInt())
        }
    }

    private fun complete() {
        if (phase == "FOCUS") {
            record(accumMs + (endAt - segStart))
            phase = "BREAK"
            running = false
            totalMs = breakMs
            pausedMs = breakMs
            accumMs = 0L
        } else {
            phase = "IDLE"
            running = false
            pausedMs = 0L
        }
        save()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        if (running && now >= endAt) complete()
        val remaining = when {
            phase == "IDLE" -> 0L
            running -> (endAt - now).coerceAtLeast(0L)
            else -> pausedMs
        }
        val focus = if (phase == "FOCUS") accumMs + (if (running) now - segStart else 0L) else 0L
        _state.value = TimerUi(
            phase = phase,
            running = running,
            remainingSec = (remaining / 1000L).toInt(),
            totalSec = (totalMs / 1000L).toInt(),
            focusSec = (focus / 1000L).toInt(),
            courseId = if (courseId >= 0) courseId else null,
            lastSavedMin = lastSavedMin
        )
    }
}
