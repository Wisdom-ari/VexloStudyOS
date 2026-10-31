package com.vexlo.studyos.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun AppRoot(vm: MainViewModel) {
    val d by vm.data.collectAsState()
    val p by vm.prefs.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var sub by rememberSaveable { mutableStateOf("") }
    var courseId by rememberSaveable { mutableStateOf(-1L) }

    BackHandler(enabled = sub.isNotEmpty()) { sub = "" }

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
            NavigationBar(containerColor = Surface1) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; sub = "" },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Navy,
                            indicatorColor = Blue,
                            selectedTextColor = Blue,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo
                        )
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).statusBarsPadding().fillMaxSize()) {
            when {
                sub == "timer" -> TimerScreen(d, p, vm) { sub = "" }
                sub == "exam" -> ExamScreen(d, p, vm) { sub = "" }
                sub == "course" -> CourseDetail(d, courseId, vm) { sub = "" }
                tab == 0 -> HomeScreen(d, p, vm, openTimer = { sub = "timer" }, openExam = { sub = "exam" })
                tab == 1 -> TimetableScreen(d, vm)
                tab == 2 -> CoursesScreen(d) { id -> courseId = id; sub = "course" }
                tab == 3 -> ProgressScreen(d, p)
                else -> SettingsScreen(d, p, vm)
            }
        }
    }
}
