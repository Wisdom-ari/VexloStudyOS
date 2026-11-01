package com.vexlo.studyos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun AppRoot(vm: MainViewModel) {
    val d by vm.data.collectAsState()
    val p by vm.prefs.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var sub by rememberSaveable { mutableStateOf("") }
    var courseId by rememberSaveable { mutableStateOf(-1L) }
    var editId by rememberSaveable { mutableStateOf(0L) }
    var timerCourse by rememberSaveable { mutableStateOf(-1L) }

    val goBack: () -> Unit = { sub = if (sub == "alarmEdit") "alarms" else "" }
    BackHandler(enabled = sub.isNotEmpty()) { goBack() }

    val tabs = listOf(
        Tab("Home", Icons.Filled.Home),
        Tab("Timetable", Icons.Filled.DateRange),
        Tab("Courses", Icons.AutoMirrored.Filled.List),
        Tab("Progress", Icons.Filled.CheckCircle),
        Tab("Settings", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = Navy,
        bottomBar = {
            Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
            NavigationBar(containerColor = Surface1) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; sub = "" },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            indicatorColor = Blue.copy(alpha = 0.14f),
                            selectedTextColor = Blue,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo
                        )
                    )
                }
            }
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // Header: Home draws its own big clock header; every other screen gets a banner.
            when {
                sub == "timer" -> HeaderBanner("Study timer", "Focus, break, repeat", goBack)
                sub == "exam" -> HeaderBanner("Examination mode", "Countdown, mocks, checklists", goBack)
                sub == "course" -> HeaderBanner(d.courses.firstOrNull { it.id == courseId }?.name ?: "Course", null, goBack)
                sub == "alarms" -> HeaderBanner("My alarms", "Study schedule", goBack)
                sub == "alarmEdit" -> HeaderBanner(if (editId == 0L) "New alarm" else "Edit alarm", null, goBack)
                tab == 1 -> HeaderBanner("Timetable", "Study plan and exam papers")
                tab == 2 -> HeaderBanner("Course overview", "Semester 1")
                tab == 3 -> HeaderBanner("Progress", "Calculated from your real activity")
                tab == 4 -> HeaderBanner("Settings", "Theme, alarms, data")
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    sub == "timer" -> TimerScreen(d, p, vm, timerCourse) { sub = "" }
                    sub == "exam" -> ExamScreen(d, p, vm) { sub = "" }
                    sub == "course" -> CourseDetail(d, courseId, vm) { sub = "" }
                    sub == "alarms" -> AlarmsScreen(d, vm) { id -> editId = id; sub = "alarmEdit" }
                    sub == "alarmEdit" -> AlarmEditScreen(d, p, editId, vm) { sub = "alarms" }
                    tab == 0 -> HomeScreen(
                        d, p, vm,
                        openTimer = { timerCourse = -1L; sub = "timer" },
                        openExam = { sub = "exam" },
                        openAlarms = { sub = "alarms" },
                        editAlarm = { id -> editId = id; sub = "alarmEdit" },
                        openCourse = { id -> courseId = id; sub = "course" },
                        studyCourse = { id -> timerCourse = id; sub = "timer" }
                    )
                    tab == 1 -> TimetableScreen(d, vm)
                    tab == 2 -> CoursesScreen(d, onOpen = { id -> courseId = id; sub = "course" }, onStudy = { id -> timerCourse = id; sub = "timer" })
                    tab == 3 -> ProgressScreen(d, p)
                    else -> SettingsScreen(d, p, vm) { sub = "alarms" }
                }
            }
        }
    }
}
