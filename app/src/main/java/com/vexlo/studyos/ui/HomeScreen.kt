package com.vexlo.studyos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Logic
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HomeScreen(
    d: Data,
    p: Prefs,
    vm: MainViewModel,
    openTimer: () -> Unit,
    openExam: () -> Unit,
    openAlarms: () -> Unit,
    editAlarm: (Long) -> Unit,
    openCourse: (Long) -> Unit,
    studyCourse: (Long) -> Unit
) {
    val today = Logic.today()
    val iso = today.toString()
    val minToday = d.sessions.filter { it.date == iso }.sumOf { it.durationSec } / 60
    val targetMin = (p.targetHours * 60).coerceAtLeast(1)
    val remaining = (targetMin - minToday).coerceAtLeast(0)
    val pct = (minToday * 100 / targetMin).coerceAtMost(100)
    val streak = Logic.streak(Logic.minutesByDate(d.sessions), today)
    val prep = Logic.parse(p.prepStart, today)
    val toPrep = ChronoUnit.DAYS.between(today, prep)

    // Countdown follows the real exam timetable when papers exist.
    val nextPaper = d.papers
        .filter { Logic.parse(it.date, today) >= today }
        .minWithOrNull(compareBy({ it.date }, { it.time }))
    val examDate = nextPaper?.let { Logic.parse(it.date, today) } ?: Logic.parse(p.examDate, today)
    val toExam = ChronoUnit.DAYS.between(today, examDate)

    val slots = Logic.daySlots(today, d.alarms, d.timetable, d.courses)
    val next = d.alarms.filter { it.enabled }
        .mapNotNull { a -> AlarmScheduler.nextTrigger(a)?.let { a to it } }
        .minByOrNull { it.second.toEpochSecond() }
    val pendingTasks = d.tasks.filter { it.date == iso && !it.done }
    val pendingAssign = d.assignments.filter { !it.done }.sortedBy { it.dueDate ?: "9999" }.take(4)

    var tab by rememberSaveable { mutableStateOf(0) }
    var logSlot by remember { mutableStateOf<Logic.Slot?>(null) }
    var logManual by remember { mutableStateOf(false) }
    var addTask by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        HeaderBanner("Vexlo StudyOS", today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)))
        UnderlineTabs(listOf("Dashboard", "Today"), tab) { tab = it }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (tab == 0) {
                // ------------------------------------------------ Dashboard
                item {
                    VCard(onClick = openExam) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Muted(if (nextPaper != null) "Next examination" else "Examination countdown")
                                Text(
                                    when {
                                        toExam > 0 -> "$toExam days"
                                        toExam == 0L -> "Today!"
                                        else -> "Exams in progress"
                                    },
                                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Blue
                                )
                                if (nextPaper != null) {
                                    val c = d.courses.firstOrNull { it.id == nextPaper.courseId }
                                    Text(c?.name ?: "Paper", fontWeight = FontWeight.SemiBold)
                                    Muted(
                                        examDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)) +
                                            " - " + Logic.timeRange(nextPaper.time)
                                    )
                                } else {
                                    Muted("First exam ${examDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))}")
                                }
                                Muted(
                                    if (toPrep > 0) "Preparation starts in $toPrep day(s)" else "Preparation day ${-toPrep + 1}",
                                    Modifier.padding(top = 2.dp)
                                )
                            }
                            Muted("Exam mode >")
                        }
                    }
                }
                item {
                    VCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Ring(pct / 100f, 96, 10) { Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Muted("Today's progress")
                                Text("${Logic.fmt(minToday)} done", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Muted("${Logic.fmt(remaining)} remaining of ${p.targetHours}h target")
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("Study streak", "$streak day${if (streak == 1) "" else "s"}", Modifier.weight(1f), Amber)
                            StatTile(
                                next?.first?.label ?: "Next alarm",
                                next?.let { Logic.untilText(ChronoUnit.MINUTES.between(ZonedDateTime.now(Logic.zone), it.second)) } ?: "None",
                                Modifier.weight(1f), Mint
                            )
                        }
                    }
                }
                item { GradButton("START STUDY", Modifier.fillMaxWidth(), height = 64, onClick = openTimer) }
                item { H2("Course overview") }
                d.courses.forEach { c ->
                    item { CourseRow(c, d, onOpen = { openCourse(c.id) }, onStudy = { studyCourse(c.id) }) }
                }
            } else {
                // ------------------------------------------------ Today
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        H2("Today's schedule", Modifier.weight(1f))
                        SoftButton("All alarms >") { openAlarms() }
                    }
                }
                if (slots.isEmpty()) item { Muted("No alarms are enabled for today. Open All alarms to add or enable some.") }
                slots.forEach { s ->
                    item {
                        val done = d.sessions.any { it.date == iso && it.alarmId == s.alarm.id }
                        val studyLike = s.alarm.type == "STUDY" || s.alarm.type == "REVISION"
                        val at = today.atTime(s.alarm.hour, s.alarm.minute).atZone(Logic.zone)
                        val nowZ = ZonedDateTime.now(Logic.zone)
                        val inText = if (at.isBefore(nowZ)) "earlier today" else Logic.untilText(ChronoUnit.MINUTES.between(nowZ, at))
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.padding(top = 22.dp).size(10.dp).clip(CircleShape).background(typeColor(s.alarm.type)))
                                Box(Modifier.width(2.dp).weight(1f).background(Border))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                AlarmCard(
                                    a = s.alarm,
                                    inText = inText,
                                    onToggle = { on -> vm.saveAlarm(s.alarm.copy(enabled = on)) },
                                    onClick = { editAlarm(s.alarm.id) },
                                    footer = if (studyLike) {
                                        {
                                            Spacer(Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Muted("${s.title} - ${Logic.fmt(s.plannedMin)}", Modifier.weight(1f))
                                                if (done) Text("Done", color = Mint, fontWeight = FontWeight.Bold)
                                                else SoftButton("Log") { logSlot = s }
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
                item { SoftButton("+ Log study time manually", Modifier.fillMaxWidth()) { logManual = true } }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        H2("Pending tasks", Modifier.weight(1f))
                        SoftButton("+ Task") { addTask = true }
                    }
                }
                if (pendingTasks.isEmpty() && pendingAssign.isEmpty()) item { Muted("Nothing pending. Nice work.") }
                pendingTasks.forEach { t ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { vm.toggleTask(t) })
                            Text(t.title, Modifier.weight(1f))
                            Text("x", color = TextLo, modifier = Modifier.clickable { vm.deleteTask(t) }.padding(10.dp))
                        }
                    }
                }
                pendingAssign.forEach { a ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { vm.toggleAssignment(a) })
                            Column(Modifier.weight(1f)) {
                                Text(a.title)
                                Muted("Assignment - ${d.courses.firstOrNull { it.id == a.courseId }?.shortName ?: ""}${a.dueDate?.let { " - due $it" } ?: ""}")
                            }
                        }
                    }
                }
            }
        }
    }

    logSlot?.let { s ->
        FormDialog(
            title = "Log: ${s.alarm.label}",
            labels = listOf("Minutes actually studied"),
            initial = listOf(s.plannedMin.toString()),
            numeric = setOf(0),
            confirm = "Mark completed",
            onSave = { v -> vm.logSession(s.course?.id, s.alarm.id, v[0].toIntOrNull() ?: 0); logSlot = null },
            onDismiss = { logSlot = null }
        )
    }
    if (logManual) {
        FormDialog(
            title = "Log study time",
            labels = listOf("Minutes studied"),
            numeric = setOf(0),
            onSave = { v -> vm.logSession(null, null, v[0].toIntOrNull() ?: 0); logManual = false },
            onDismiss = { logManual = false }
        )
    }
    if (addTask) {
        FormDialog(
            title = "New task for today",
            labels = listOf("Task"),
            onSave = { v -> vm.addTask(v[0]); addTask = false },
            onDismiss = { addTask = false }
        )
    }
}
