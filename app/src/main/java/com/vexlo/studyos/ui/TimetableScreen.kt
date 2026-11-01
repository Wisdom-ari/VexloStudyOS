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
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.domain.Logic

private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val DAY_SHORT = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun TimetableScreen(d: Data, vm: MainViewModel) {
    var dayIdx by rememberSaveable { mutableStateOf(Logic.today().dayOfWeek.value.dec()) }
    val day: Int = dayIdx.inc()
    var adding by remember { mutableStateOf(false) }
    val items = d.timetable.filter { it.dayOfWeek == day }.sortedBy { it.sortOrder }
    val date = Logic.today().plusDays((day - Logic.today().dayOfWeek.value).toLong())
    val slots = Logic.daySlots(date, d.alarms, d.timetable, d.courses)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { H1("Weekly timetable") }
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
