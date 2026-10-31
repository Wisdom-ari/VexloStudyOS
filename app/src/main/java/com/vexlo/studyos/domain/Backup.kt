package com.vexlo.studyos.domain

import android.content.Context
import android.net.Uri
import com.vexlo.studyos.data.AppDatabase
import java.io.File

object Backup {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    /** Copies the whole local database (all progress, courses, alarms, settings tables) to [out]. */
    fun backup(ctx: Context, out: Uri) {
        val db = AppDatabase.get(ctx)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        val src = ctx.getDatabasePath(AppDatabase.FILE_NAME)
        ctx.contentResolver.openOutputStream(out, "w")!!.use { o -> src.inputStream().use { it.copyTo(o) } }
    }

    /** Replaces the local database with the backup. Caller must restart the app afterwards. */
    fun restore(ctx: Context, input: Uri): Boolean {
        val tmp = File(ctx.cacheDir, "restore.tmp")
        ctx.contentResolver.openInputStream(input)!!.use { i -> tmp.outputStream().use { i.copyTo(it) } }
        val head = ByteArray(SQLITE_HEADER.size)
        val ok = tmp.inputStream().use { it.read(head) == head.size } && head.contentEquals(SQLITE_HEADER)
        if (!ok) {
            tmp.delete()
            return false
        }
        AppDatabase.closeInstance()
        val dst = ctx.getDatabasePath(AppDatabase.FILE_NAME)
        File(dst.path + "-wal").delete()
        File(dst.path + "-shm").delete()
        File(dst.path + "-journal").delete()
        tmp.copyTo(dst, overwrite = true)
        tmp.delete()
        return true
    }

    fun writeText(ctx: Context, out: Uri, text: String) {
        ctx.contentResolver.openOutputStream(out, "w")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }
}
