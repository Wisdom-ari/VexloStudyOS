package com.vexlo.studyos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Logic
import java.time.temporal.ChronoUnit

@Composable
fun ExamScreen(d: Data, p: Prefs, vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val today = Logic.today()
    val exam = Logic.parse(p.examDate, today)
    val toExam = ChronoUnit.DAYS.between(today, exam)
    var addPaper by remember { mutableStateOf(false) }
    var addMock by remember { mutableStateOf(false) }
    var addCheck by remember { mutableStateOf<Long?>(null) }
    val weak = Logic.weakTopics(d)
    val weakCourses = Logic.weakCourses(d)
    val name = { id: Long -> d.courses.firstOrNull { it.id == id }?.name ?: "Course" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            VCard {
                Muted("First exam: ${p.examDate}")
                Text(
                    when { toExam > 0 -> "$toExam days to go"; toExam == 0L -> "Exam day - good luck!"; else -> "Exams under way" },
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Purple
                )
                Muted(
                    "Final revision reminders: from 7 days before the first exam you get a daily notification with your weak topics (needs notification permission). " +
                        "Change dates in Settings.",
                    Modifier.padding(top = 6.dp)
                )
            }
        }

        item { H2("Weak topics") }
        if (weak.isEmpty() && weakCourses.isEmpty()) item { Muted("Nothing flagged. Star topics you find hard in Courses, and record mock scores - courses under 50% show up here.") }
        weakCourses.forEach { (c, pct) -> item { Text("Low mock average: ${c.name} ($pct%)", color = Rose) } }
        weak.forEach { (c, t) -> item { Text("${c.shortName}: ${t.title}", color = Amber) } }

        item { H2("Examination timetable") }
        d.papers.forEach { pp ->
            item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name(pp.courseId), fontWeight = FontWeight.SemiBold)
                            val days = ChronoUnit.DAYS.between(today, Logic.parse(pp.date, today))
                            Muted("${pp.date} at ${pp.time}${if (pp.venue.isNotBlank()) " - ${pp.venue}" else ""} (${if (days >= 0) "in $days days" else "past"})")
                        }
                        Text("x", color = Rose, modifier = Modifier.clickable { vm.deletePaper(pp) }.padding(8.dp))
                    }
                }
            }
        }
        item { SoftButton("+ Add exam paper", Modifier.fillMaxWidth()) { addPaper = true } }

        item { H2("Mock examination scores") }
        d.courses.forEach { c ->
            val avg = Logic.mockPct(d, c.id, 100)
            item { Muted("${c.name}: " + (avg?.let { "average ${(it * 100).toInt()}% over ${d.mocks.count { m -> m.courseId == c.id }} mock(s)" } ?: "no mocks yet")) }
        }
        d.mocks.take(8).forEach { m ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${m.date}  ${d.courses.firstOrNull { it.id == m.courseId }?.shortName ?: ""}  ${m.score}/${m.maxScore}", Modifier.weight(1f))
                    Text("x", color = Rose, modifier = Modifier.clickable { vm.deleteMock(m) }.padding(8.dp))
                }
            }
        }
        item { SoftButton("+ Record mock score", Modifier.fillMaxWidth()) { addMock = true } }

        item { H2("Revision checklists") }
        d.courses.forEach { c ->
            val list = d.checklist.filter { it.courseId == c.id }
            item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Muted("${list.count { it.done }}/${list.size}")
                    }
                    list.forEach { it2 ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = it2.done, onCheckedChange = { vm.toggleChecklist(it2) })
                            Text(it2.text, Modifier.weight(1f), textDecoration = if (it2.done) TextDecoration.LineThrough else null, fontSize = 14.sp)
                            Text("x", color = Rose, modifier = Modifier.clickable { vm.deleteChecklist(it2) }.padding(8.dp))
                        }
                    }
                    Text("+ Add item", color = Blue, modifier = Modifier.clickable { addCheck = c.id }.padding(vertical = 8.dp))
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (addPaper) {
        var course by remember { mutableStateOf(d.courses.firstOrNull()?.id) }
        var date by remember { mutableStateOf(p.examDate) }
        var time by remember { mutableStateOf("09:00") }
        var venue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addPaper = false },
            containerColor = Surface1,
            title = { Text("Add exam paper") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField("Course", d.courses.map { it.id to it.name }, course, { course = it })
                    SoftButton("Date: $date") { pickDate(ctx, date) { date = it } }
                    SoftButton("Time: $time") { pickTime(ctx, 9, 0) { h, m -> time = Logic.hhmm(h, m) } }
                    OutlinedTextField(venue, { venue = it }, label = { Text("Venue (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { course?.let { vm.addPaper(it, date, time, venue) }; addPaper = false }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addPaper = false }) { Text("Cancel") } }
        )
    }
    if (addMock) {
        var course by remember { mutableStateOf(d.courses.firstOrNull()?.id) }
        var score by remember { mutableStateOf("") }
        var max by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { addMock = false },
            containerColor = Surface1,
            title = { Text("Mock exam score") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField("Course", d.courses.map { it.id to it.name }, course, { course = it })
                    OutlinedTextField(score, { score = it }, label = { Text("Score") }, singleLine = true)
                    OutlinedTextField(max, { max = it }, label = { Text("Out of") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sc = score.toIntOrNull()
                    val mx = max.toIntOrNull()
                    if (course != null && sc != null && mx != null) vm.addMock(course!!, sc, mx)
                    addMock = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { addMock = false }) { Text("Cancel") } }
        )
    }
    addCheck?.let { cid ->
        FormDialog("New checklist item", listOf("Item"), onSave = { v -> vm.addChecklist(cid, v[0]); addCheck = null }, onDismiss = { addCheck = null })
    }
}
