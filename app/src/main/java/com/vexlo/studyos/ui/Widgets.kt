package com.vexlo.studyos.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.AlarmEntity
import kotlin.math.cos
import kotlin.math.sin

private val DAYS2 = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

fun typeColor(type: String): Color = when (type) {
    "WAKE" -> Amber
    "STUDY" -> Blue
    "BREAK" -> Mint
    "LUNCH" -> Cyan
    "END" -> Rose
    else -> Purple
}

/** KabVile-style top bar: blue strip behind the status bar, then a white app bar with the title. */
@Composable
fun HeaderBanner(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(Surface1)) {
        Box(Modifier.fillMaxWidth().background(Strip).statusBarsPadding()) { Spacer(Modifier.height(0.dp)) }
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Blue)
                }
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextHi, maxLines = 1)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextLo, maxLines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
    }
}

/** Clock face with sun-ray ticks and a progress arc. */
@Composable
fun ClockDial(progress: Float, dialSize: Int, content: @Composable () -> Unit) {
    Box(Modifier.size(dialSize.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension / 2f
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val len = if (major) 12.dp.toPx() else 6.dp.toPx()
                val ang = Math.toRadians(i * 6.0 - 90.0)
                val cs = cos(ang).toFloat()
                val sn = sin(ang).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = if (major) 0.9f else 0.4f),
                    start = Offset(c.x + (r - len) * cs, c.y + (r - len) * sn),
                    end = Offset(c.x + r * cs, c.y + r * sn),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                )
            }
            val w = 6.dp.toPx()
            val inset = 24.dp.toPx() + w / 2f
            val arcSize = Size(this.size.width - 2f * inset, this.size.height - 2f * inset)
            drawArc(Color.White.copy(alpha = 0.22f), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(Color.White, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Alarm row: big time, AM/PM, type dot + label, repeat days, switch and "in 1 h 20 min". */
@Composable
fun AlarmCard(
    a: AlarmEntity,
    inText: String,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    val fade = if (a.enabled) 1f else 0.5f
    VCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Logic12.hm(a), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextHi.copy(alpha = fade))
                    Spacer(Modifier.width(4.dp))
                    Text(Logic12.ap(a), fontSize = 12.sp, color = TextLo, modifier = Modifier.padding(bottom = 5.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .padding(bottom = 9.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(typeColor(a.type).copy(alpha = fade))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        a.label, fontSize = 11.sp, color = TextLo, maxLines = 1,
                        modifier = Modifier.padding(bottom = 6.dp).weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    DAYS2.forEachIndexed { i, l ->
                        val on = ((a.daysMask shr i) and 1) == 1
                        Text(
                            l, fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) TextHi.copy(alpha = fade) else TextLo.copy(alpha = 0.5f),
                            textDecoration = if (on) TextDecoration.Underline else null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = a.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Color.White)
                )
                Text(inText, fontSize = 11.sp, color = TextLo)
            }
        }
        footer?.invoke(this)
    }
}

/** Tiny helpers so AlarmCard does not depend on the domain package directly. */
object Logic12 {
    fun hm(a: AlarmEntity): String = com.vexlo.studyos.domain.Logic.hm12(a.hour, a.minute)
    fun ap(a: AlarmEntity): String = com.vexlo.studyos.domain.Logic.ampm(a.hour)
}

@Composable
fun RoundButton(icon: ImageVector, color: Color, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

/** Scroll-wheel style picker: drag up/down or tap the faded neighbours. Wraps around. */
@Composable
fun WheelColumn(labels: List<String>, index: Int, onIndex: (Int) -> Unit, modifier: Modifier = Modifier) {
    val n = labels.size
    val curIndex by rememberUpdatedState(index)
    val callback by rememberUpdatedState(onIndex)
    val stepPx = with(LocalDensity.current) { 38.dp.toPx() }
    var acc by remember { mutableStateOf(0f) }
    fun wrap(i: Int): Int = ((i % n) + n) % n

    Column(
        modifier.pointerInput(n) {
            detectVerticalDragGestures(
                onDragEnd = { acc = 0f },
                onDragCancel = { acc = 0f }
            ) { change, dragAmount ->
                change.consume()
                acc += dragAmount
                val steps = (acc / stepPx).toInt()
                if (steps != 0) {
                    callback(wrap(curIndex - steps))
                    acc -= steps * stepPx
                }
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            labels[wrap(index - 1)], fontSize = 20.sp, color = TextLo.copy(alpha = 0.5f), textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp).clickable { onIndex(wrap(index - 1)) }.padding(vertical = 6.dp)
        )
        Text(
            labels[wrap(index)], fontSize = 42.sp, fontWeight = FontWeight.Bold, color = TextHi, textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp).padding(vertical = 2.dp)
        )
        Text(
            labels[wrap(index + 1)], fontSize = 20.sp, color = TextLo.copy(alpha = 0.5f), textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp).clickable { onIndex(wrap(index + 1)) }.padding(vertical = 6.dp)
        )
    }
}
