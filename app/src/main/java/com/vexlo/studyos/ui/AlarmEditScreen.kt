package com.vexlo.studyos.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.data.AlarmEntity
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.PrefKeys
import com.vexlo.studyos.data.Prefs
import android.content.Intent

private val DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
private val HOURS = (1..12).map { it.toString() }
private val MINUTES = (0..59).map { if (it < 10) "0$it" else it.toString() }
private val AMPM = listOf("AM", "PM")

@Composable
fun AlarmEditScreen(d: Data, p: Prefs, alarmId: Long, vm: MainViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val existing = d.alarms.firstOrNull { it.id == alarmId }

    var hour by remember(alarmId) { mutableStateOf(existing?.hour ?: 6) }
    var minute by remember(alarmId) { mutableStateOf(existing?.minute ?: 0) }
    var label by remember(alarmId) { mutableStateOf(existing?.label ?: "New alarm") }
    var mask by remember(alarmId) { mutableStateOf(existing?.daysMask ?: 127) }
    var sound by remember(alarmId) { mutableStateOf(existing?.soundUri) }
    var confirmDelete by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            sound = uri.toString()
        }
    }

    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val pm = hour >= 12

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = label, onValueChange = { label = it }, label = { Text("Alarm name") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            VCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    WheelColumn(HOURS, h12 - 1, { i ->
                        val nh = i + 1
                        hour = (nh % 12) + if (pm) 12 else 0
                    })
                    Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextHi, modifier = Modifier.padding(horizontal = 4.dp))
                    WheelColumn(MINUTES, minute, { i -> minute = i })
                    Spacer(Modifier.width(8.dp))
                    WheelColumn(AMPM, if (pm) 1 else 0, { i ->
                        hour = (hour % 12) + if (i == 1) 12 else 0
                    })
                }
                Muted("Drag a column up or down, or tap the faded numbers.", Modifier.fillMaxWidth().padding(top = 6.dp))
            }

            VCard {
                H2("Repeat")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DAYS.forEachIndexed { i, l ->
                        DayDot(l, ((mask shr i) and 1) == 1) { mask = mask xor (1 shl i) }
                    }
                }
                if (mask == 0) Muted("No days selected - this alarm will never ring.", Modifier.padding(top = 8.dp))
            }

            VCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sound", fontWeight = FontWeight.SemiBold)
                        Muted(if (sound == null) "Default alarm sound" else "Custom audio file")
                    }
                    SoftButton("Choose") { pick.launch(arrayOf("audio/*")) }
                    if (sound != null) {
                        Spacer(Modifier.width(8.dp))
                        SoftButton("Default") { sound = null }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Snooze duration", fontWeight = FontWeight.SemiBold)
                        Muted("Applies to all alarms")
                    }
                    SoftButton("-") { vm.setPref { this[PrefKeys.SNOOZE] = (p.snoozeMin - 1).coerceAtLeast(1) } }
                    Text("${p.snoozeMin} min", Modifier.width(64.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    SoftButton("+") { vm.setPref { this[PrefKeys.SNOOZE] = (p.snoozeMin + 1).coerceAtMost(30) } }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibrate", fontWeight = FontWeight.SemiBold)
                        Muted("Applies to all alarms")
                    }
                    Switch(checked = p.vibrate, onCheckedChange = { v -> vm.setPref { this[PrefKeys.VIBRATE] = v } })
                }
            }

            if (existing != null) {
                Text(
                    "Delete this alarm", color = Rose, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally)
        ) {
            RoundButton(Icons.Filled.Close, Rose, "Cancel", onClose)
            RoundButton(Icons.Filled.Check, Blue, "Save", {
                val base = existing ?: AlarmEntity(hour = hour, minute = minute, label = label, type = "CUSTOM")
                vm.saveAlarm(
                    base.copy(
                        hour = hour, minute = minute,
                        label = label.ifBlank { "Alarm" },
                        daysMask = mask, soundUri = sound, enabled = true
                    )
                )
                onClose()
            })
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Surface1,
            title = { Text("Delete alarm?") },
            text = { Text("\"${existing.label}\" will be removed.") },
            confirmButton = { TextButton(onClick = { vm.deleteAlarm(existing); confirmDelete = false; onClose() }) { Text("Delete", color = Rose) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
