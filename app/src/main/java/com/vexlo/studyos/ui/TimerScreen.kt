package com.vexlo.studyos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Logic
import java.util.Locale

@Composable
fun TimerScreen(d: Data, p: Prefs, vm: MainViewModel, onBack: () -> Unit) {
    val t by vm.timer.state.collectAsState()
    var course by rememberSaveable { mutableStateOf<Long?>(null) }
    var focus by rememberSaveable(p.focusMin) { mutableStateOf(p.focusMin) }
    var brk by rememberSaveable(p.breakMin) { mutableStateOf(p.breakMin) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val iso = Logic.today().toString()
    val todayMin = d.sessions.filter { it.date == iso }.sumOf { it.durationSec } / 60
    val frac = if (t.totalSec == 0) 0f else 1f - t.remainingSec.toFloat() / t.totalSec
    val mm = String.format(Locale.US, "%02d:%02d", t.remainingSec / 60, t.remainingSec % 60)
    val idleMm = String.format(Locale.US, "%02d:00", focus)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SoftButton("< Back", onClick = onBack)
        H1("Study timer")
        Muted("Studied today: ${Logic.fmt(todayMin)} of ${p.targetHours}h")

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Ring(if (t.phase == "IDLE") 0f else frac, 240, 16) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (t.phase == "IDLE") idleMm else mm, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Muted(
                        when (t.phase) {
                            "FOCUS" -> if (t.running) "Focus" else "Paused"
                            "BREAK" -> if (t.running) "Break" else "Break ready"
                            else -> "Ready"
                        }
                    )
                }
            }
        }
        if (t.phase == "FOCUS") Muted("Focused so far this session: ${Logic.fmt(t.focusSec / 60)} (${t.focusSec % 60}s)")
        if (t.lastSavedMin > 0 && t.phase != "FOCUS") Muted("Last saved session: ${t.lastSavedMin} min - added to your statistics.")

        if (t.phase == "IDLE") {
            DropdownField("Course", listOf<Pair<Long?, String>>(null to "General") + d.courses.map { it.id to it.name }, course, { course = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Pomodoro 25/5", focus == 25 && brk == 5) { focus = 25; brk = 5 }
                Chip("Deep 50/10", focus == 50 && brk == 10) { focus = 50; brk = 10 }
                Chip("Long 90/15", focus == 90 && brk == 15) { focus = 90; brk = 15 }
            }
            Stepper("Focus minutes", focus, 5, 180) { focus = it }
            Stepper("Break minutes", brk, 1, 60) { brk = it }
            GradButton("START FOCUS", Modifier.fillMaxWidth(), 64) { vm.timer.startFocus(course, focus, brk) }
        } else {
            if (t.phase == "FOCUS") {
                if (t.running) GradButton("PAUSE", Modifier.fillMaxWidth(), 60) { vm.timer.pause() }
                else GradButton("RESUME", Modifier.fillMaxWidth(), 60) { vm.timer.resume() }
                SoftButton("Finish and save time", Modifier.fillMaxWidth()) { vm.timer.finish() }
            } else {
                if (!t.running) GradButton("START BREAK", Modifier.fillMaxWidth(), 60) { vm.timer.startBreak() }
                else SoftButton("Pause break", Modifier.fillMaxWidth()) { vm.timer.pause() }
                SoftButton(if (t.running || t.remainingSec == 0) "Skip break" else "Skip break and finish", Modifier.fillMaxWidth()) { vm.timer.skipBreak() }
            }
        }
        Muted("The timer keeps counting with the screen off and rings when a phase ends. Focus time is saved to your statistics automatically.")
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        SoftButton("-") { onChange((value - 5).coerceAtLeast(min)) }
        Text("$value", Modifier.width(56.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        SoftButton("+") { onChange((value + 5).coerceAtMost(max)) }
    }
}
