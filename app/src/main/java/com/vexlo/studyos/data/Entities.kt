package com.vexlo.studyos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shortName: String,
    val colorIdx: Int = 0
)

/** parentId == null -> topic, otherwise subtopic of that topic. */
@Entity(tableName = "topics", indices = [Index("courseId")])
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val parentId: Long? = null,
    val title: String,
    val done: Boolean = false,
    val weak: Boolean = false
)

@Entity(tableName = "notes", indices = [Index("courseId")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assignments", indices = [Index("courseId")])
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val dueDate: String? = null,
    val done: Boolean = false
)

/** Practice / examination questions. */
@Entity(tableName = "questions", indices = [Index("courseId")])
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val text: String,
    val attempted: Boolean = false,
    val correct: Boolean = false
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val alarmId: Long? = null,
    val date: String,
    val startMs: Long,
    val durationSec: Int
)

/** type: WAKE, STUDY, BREAK, LUNCH, END, REVISION, CUSTOM. daysMask: bit0 = Monday ... bit6 = Sunday. */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val type: String = "CUSTOM",
    val enabled: Boolean = true,
    val daysMask: Int = 127,
    val soundUri: String? = null
)

/** dayOfWeek: 1 = Monday ... 7 = Sunday. */
@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val courseId: Long? = null,
    val title: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "mocks")
data class MockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val score: Int,
    val maxScore: Int,
    val date: String
)

@Entity(tableName = "papers")
data class ExamPaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val date: String,
    val time: String = "09:00",
    val venue: String = ""
)

@Entity(tableName = "checklist", indices = [Index("courseId")])
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val text: String,
    val done: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val done: Boolean = false
)

/** Snapshot of every table, observed by the UI. */
data class Data(
    val courses: List<CourseEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val alarms: List<AlarmEntity> = emptyList(),
    val timetable: List<TimetableEntity> = emptyList(),
    val mocks: List<MockEntity> = emptyList(),
    val papers: List<ExamPaperEntity> = emptyList(),
    val checklist: List<ChecklistEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList()
)
