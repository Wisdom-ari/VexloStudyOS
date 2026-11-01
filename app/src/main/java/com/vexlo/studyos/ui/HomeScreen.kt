package com.vexlo.studyos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Logic
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun LazyListScope.pad(content: @Composable () -> Unit) {
    item { Box(Modifier.padding(horizontal = 16.dp)) { content() } }
}

@Composable
private fun HomeHeader(pct: Int, doneText: String, remainingText: String) {
    var now by remember { mutableStateOf(ZonedDateTime.now(Logic.zone)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(Logic.zone)
            delay(1000L)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(HeaderBrush)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Vexlo StudyOS", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(10.dp))
            ClockDial(progress = pct / 100f, dialSize = 230) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(now.format(DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)), fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(now.format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH)), fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(now.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Kabale, Uganda", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("$doneText done  -  $remainingText left  -  $pct%", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun HomeScreen(
    d: Data,
    p: Prefs,
    vm: MainViewModel,
    openTimer: () -> Unit,
    openExam: () -> Unit,
    openAlarms: () -> Unit,
    editAlarm: (Long) -> Unit
) {
    val today = Logic.today()
    val iso = today.toString()
    val minToday = d.sessions.filter { it.date == iso }.sumOf { it.durationSec } / 60
    val targetMin = (p.targetHours * 60).coerceAtLeast(1)
    val remaining = (targetMin - minToday).coerceAtLeast(0)
    val pct = (minToday * 100 / targetMin).coerceAtMost(100)
    val streak = Logic.streak(Logic.minutesByDate(d.sessions), today)
    val exam = Logic.parse(p.examDate, today)
    val prep = Logic.parse(p.prepStart, today)
    val toExam = ChronoUnit.DAYS.between(today, exam)
    val toPrep = ChronoUnit.DAYS.between(today, prep)
    val slots = Logic.daySlots(today, d.alarms, d.timetable, d.courses)
    val next = d.alarms.filter { it.enabled }
        .mapNotNull { a -> AlarmScheduler.nextTrigger(a)?.let { a to it } }
        .minByOrNull { it.second.toEpochSecond() }
    val pendingTasks = d.tasks.filter { it.date == iso && !it.done }
    val pendingAssign = d.assignments.filter { !it.done }.sortedBy { it.dueDate ?: "9999" }.take(4)

    var logSlot by remember { mutableStateOf<Logic.Slot?>(null) }
    var logManual by remember { mutableStateOf(false) }
    var addTask by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { HomeHeader(pct, Logic.fmt(minToday), Logic.fmt(remaining)) }

        pad {
            VCard(onClick = openExam) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted("Examination countdown")
                        Text(
                            when {
                                toExam > 0 -> "$toExam days"
                                toExam == 0L -> "Exam day!"
                                else -> "Exams in progress"
                            },
                            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Blue
                        )
                        Muted(
                            if (toPrep > 0) "Preparation starts in $toPrep day(s)"
                            else "Preparation day ${-toPrep + 1} - first exam ${exam.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))}"
                        )
                    }
                    Muted("Exam mode >")
                }
            }
        }
        pad {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Study streak", "$streak day${if (streak == 1) "" else "s"}", Modifier.weight(1f), Amber)
                StatTile(
                    next?.first?.label ?: "Next alarm",
                    next?.let { Logic.untilText(ChronoUnit.MINUTES.between(ZonedDateTime.now(Logic.zone), it.second)) } ?: "None",
                    Modifier.weight(1f), Mint
                )
            }
        }
        pad { GradButton("START STUDY", Modifier.fillMaxWidth(), height = 68, onClick = openTimer) }

        pad {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H2("Today's schedule", Modifier.weight(1f))
                SoftButton("All alarms >") { openAlarms() }
            }
        }
        if (slots.isEmpty()) pad { Muted("No alarms are enabled for today. Open All alarms to add or enable some.") }
        slots.forEach { s ->
            pad {
                val done = d.sessions.any { it.date == iso && it.alarmId == s.alarm.id }
                val studyLike = s.alarm.type == "STUDY" || s.alarm.type == "REVISION"
                val at = today.atTime(s.alarm.hour, s.alarm.minute).atZone(Logic.zone)
                val nowZ = ZonedDateTime.now(Logic.zone)
                val inText = if (at.isBefore(nowZ)) "earlier today" else Logic.untilText(ChronoUnit.MINUTES.between(nowZ, at))
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.padding(top = 22.dp).size(10.dp).clip(CircleShape).background(typeColor(s.alarm.type)))
                        Box(Modifier.width(2.dp).weight(1f).background(Surface2))
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
        pad { SoftButton("+ Log study time manually", Modifier.fillMaxWidth()) { logManual = true } }

        pad {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H2("Pending tasks", Modifier.weight(1f))
                SoftButton("+ Task") { addTask = true }
            }
        }
        if (pendingTasks.isEmpty() && pendingAssign.isEmpty()) pad { Muted("Nothing pending. Nice work.") }
        pendingTasks.forEach { t ->
            pad {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = false, onCheckedChange = { vm.toggleTask(t) })
                    Text(t.title, Modifier.weight(1f))
                    Text("x", color = TextLo, modifier = Modifier.clickable { vm.deleteTask(t) }.padding(10.dp))
                }
            }
        }
        pendingAssign.forEach { a ->
            pad {
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
