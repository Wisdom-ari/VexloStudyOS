package com.vexlo.studyos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.Data
import com.vexlo.studyos.data.Prefs
import com.vexlo.studyos.domain.Logic
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(d: Data, p: Prefs) {
    val today = Logic.today()
    val byDate = Logic.minutesByDate(d.sessions)
    val totalMin = byDate.values.sum()
    val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
    val weekMin: Int = (0..6).map { byDate[weekStart.plusDays(it.toLong()).toString()] ?: 0 }.sum()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dayMin = days.map { byDate[it.toString()] ?: 0 }
    val weeks = (5 downTo 0).map { weekStart.minusWeeks(it.toLong()) }
    val weekTotals: List<Int> = weeks.map { ws -> (0..6).map { byDate[ws.plusDays(it.toLong()).toString()] ?: 0 }.sum() }
    val topicsDone = d.topics.count { it.done }
    val assignDone = d.assignments.count { it.done }
    val attempted = d.questions.count { it.attempted }
    val mockAvg = if (d.mocks.isEmpty()) null else (d.mocks.map { it.score * 100f / it.maxScore.coerceAtLeast(1) }.average()).toInt()
    val readiness = Logic.overallReadiness(d)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            VCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(readiness, 110, 12) { Text("${(readiness * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        H2("Examination readiness")
                        Muted("Blend of topics done, revision checklist, recent mock scores and question accuracy across all courses.")
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total studied", Logic.fmt(totalMin), Modifier.weight(1f))
                    StatTile("This week", Logic.fmt(weekMin), Modifier.weight(1f), Purple)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Topics done / left", "$topicsDone / ${d.topics.size - topicsDone}", Modifier.weight(1f), Mint)
                    StatTile("Assignments done", "$assignDone / ${d.assignments.size}", Modifier.weight(1f), Amber)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Questions attempted", "$attempted", Modifier.weight(1f), Cyan)
                    StatTile("Mock average", mockAvg?.let { "$it%" } ?: "-", Modifier.weight(1f), Rose)
                }
            }
        }
        item {
            VCard {
                H2("Daily study hours (last 7 days)")
                Spacer(Modifier.height(10.dp))
                BarChart(
                    values = dayMin.map { it / 60f },
                    labels = days.map { it.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) },
                    valueLabels = dayMin.map { if (it == 0) "-" else String.format(Locale.US, "%.1f", it / 60f) }
                )
                Muted("Target: ${p.targetHours}h per day", Modifier.padding(top = 6.dp))
            }
        }
        item {
            VCard {
                H2("Weekly study performance")
                Spacer(Modifier.height(10.dp))
                BarChart(
                    values = weekTotals.map { it / 60f },
                    labels = weeks.map { it.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) },
                    valueLabels = weekTotals.map { if (it == 0) "-" else "${it / 60}h" }
                )
                Muted("Weekly target: ${p.targetHours * 6}h (six study days)", Modifier.padding(top = 6.dp))
            }
        }
        item {
            VCard {
                H2("Course completion and hours")
                Spacer(Modifier.height(10.dp))
                d.courses.forEach { c ->
                    val (done, total) = Logic.topicCounts(d, c.id)
                    val col = CourseColors[c.colorIdx % CourseColors.size]
                    Row {
                        Text(c.shortName, fontWeight = FontWeight.SemiBold, color = col, modifier = Modifier.width(48.dp))
                        Muted("$done/$total topics - ${Logic.fmt(Logic.courseMinutes(d, c.id))} - readiness ${(Logic.readiness(d, c.id) * 100).toInt()}%")
                    }
                    Spacer(Modifier.height(4.dp))
                    ProgressBar(if (total == 0) 0f else done.toFloat() / total, color = col)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        item {
            VCard {
                H2("Achievements")
                Spacer(Modifier.height(8.dp))
                Logic.achievements(d, today).forEach { (name, ok) ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(if (ok) "Done" else "Locked", color = if (ok) Mint else TextLo, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                        Text(name, color = if (ok) TextHi else TextLo, fontSize = 14.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
