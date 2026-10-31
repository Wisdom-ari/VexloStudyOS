package com.vexlo.studyos.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.alarm.Notifications
import com.vexlo.studyos.data.AlarmEntity
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.PrefKeys
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Backup
import com.vexlo.studyos.domain.Logic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun SettingsScreen(d: Data, p: Prefs, vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var renaming by remember { mutableStateOf<AlarmEntity?>(null) }
    var soundFor by remember { mutableStateOf<AlarmEntity?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    // Re-check permission state whenever the user comes back from system settings.
    val activity = ctx as? ComponentActivity
    DisposableEffect(activity) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        activity?.lifecycle?.addObserver(obs)
        onDispose { activity?.lifecycle?.removeObserver(obs) }
    }
    val notifOk = remember(refresh) { Notifications.canPost(ctx) }
    val exactOk = remember(refresh) {
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true
    }
    val batteryOk = remember(refresh) {
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val a = soundFor
        if (uri != null && a != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.saveAlarm(a.copy(soundUri = uri.toString()))
        }
        soundFor = null
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.backup(ctx, uri) }.isSuccess }
            toast(ctx, if (ok) "Backup saved" else "Backup failed")
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.writeText(ctx, uri, Logic.reportCsv(d, p)) }.isSuccess }
            toast(ctx, if (ok) "Progress exported" else "Export failed")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.restore(ctx, uri) }.getOrDefault(false) }
            if (ok) {
                toast(ctx, "Restored. The app will close - open it again to finish.")
                Handler(Looper.getMainLooper()).postDelayed({ exitProcess(0) }, 1800)
            } else toast(ctx, "That file is not a Vexlo StudyOS backup")
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { H1("Settings") }

        // ---------- Permissions ----------
        item { H2("Alarm reliability") }
        item {
            VCard {
                StatusRow("Notifications", notifOk)
                StatusRow("Exact alarms (Android 12+)", exactOk)
                StatusRow("Battery optimisation off", batteryOk)
                Spacer(Modifier.height(8.dp))
                Muted(
                    "How to set up: 1) Allow notifications. 2) Allow \"Alarms & reminders\" for Vexlo StudyOS. " +
                        "3) Set battery to Unrestricted / No restrictions. 4) On Tecno (HiOS): Phone Master > Auto-start > allow Vexlo StudyOS, " +
                        "and in Recent apps, pull down / long-press the app card and tap the lock icon so it is not cleaned. " +
                        "Alarms use Android's AlarmManager and are re-armed after reboot."
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Notifications", Modifier.weight(1f)) {
                        open(ctx, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                    }
                    SoftButton("Exact alarms", Modifier.weight(1f)) {
                        if (Build.VERSION.SDK_INT >= 31) open(ctx, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        else toast(ctx, "Not needed on this Android version")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Battery", Modifier.weight(1f)) {
                        open(ctx, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                    }
                    SoftButton("App info", Modifier.weight(1f)) {
                        open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                    }
                }
                Spacer(Modifier.height(8.dp))
                GradButton("Test alarm in 20 seconds", Modifier.fillMaxWidth(), 48) {
                    AlarmScheduler.scheduleTest(ctx, 20)
                    toast(ctx, "Test alarm set - lock the screen and wait 20 seconds")
                }
            }
        }

        // ---------- Alarms ----------
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H2("Alarms", Modifier.weight(1f))
                SoftButton("+ Alarm") { vm.saveAlarm(AlarmEntity(hour = 6, minute = 0, label = "New alarm", type = "CUSTOM", daysMask = 127)) }
            }
        }
        d.alarms.forEach { a ->
            item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Logic.hhmm(a.hour, a.minute), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            color = if (a.enabled) Blue else TextLo,
                            modifier = Modifier.clickable { pickTime(ctx, a.hour, a.minute) { h, m -> vm.saveAlarm(a.copy(hour = h, minute = m)) } }
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f).clickable { renaming = a }) {
                            Text(a.label, fontWeight = FontWeight.SemiBold)
                            Muted(if (a.soundUri == null) "Default alarm sound" else "Custom sound")
                        }
                        Switch(checked = a.enabled, onCheckedChange = { vm.saveAlarm(a.copy(enabled = it)) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DAY_LETTERS.forEachIndexed { i, l ->
                            val on = ((a.daysMask shr i) and 1) == 1
                            DayDot(l, on) { vm.saveAlarm(a.copy(daysMask = a.daysMask xor (1 shl i))) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton("Choose sound") { soundFor = a; soundPicker.launch(arrayOf("audio/*")) }
                        if (a.soundUri != null) SoftButton("Default") { vm.saveAlarm(a.copy(soundUri = null)) }
                        Spacer(Modifier.weight(1f))
                        Text("Delete", color = Rose, modifier = Modifier.clickable { vm.deleteAlarm(a) }.padding(10.dp))
                    }
                }
            }
        }
        item { SoftButton("Reset alarms to the default exam schedule", Modifier.fillMaxWidth()) { confirmReset = true } }

        // ---------- Study settings ----------
        item { H2("Study settings") }
        item {
            VCard {
                SettingStepper("Daily study target (hours)", p.targetHours, 1, 14) { v -> vm.setPref { this[PrefKeys.TARGET] = v } }
                SettingStepper("Snooze (minutes)", p.snoozeMin, 1, 30) { v -> vm.setPref { this[PrefKeys.SNOOZE] = v } }
                SettingStepper("Default focus (minutes)", p.focusMin, 5, 180, 5) { v -> vm.setPref { this[PrefKeys.FOCUS] = v } }
                SettingStepper("Default break (minutes)", p.breakMin, 1, 60) { v -> vm.setPref { this[PrefKeys.BREAK] = v } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Prep starts: ${p.prepStart}", Modifier.weight(1f)) { pickDate(ctx, p.prepStart) { s -> vm.setPref { this[PrefKeys.PREP] = s } } }
                    SoftButton("First exam: ${p.examDate}", Modifier.weight(1f)) { pickDate(ctx, p.examDate) { s -> vm.setPref { this[PrefKeys.EXAM] = s } } }
                }
            }
        }

        // ---------- Data ----------
        item { H2("Data - backup, restore, export") }
        item {
            VCard {
                Muted("Everything is stored on this phone only (no account, works offline). Back up regularly to Downloads or Google Drive.")
                Spacer(Modifier.height(8.dp))
                SoftButton("Back up all data", Modifier.fillMaxWidth()) { backupLauncher.launch("vexlo-studyos-backup-${Logic.today()}.db") }
                Spacer(Modifier.height(8.dp))
                SoftButton("Restore from backup", Modifier.fillMaxWidth()) { restoreLauncher.launch(arrayOf("*/*")) }
                Spacer(Modifier.height(8.dp))
                SoftButton("Export progress report (CSV)", Modifier.fillMaxWidth()) { exportLauncher.launch("vexlo-progress-${Logic.today()}.csv") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Muted("Vexlo StudyOS 1.0 - by Vexlo Digital")
            }
        }
    }

    renaming?.let { a ->
        FormDialog("Alarm label", listOf("Label"), listOf(a.label), onSave = { v -> vm.saveAlarm(a.copy(label = v[0].ifBlank { a.label })); renaming = null }, onDismiss = { renaming = null })
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

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(if (ok) "OK" else "Action needed", color = if (ok) Mint else Amber, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingStepper(label: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        SoftButton("-") { onChange((value - step).coerceAtLeast(min)) }
        Text("$value", Modifier.width(44.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        SoftButton("+") { onChange((value + step).coerceAtMost(max)) }
    }
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

private fun open(ctx: Context, i: Intent) {
    try {
        ctx.startActivity(i)
    } catch (_: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        } catch (_: Exception) {
            toast(ctx, "Open Settings > Apps > Vexlo StudyOS manually")
        }
    }
}
