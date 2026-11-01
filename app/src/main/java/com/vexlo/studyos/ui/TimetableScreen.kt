package com.vexlo.studyos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.domain.Logic

private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val DAY_SHORT = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun TimetableScreen(d: Data, vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        UnderlineTabs(listOf("Exam papers", "Study plan"), tab) { tab = it }
        Box(Modifier.weight(1f)) {
            if (tab == 0) ExamPapersList(d) else StudyPlan(d, vm)
        }
    }
}

@Composable
private fun ExamPapersList(d: Data) {
    val today = Logic.today()
    val papers = d.papers.sortedWith(compareBy({ it.date }, { it.time }))
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { H2("FOCLIS examinations, Nov - Dec 2026") }
        item { Muted("Draft timetable. Check the faculty notice board for changes. Times are East Africa Time.") }
        if (papers.isEmpty()) item { Muted("No papers yet. Add them from Home > Exam mode.") }
        papers.forEach { pp ->
            item {
                val c = d.courses.firstOrNull { it.id == pp.courseId }
                val date = Logic.parse(pp.date, today)
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfMonth.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue)
                            Text(
                                date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                                fontSize = 12.sp, color = TextLo
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c?.name ?: "Paper", fontWeight = FontWeight.SemiBold)
                            Muted(
                                date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) +
                                    " - " + Logic.timeRange(pp.time)
                            )
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val code = courseCode(c?.shortName ?: "")
                                if (code.isNotEmpty()) Pill(code, Blue.copy(alpha = 0.12f), Blue)
                                if (pp.venue.isNotBlank()) Pill(pp.venue, Amber.copy(alpha = 0.2f), TextHi)
                                Pill(
                                    when { days > 0 -> "in $days d"; days == 0L -> "today"; else -> "done" },
                                    Mint.copy(alpha = 0.16f), Mint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyPlan(d: Data, vm: MainViewModel) {
    var dayIdx by rememberSaveable { mutableStateOf(Logic.today().dayOfWeek.value.dec()) }
    val day: Int = dayIdx.inc()
    var adding by remember { mutableStateOf(false) }
    val items = d.timetable.filter { it.dayOfWeek == day }.sortedBy { it.sortOrder }
    val date = Logic.today().plusDays((day - Logic.today().dayOfWeek.value).toLong())
    val slots = Logic.daySlots(date, d.alarms, d.timetable, d.courses)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DAY_SHORT.forEachIndexed { i, s -> DayDot(s, dayIdx == i) { dayIdx = i } }
            }
        }
        item { H2(DAY_NAMES[dayIdx]) }
        item { Muted("Subjects for this day. The day's study sessions are shared out between them in order.") }
        if (items.isEmpty()) item { Muted("Nothing planned. Add a subject or activity below.") }
        items.forEach { t ->
            item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val c = d.courses.firstOrNull { it.id == t.courseId }
                        Text(c?.name ?: t.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("Remove", color = Rose, modifier = Modifier.clickable { vm.deleteTimetable(t) }.padding(8.dp))
                    }
                }
            }
        }
        item { SoftButton("+ Add subject / activity", Modifier.fillMaxWidth()) { adding = true } }
        item { H2("Sessions that day") }
        if (slots.isEmpty()) item { Muted("No alarms enabled on this day.") }
        slots.forEach { s ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Logic.hhmm(s.alarm.hour, s.alarm.minute), color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                    Column {
                        Text(s.alarm.label)
                        if (s.alarm.type == "STUDY" || s.alarm.type == "REVISION") Muted(s.title)
                    }
                }
            }
        }
        item { Muted("Edit alarm times and days in Settings > Alarms.") }
    }

    if (adding) {
        var course by remember { mutableStateOf<Long?>(null) }
        var custom by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = Surface1,
            title = { Text("Add to ${DAY_NAMES[dayIdx]}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField("Course", listOf<Pair<Long?, String>>(null to "Custom activity") + d.courses.map { it.id to it.name }, course, { course = it })
                    if (course == null) OutlinedTextField(custom, { custom = it }, label = { Text("Activity name") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = d.courses.firstOrNull { it.id == course }?.name ?: custom
                    if (title.isNotBlank()) vm.addTimetable(day, course, title)
                    adding = false
                    course = null
                    custom = ""
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }
        )
    }
}
