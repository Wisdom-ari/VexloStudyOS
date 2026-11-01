package com.vexlo.studyos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.domain.Logic

@Composable
fun AlarmsScreen(d: Data, vm: MainViewModel, onEdit: (Long) -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    val enabled = d.alarms.count { it.enabled }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Muted("$enabled of ${d.alarms.size} alarms on. Tap an alarm to edit its time, days and sound.") }
            if (d.alarms.isEmpty()) item { Muted("No alarms yet. Tap + to add one.") }
            d.alarms.forEach { a ->
                item {
                    AlarmCard(
                        a = a,
                        inText = Logic.alarmIn(a),
                        onToggle = { on -> vm.saveAlarm(a.copy(enabled = on)) },
                        onClick = { onEdit(a.id) }
                    )
                }
            }
            item { SoftButton("Reset to the default exam schedule", Modifier.fillMaxWidth()) { confirmReset = true } }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(Blue)
                .clickable { onEdit(0L) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add alarm", tint = Color.White)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Surface1,
            title = { Text("Reset alarms?") },
            text = { Text("This replaces all alarms with the default schedule (5:00 wake-up ... 7:00 PM light revision). Your progress is not affected.") },
            confirmButton = { TextButton(onClick = { vm.resetAlarms(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}
