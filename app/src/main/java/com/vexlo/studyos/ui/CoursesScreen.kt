package com.vexlo.studyos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlo.studyos.data.*
import com.vexlo.studyos.domain.Logic

/** One course row: patterned thumbnail, name, code + readiness pills, progress and a menu. */
@Composable
fun CourseRow(c: CourseEntity, d: Data, onOpen: () -> Unit, onStudy: () -> Unit) {
    val col = CourseColors[c.colorIdx % CourseColors.size]
    var menu by remember { mutableStateOf(false) }
    val (done, total) = Logic.topicCounts(d, c.id)
    val ready = (Logic.readiness(d, c.id) * 100).toInt()
    VCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CourseThumb(col, c.id.toInt())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextHi)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val code = courseCode(c.shortName)
                    Pill(if (code.isEmpty()) "SEM 1" else "$code - SEM 1", Color(0xFFFFE8D9), Color(0xFF8A4B1F))
                    Pill("$ready% ready", Mint.copy(alpha = 0.16f), Mint)
                }
            }
            Box {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Course menu", tint = TextHi)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Open course") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Study this course") }, onClick = { menu = false; onStudy() })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(if (total == 0) 0f else done.toFloat() / total, color = col)
        Spacer(Modifier.height(6.dp))
        Muted("$done / $total topics - ${Logic.fmt(Logic.courseMinutes(d, c.id))} studied")
    }
}

@Composable
fun CoursesScreen(d: Data, onOpen: (Long) -> Unit, onStudy: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var byReady by rememberSaveable { mutableStateOf(false) }
    val list = d.courses
        .filter { it.name.contains(query.trim(), ignoreCase = true) }
        .let { l -> if (byReady) l.sortedBy { Logic.readiness(d, it.id) } else l.sortedBy { it.name } }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Filter my courses") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("A - Z", !byReady) { byReady = false }
                Chip("Least ready first", byReady) { byReady = true }
            }
        }
        if (list.isEmpty()) item { Muted("No course matches \"$query\".") }
        list.forEach { c -> item { CourseRow(c, d, onOpen = { onOpen(c.id) }, onStudy = { onStudy(c.id) }) } }
    }
}

@Composable
fun CourseDetail(d: Data, courseId: Long, vm: MainViewModel, onBack: () -> Unit) {
    val course = d.courses.firstOrNull { it.id == courseId }
    if (course == null) {
        Column(Modifier.padding(16.dp)) { SoftButton("< Back", onClick = onBack) }
        return
    }
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Topics", "Notes", "Tasks", "Questions")
    val (done, total) = Logic.topicCounts(d, courseId)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            ProgressBar(if (total == 0) 0f else done.toFloat() / total, color = CourseColors[course.colorIdx % CourseColors.size])
            Muted("$done of $total topics complete", Modifier.padding(top = 4.dp))
        }
        UnderlineTabs(tabs, tab) { tab = it }
        when (tab) {
            0 -> TopicsTab(d, courseId, vm)
            1 -> NotesTab(d, courseId, vm)
            2 -> AssignmentsTab(d, courseId, vm)
            else -> QuestionsTab(d, courseId, vm)
        }
    }
}

@Composable
private fun TopicsTab(d: Data, courseId: Long, vm: MainViewModel) {
    var addFor by remember { mutableStateOf<Long?>(null) } // 0 = new topic, else parent id
    val all = d.topics.filter { it.courseId == courseId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { SoftButton("+ Add topic", Modifier.fillMaxWidth()) { addFor = 0L }; Spacer(Modifier.height(8.dp)) }
        item { Muted("Star a topic to mark it as weak - weak topics appear in Exam mode."); Spacer(Modifier.height(6.dp)) }
        all.filter { it.parentId == null }.forEach { t ->
            item { TopicRow(t, 0, vm) { addFor = t.id } }
            all.filter { it.parentId == t.id }.forEach { sub -> item { TopicRow(sub, 28, vm, null) } }
        }
    }
    addFor?.let { parent ->
        FormDialog(
            title = if (parent == 0L) "New topic" else "New subtopic",
            labels = listOf("Title"),
            onSave = { v -> vm.addTopic(courseId, if (parent == 0L) null else parent, v[0]); addFor = null },
            onDismiss = { addFor = null }
        )
    }
}

@Composable
private fun TopicRow(t: TopicEntity, indent: Int, vm: MainViewModel, onAddSub: (() -> Unit)?) {
    Row(Modifier.padding(start = indent.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = t.done, onCheckedChange = { vm.toggleTopic(t) })
        Text(
            t.title, Modifier.weight(1f),
            textDecoration = if (t.done) TextDecoration.LineThrough else null,
            color = if (t.done) TextLo else TextHi
        )
        Text(if (t.weak) "*" else "o", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.toggleWeak(t) }.padding(8.dp))
        if (onAddSub != null) Text("+", color = Blue, fontSize = 18.sp, modifier = Modifier.clickable { onAddSub() }.padding(8.dp))
        Text("x", color = Rose, modifier = Modifier.clickable { vm.deleteTopic(t) }.padding(8.dp))
    }
}

@Composable
private fun NotesTab(d: Data, courseId: Long, vm: MainViewModel) {
    var editing by remember { mutableStateOf<NoteEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    val notes = d.notes.filter { it.courseId == courseId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SoftButton("+ New note", Modifier.fillMaxWidth()) { creating = true } }
        if (notes.isEmpty()) item { Muted("No notes yet.") }
        notes.forEach { n ->
            item {
                VCard(onClick = { editing = n }) {
                    Text(n.title, fontWeight = FontWeight.SemiBold)
                    if (n.body.isNotBlank()) Muted(n.body.take(140), Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    if (creating) {
        FormDialog("New note", listOf("Title", "Note"), multiline = 1, onSave = { v -> vm.saveNote(courseId, null, v[0], v[1]); creating = false }, onDismiss = { creating = false })
    }
    editing?.let { n ->
        NoteEditDialog(
            note = n,
            onSave = { t, b -> vm.saveNote(courseId, n, t, b); editing = null },
            onDelete = { vm.deleteNote(n); editing = null },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun NoteEditDialog(note: NoteEntity, onSave: (String, String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var t by remember { mutableStateOf(note.title) }
    var b by remember { mutableStateOf(note.body) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Edit note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(t, { t = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Note") }, minLines = 5)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(t, b) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = Rose) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun AssignmentsTab(d: Data, courseId: Long, vm: MainViewModel) {
    var adding by remember { mutableStateOf(false) }
    val list = d.assignments.filter { it.courseId == courseId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { SoftButton("+ Add assignment", Modifier.fillMaxWidth()) { adding = true }; Spacer(Modifier.height(8.dp)) }
        if (list.isEmpty()) item { Muted("No assignments yet.") }
        list.forEach { a ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = a.done, onCheckedChange = { vm.toggleAssignment(a) })
                    Column(Modifier.weight(1f)) {
                        Text(a.title, textDecoration = if (a.done) TextDecoration.LineThrough else null)
                        a.dueDate?.let { Muted("Due $it") }
                    }
                    Text("x", color = Rose, modifier = Modifier.clickable { vm.deleteAssignment(a) }.padding(8.dp))
                }
            }
        }
    }
    if (adding) {
        FormDialog("New assignment", listOf("Title", "Due date (yyyy-mm-dd, optional)"), onSave = { v -> vm.addAssignment(courseId, v[0], v[1].ifBlank { null }); adding = false }, onDismiss = { adding = false })
    }
}

@Composable
private fun QuestionsTab(d: Data, courseId: Long, vm: MainViewModel) {
    var adding by remember { mutableStateOf(false) }
    val list = d.questions.filter { it.courseId == courseId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SoftButton("+ Add exam / practice question", Modifier.fillMaxWidth()) { adding = true }
            Muted("Tick Attempted after you answer it, and Correct if you got it right.", Modifier.padding(top = 6.dp))
        }
        list.forEach { q ->
            item {
                VCard {
                    Text(q.text)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = q.attempted, onCheckedChange = { vm.setQuestion(q, it, q.correct) })
                        Text("Attempted", fontSize = 13.sp)
                        Checkbox(checked = q.correct, enabled = q.attempted, onCheckedChange = { vm.setQuestion(q, true, it) })
                        Text("Correct", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("x", color = Rose, modifier = Modifier.clickable { vm.deleteQuestion(q) }.padding(8.dp))
                    }
                }
            }
        }
    }
    if (adding) {
        FormDialog("New question", listOf("Question"), multiline = 0, onSave = { v -> vm.addQuestion(courseId, v[0]); adding = false }, onDismiss = { adding = false })
    }
}
