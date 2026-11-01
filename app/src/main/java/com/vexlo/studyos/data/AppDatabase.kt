package com.vexlo.studyos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CourseEntity::class, TopicEntity::class, NoteEntity::class, AssignmentEntity::class,
        QuestionEntity::class, SessionEntity::class, AlarmEntity::class, TimetableEntity::class,
        MockEntity::class, ExamPaperEntity::class, ChecklistEntity::class, TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao

    companion object {
        const val FILE_NAME = "studyos.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, FILE_NAME
                )
                    // Single-file journal mode keeps backup / restore a plain file copy.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                    .also { instance = it }
            }

        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
