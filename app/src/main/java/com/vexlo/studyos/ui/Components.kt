package com.vexlo.studyos.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun VCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = if (onClick != null) modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick) else modifier
    Card(
        modifier = m.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        elevation = CardDefaults.cardElevation(defaultElevation = if (IsDark) 0.dp else 3.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun H1(text: String) = Text(text, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextHi)

@Composable
fun H2(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextHi, modifier = modifier)

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) = Text(text, fontSize = 13.sp, color = TextLo, modifier = modifier)

@Composable
fun GradButton(text: String, modifier: Modifier = Modifier, height: Int = 52, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Grad)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextHi, fontSize = 14.sp)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Blue else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.White else TextLo, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DayDot(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (selected) Purple else Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else TextLo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = Blue) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(12.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Muted(label)
    }
}

@Composable
fun ProgressBar(frac: Float, modifier: Modifier = Modifier, color: Color = Blue) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Surface2)
    ) {
        Box(
            Modifier
                .fillMaxWidth(frac.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

@Composable
fun BarChart(values: List<Float>, labels: List<String>, valueLabels: List<String>, modifier: Modifier = Modifier) {
    val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(0.1f)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { v ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight((v / maxV).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Brush.verticalGradient(listOf(Purple, Blue)))
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            valueLabels.forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, color = TextHi, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, color = TextLo, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}

@Composable
fun Ring(frac: Float, size: Int, stroke: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val track = Surface2
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.dp.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(Blue, Purple, Blue)),
                startAngle = -90f, sweepAngle = 360f * frac.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun FormDialog(
    title: String,
    labels: List<String>,
    initial: List<String> = emptyList(),
    multiline: Int = -1,
    numeric: Set<Int> = emptySet(),
    confirm: String = "Save",
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val vals = remember {
        mutableStateListOf<String>().apply { labels.indices.forEach { add(initial.getOrElse(it) { "" }) } }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    OutlinedTextField(
                        value = vals[i],
                        onValueChange = { vals[i] = it },
                        label = { Text(l) },
                        singleLine = i != multiline,
                        minLines = if (i == multiline) 4 else 1,
                        keyboardOptions = KeyboardOptions(keyboardType = if (i in numeric) KeyboardType.Number else KeyboardType.Text)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(vals.toList()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DropdownField(label: String, options: List<Pair<Long?, String>>, selected: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) {
            Text("$label: " + (options.firstOrNull { it.first == selected }?.second ?: "-"), maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

fun pickDate(ctx: Context, initial: String, onPick: (String) -> Unit) {
    val d = runCatching { LocalDate.parse(initial) }.getOrDefault(LocalDate.now())
    DatePickerDialog(ctx, { _, y, m, day -> onPick(LocalDate.of(y, m + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

fun pickTime(ctx: Context, h: Int, m: Int, onPick: (Int, Int) -> Unit) {
    TimePickerDialog(ctx, { _, hh, mm -> onPick(hh, mm) }, h, m, false).show()
}
