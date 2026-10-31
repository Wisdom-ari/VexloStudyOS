package com.vexlo.studyos.ui

import android.app.Application
import androidx.datastore.preferences.core.MutablePreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vexlo.studyos.alarm.AlarmScheduler
import com.vexlo.studyos.data.*
import com.vexlo.studyos.domain.Logic
import com.vexlo.studyos.domain.TimerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    @Suppress("UNCHECKED_CAST")
    val data: StateFlow<Data> = combine(
        listOf<Flow<List<Any>>>(
            dao.courses(), dao.topics(), dao.notes(), dao.assignments(), dao.questions(), dao.sessions(),
            dao.alarms(), dao.timetable(), dao.mocks(), dao.papers(), dao.checklist(), dao.tasks()
        )
    ) { a ->
        Data(
            courses = a[0] as List<CourseEntity>,
            topics = a[1] as List<TopicEntity>,
            notes = a[2] as List<NoteEntity>,
            assignments = a[3] as List<AssignmentEntity>,
            questions = a[4] as List<QuestionEntity>,
            sessions = a[5] as List<SessionEntity>,
            alarms = a[6] as List<AlarmEntity>,
            timetable = a[7] as List<TimetableEntity>,
            mocks = a[8] as List<MockEntity>,
            papers = a[9] as List<ExamPaperEntity>,
            checklist = a[10] as List<ChecklistEntity>,
            tasks = a[11] as List<TaskEntity>
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Data())

    val prefs: StateFlow<Prefs> = app.prefsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    val timer = TimerEngine(app, viewModelScope) { courseId, seconds -> recordSeconds(courseId, null, seconds) }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    fun setPref(block: MutablePreferences.() -> Unit) = io { app.updatePrefs(block) }

    // ---- sessions ----
    private fun recordSeconds(courseId: Long?, alarmId: Long?, seconds: Int) = io {
        val now = System.currentTimeMillis()
        dao.insertSession(
            SessionEntity(courseId = courseId, alarmId = alarmId, date = Logic.isoOf(now), startMs = now - seconds * 1000L, durationSec = seconds)
        )
    }

    fun logSession(courseId: Long?, alarmId: Long?, minutes: Int) {
        if (minutes > 0) recordSeconds(courseId, alarmId, minutes * 60)
    }

    fun deleteSession(s: SessionEntity) = io { dao.deleteSession(s) }

    // ---- topics ----
    fun addTopic(courseId: Long, parentId: Long?, title: String) = io { if (title.isNotBlank()) dao.insertTopic(TopicEntity(courseId = courseId, parentId = parentId, title = title.trim())) }
    fun toggleTopic(t: TopicEntity) = io { dao.updateTopic(t.copy(done = !t.done)) }
    fun toggleWeak(t: TopicEntity) = io { dao.updateTopic(t.copy(weak = !t.weak)) }
    fun deleteTopic(t: TopicEntity) = io { dao.deleteTopic(t.id) }

    // ---- notes ----
    fun saveNote(courseId: Long, existing: NoteEntity?, title: String, body: String) = io {
        if (title.isBlank() && body.isBlank()) return@io
        if (existing == null) dao.insertNote(NoteEntity(courseId = courseId, title = title.ifBlank { "Untitled" }, body = body))
        else dao.updateNote(existing.copy(title = title.ifBlank { "Untitled" }, body = body, updatedAt = System.currentTimeMillis()))
    }
    fun deleteNote(n: NoteEntity) = io { dao.deleteNote(n) }

    // ---- assignments ----
    fun addAssignment(courseId: Long, title: String, due: String?) = io {
        if (title.isNotBlank()) dao.insertAssignment(AssignmentEntity(courseId = courseId, title = title.trim(), dueDate = due?.takeIf { s -> runCatching { java.time.LocalDate.parse(s) }.isSuccess }))
    }
    fun toggleAssignment(a: AssignmentEntity) = io { dao.updateAssignment(a.copy(done = !a.done)) }
    fun deleteAssignment(a: AssignmentEntity) = io { dao.deleteAssignment(a) }

    // ---- questions ----
    fun addQuestion(courseId: Long, text: String) = io { if (text.isNotBlank()) dao.insertQuestion(QuestionEntity(courseId = courseId, text = text.trim())) }
    fun setQuestion(q: QuestionEntity, attempted: Boolean, correct: Boolean) = io { dao.updateQuestion(q.copy(attempted = attempted, correct = attempted && correct)) }
    fun deleteQuestion(q: QuestionEntity) = io { dao.deleteQuestion(q) }

    // ---- timetable ----
    fun addTimetable(day: Int, courseId: Long?, title: String) = io {
        val order = (dao.timetableOnce().filter { it.dayOfWeek == day }.maxOfOrNull { it.sortOrder } ?: 0) + 1
        dao.insertTimetable(TimetableEntity(dayOfWeek = day, courseId = courseId, title = title.trim(), sortOrder = order))
    }
    fun deleteTimetable(t: TimetableEntity) = io { dao.deleteTimetable(t) }

    // ---- alarms ----
    fun saveAlarm(a: AlarmEntity) = io {
        val id = if (a.id == 0L) dao.insertAlarm(a) else { dao.updateAlarm(a); a.id }
        AlarmScheduler.schedule(app, a.copy(id = id))
    }
    fun deleteAlarm(a: AlarmEntity) = io {
        AlarmScheduler.cancel(app, a.id)
        dao.deleteAlarm(a)
    }
    fun resetAlarms() = io {
        dao.alarmsOnce().forEach { AlarmScheduler.cancel(app, it.id) }
        dao.deleteAllAlarms()
        seedAlarms(dao)
        AlarmScheduler.scheduleAll(app, dao.alarmsOnce())
    }
    fun rescheduleAll() = io { AlarmScheduler.scheduleAll(app, dao.alarmsOnce()) }

    // ---- mocks / papers / checklist / tasks ----
    fun addMock(courseId: Long, score: Int, max: Int) = io { if (max > 0) dao.insertMock(MockEntity(courseId = courseId, score = score.coerceIn(0, max), maxScore = max, date = Logic.today().toString())) }
    fun deleteMock(m: MockEntity) = io { dao.deleteMock(m) }
    fun addPaper(courseId: Long, date: String, time: String, venue: String) = io { dao.insertPaper(ExamPaperEntity(courseId = courseId, date = date, time = time, venue = venue)) }
    fun deletePaper(p: ExamPaperEntity) = io { dao.deletePaper(p) }
    fun addChecklist(courseId: Long, text: String) = io { if (text.isNotBlank()) dao.insertChecklist(ChecklistEntity(courseId = courseId, text = text.trim())) }
    fun toggleChecklist(c: ChecklistEntity) = io { dao.updateChecklist(c.copy(done = !c.done)) }
    fun deleteChecklist(c: ChecklistEntity) = io { dao.deleteChecklist(c) }
    fun addTask(title: String) = io { if (title.isNotBlank()) dao.insertTask(TaskEntity(title = title.trim(), date = Logic.today().toString())) }
    fun toggleTask(t: TaskEntity) = io { dao.updateTask(t.copy(done = !t.done)) }
    fun deleteTask(t: TaskEntity) = io { dao.deleteTask(t) }
}
