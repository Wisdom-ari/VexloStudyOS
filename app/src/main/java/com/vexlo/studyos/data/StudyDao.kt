package com.vexlo.studyos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // Courses
    @Query("SELECT * FROM courses ORDER BY id") fun courses(): Flow<List<CourseEntity>>
    @Query("SELECT * FROM courses ORDER BY id") suspend fun coursesOnce(): List<CourseEntity>
    @Query("SELECT COUNT(*) FROM courses") suspend fun courseCount(): Int
    @Insert suspend fun insertCourse(c: CourseEntity): Long

    // Topics
    @Query("SELECT * FROM topics ORDER BY id") fun topics(): Flow<List<TopicEntity>>
    @Query("SELECT * FROM topics ORDER BY id") suspend fun topicsOnce(): List<TopicEntity>
    @Insert suspend fun insertTopic(t: TopicEntity): Long
    @Update suspend fun updateTopic(t: TopicEntity)
    @Query("DELETE FROM topics WHERE id = :id OR parentId = :id") suspend fun deleteTopic(id: Long)

    // Notes
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun notes(): Flow<List<NoteEntity>>
    @Insert suspend fun insertNote(n: NoteEntity): Long
    @Update suspend fun updateNote(n: NoteEntity)
    @Delete suspend fun deleteNote(n: NoteEntity)

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY id") fun assignments(): Flow<List<AssignmentEntity>>
    @Insert suspend fun insertAssignment(a: AssignmentEntity): Long
    @Update suspend fun updateAssignment(a: AssignmentEntity)
    @Delete suspend fun deleteAssignment(a: AssignmentEntity)

    // Questions
    @Query("SELECT * FROM questions ORDER BY id") fun questions(): Flow<List<QuestionEntity>>
    @Insert suspend fun insertQuestion(q: QuestionEntity): Long
    @Update suspend fun updateQuestion(q: QuestionEntity)
    @Delete suspend fun deleteQuestion(q: QuestionEntity)

    // Sessions
    @Query("SELECT * FROM sessions ORDER BY startMs DESC") fun sessions(): Flow<List<SessionEntity>>
    @Insert suspend fun insertSession(s: SessionEntity): Long
    @Delete suspend fun deleteSession(s: SessionEntity)

    // Alarms
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id") fun alarms(): Flow<List<AlarmEntity>>
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id") suspend fun alarmsOnce(): List<AlarmEntity>
    @Query("SELECT * FROM alarms WHERE id = :id") suspend fun getAlarm(id: Long): AlarmEntity?
    @Insert suspend fun insertAlarm(a: AlarmEntity): Long
    @Update suspend fun updateAlarm(a: AlarmEntity)
    @Delete suspend fun deleteAlarm(a: AlarmEntity)
    @Query("DELETE FROM alarms") suspend fun deleteAllAlarms()

    // Timetable
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, sortOrder, id") fun timetable(): Flow<List<TimetableEntity>>
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, sortOrder, id") suspend fun timetableOnce(): List<TimetableEntity>
    @Insert suspend fun insertTimetable(t: TimetableEntity): Long
    @Delete suspend fun deleteTimetable(t: TimetableEntity)

    // Mock exams
    @Query("SELECT * FROM mocks ORDER BY id DESC") fun mocks(): Flow<List<MockEntity>>
    @Insert suspend fun insertMock(m: MockEntity): Long
    @Delete suspend fun deleteMock(m: MockEntity)

    // Exam papers
    @Query("SELECT * FROM papers ORDER BY date, time") fun papers(): Flow<List<ExamPaperEntity>>
    @Insert suspend fun insertPaper(p: ExamPaperEntity): Long
    @Delete suspend fun deletePaper(p: ExamPaperEntity)

    // Revision checklist
    @Query("SELECT * FROM checklist ORDER BY id") fun checklist(): Flow<List<ChecklistEntity>>
    @Insert suspend fun insertChecklist(c: ChecklistEntity): Long
    @Update suspend fun updateChecklist(c: ChecklistEntity)
    @Delete suspend fun deleteChecklist(c: ChecklistEntity)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY id") fun tasks(): Flow<List<TaskEntity>>
    @Insert suspend fun insertTask(t: TaskEntity): Long
    @Update suspend fun updateTask(t: TaskEntity)
    @Delete suspend fun deleteTask(t: TaskEntity)
}
