package com.vexlo.studyos.domain

import com.vexlo.studyos.data.AlarmEntity
import com.vexlo.studyos.data.CourseEntity
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.data.SessionEntity
import com.vexlo.studyos.data.TimetableEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/** All progress numbers are derived from real records (sessions, topics, mocks ...). */
object Logic {
    val zone: ZoneId = ZoneId.of("Africa/Kampala")

    fun today(): LocalDate = LocalDate.now(zone)
    fun parse(s: String, fallback: LocalDate): LocalDate = runCatching { LocalDate.parse(s) }.getOrDefault(fallback)
    fun isoOf(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toString()
    fun fmt(min: Int): String = if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"
    fun hhmm(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)

    fun minutesByDate(s: List<SessionEntity>): Map<String, Int> =
        s.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.durationSec } / 60 }

    /** Consecutive days (ending today, or yesterday if nothing logged yet today) with >= minMin minutes. */
    fun streak(m: Map<String, Int>, today: LocalDate, minMin: Int = 60): Int {
        var d = if ((m[today.toString()] ?: 0) >= minMin) today else today.minusDays(1)
        var n = 0
        while ((m[d.toString()] ?: 0) >= minMin) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    data class Slot(val alarm: AlarmEntity, val course: CourseEntity?, val title: String, val plannedMin: Int)

    /** The enabled alarms of [date] with the course each study slot is assigned from the weekly timetable. */
    fun daySlots(date: LocalDate, alarms: List<AlarmEntity>, timetable: List<TimetableEntity>, courses: List<CourseEntity>): List<Slot> {
        val dow = date.dayOfWeek.value
        val day = alarms.filter { it.enabled && ((it.daysMask shr (dow - 1)) and 1) == 1 }
            .sortedBy { it.hour * 60 + it.minute }
        val items = timetable.filter { it.dayOfWeek == dow }.sortedBy { it.sortOrder }
        val study = day.filter { it.type == "STUDY" }
        return day.mapIndexed { idx, a ->
            var course: CourseEntity? = null
            var title = a.label
            var planned = 0
            when (a.type) {
                "STUDY" -> {
                    val i = study.indexOf(a)
                    if (items.isNotEmpty()) {
                        val item = items[minOf(items.size - 1, i * items.size / study.size)]
                        course = courses.firstOrNull { c -> c.id == item.courseId }
                        title = course?.name ?: item.title
                    }
                    val nextMin = day.getOrNull(idx + 1)?.let { it.hour * 60 + it.minute }
                    val start = a.hour * 60 + a.minute
                    planned = if (nextMin != null) (nextMin - start).coerceIn(15, 240) else 120
                }
                "REVISION" -> {
                    title = if (dow == 7 && items.isNotEmpty()) items.first().title else "Light revision"
                    planned = 60
                }
            }
            Slot(a, course, title, planned)
        }
    }

    fun topicCounts(d: Data, courseId: Long): Pair<Int, Int> {
        val t = d.topics.filter { it.courseId == courseId }
        return t.count { it.done } to t.size
    }

    fun courseMinutes(d: Data, courseId: Long): Int = d.sessions.filter { it.courseId == courseId }.sumOf { it.durationSec } / 60

    fun mockPct(d: Data, courseId: Long, last: Int = 3): Float? {
        val m = d.mocks.filter { it.courseId == courseId }.sortedByDescending { it.id }.take(last)
        if (m.isEmpty()) return null
        return m.map { it.score.toFloat() / it.maxScore.coerceAtLeast(1) }.average().toFloat()
    }

    /** Weighted blend of topics (40%), checklist (20%), recent mock scores (30%) and question accuracy (10%); only components with data count. */
    fun readiness(d: Data, courseId: Long): Float {
        var w = 0f
        var s = 0f
        val t = d.topics.filter { it.courseId == courseId }
        if (t.isNotEmpty()) { w += 0.4f; s += 0.4f * t.count { it.done } / t.size }
        val c = d.checklist.filter { it.courseId == courseId }
        if (c.isNotEmpty()) { w += 0.2f; s += 0.2f * c.count { it.done } / c.size }
        mockPct(d, courseId)?.let { w += 0.3f; s += 0.3f * it }
        val q = d.questions.filter { it.courseId == courseId && it.attempted }
        if (q.isNotEmpty()) { w += 0.1f; s += 0.1f * q.count { it.correct } / q.size }
        return if (w == 0f) 0f else s / w
    }

    fun overallReadiness(d: Data): Float =
        if (d.courses.isEmpty()) 0f else d.courses.map { readiness(d, it.id) }.average().toFloat()

    fun weakTopics(d: Data) = d.topics.filter { it.weak && !it.done }.mapNotNull { t ->
        d.courses.firstOrNull { it.id == t.courseId }?.let { it to t }
    }

    /** Courses whose recent mock average is under 50%. */
    fun weakCourses(d: Data): List<Pair<CourseEntity, Int>> = d.courses.mapNotNull { c ->
        mockPct(d, c.id)?.let { if (it < 0.5f) c to (it * 100).toInt() else null }
    }

    fun achievements(d: Data, today: LocalDate): List<Pair<String, Boolean>> {
        val byDate = minutesByDate(d.sessions)
        val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
        val weekMin: Int = (0..6).map { byDate[weekStart.plusDays(it.toLong()).toString()] ?: 0 }.sum()
        return listOf(
            "First study hour logged" to (byDate.values.sum() >= 60),
            "Hit the daily target at least once" to (byDate.values.any { it >= 480 }),
            "3-day streak" to (streak(byDate, today) >= 3),
            "7-day streak" to (streak(byDate, today) >= 7),
            "20 hours studied this week" to (weekMin >= 1200),
            "10 topics completed" to (d.topics.count { it.done } >= 10),
            "First mock exam recorded" to d.mocks.isNotEmpty(),
            "A mock score of 70% or more" to d.mocks.any { it.score * 100 >= it.maxScore * 70 },
            "All assignments done" to (d.assignments.isNotEmpty() && d.assignments.all { it.done }),
            "50 practice questions attempted" to (d.questions.count { it.attempted } >= 50)
        )
    }

    /** Plain CSV report for "Export progress". */
    fun reportCsv(d: Data, p: Prefs): String {
        val today = today()
        val sb = StringBuilder()
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val byDate = minutesByDate(d.sessions)
        sb.appendLine("Vexlo StudyOS progress report")
        sb.appendLine("Generated,$today")
        sb.appendLine("Days to first exam,${ChronoUnit.DAYS.between(today, parse(p.examDate, today))}")
        sb.appendLine("Total study hours,${"%.1f".format(Locale.US, byDate.values.sum() / 60f)}")
        sb.appendLine("Current streak (days),${streak(byDate, today)}")
        sb.appendLine("Overall readiness %,${(overallReadiness(d) * 100).toInt()}")
        sb.appendLine()
        sb.appendLine("Course,Hours,Topics done,Topics total,Assignments done,Assignments total,Questions attempted,Mock average %,Readiness %")
        d.courses.forEach { c ->
            val (done, total) = topicCounts(d, c.id)
            val a = d.assignments.filter { it.courseId == c.id }
            sb.appendLine(
                listOf(
                    esc(c.name), "%.1f".format(Locale.US, courseMinutes(d, c.id) / 60f), done, total,
                    a.count { it.done }, a.size, d.questions.count { it.courseId == c.id && it.attempted },
                    mockPct(d, c.id, 100)?.let { (it * 100).toInt() } ?: "", (readiness(d, c.id) * 100).toInt()
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("Mock exams")
        sb.appendLine("Date,Course,Score,Max")
        d.mocks.sortedBy { it.date }.forEach { m ->
            sb.appendLine("${m.date},${esc(d.courses.firstOrNull { it.id == m.courseId }?.name ?: "")},${m.score},${m.maxScore}")
        }
        sb.appendLine()
        sb.appendLine("Study sessions")
        sb.appendLine("Date,Course,Minutes")
        d.sessions.sortedBy { it.startMs }.forEach { s ->
            sb.appendLine("${s.date},${esc(d.courses.firstOrNull { it.id == s.courseId }?.name ?: "General")},${s.durationSec / 60}")
        }
        return sb.toString()
    }
}
