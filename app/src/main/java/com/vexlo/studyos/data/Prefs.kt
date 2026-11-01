package com.vexlo.studyos.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Prefs(
    val targetHours: Int = 8,
    val examDate: String = "2026-11-28",
    val prepStart: String = "2026-11-02",
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    val snoozeMin: Int = 10,
    val lastReminder: String = "",
    val themeMode: Int = 0,
    val vibrate: Boolean = true,
    val papersSeeded: Boolean = false
)

object PrefKeys {
    val TARGET = intPreferencesKey("target_hours")
    val EXAM = stringPreferencesKey("exam_date")
    val PREP = stringPreferencesKey("prep_start")
    val FOCUS = intPreferencesKey("focus_min")
    val BREAK = intPreferencesKey("break_min")
    val SNOOZE = intPreferencesKey("snooze_min")
    val LAST_REMINDER = stringPreferencesKey("last_reminder")
    val THEME = intPreferencesKey("theme_mode")
    val VIBRATE = booleanPreferencesKey("vibrate")
    val PAPERS_SEEDED = booleanPreferencesKey("papers_seeded")
}

fun Context.prefsFlow(): Flow<Prefs> = dataStore.data.map { p ->
    Prefs(
        targetHours = p[PrefKeys.TARGET] ?: 8,
        examDate = p[PrefKeys.EXAM] ?: "2026-11-28",
        prepStart = p[PrefKeys.PREP] ?: "2026-11-02",
        focusMin = p[PrefKeys.FOCUS] ?: 25,
        breakMin = p[PrefKeys.BREAK] ?: 5,
        snoozeMin = p[PrefKeys.SNOOZE] ?: 10,
        lastReminder = p[PrefKeys.LAST_REMINDER] ?: "",
        themeMode = p[PrefKeys.THEME] ?: 0,
        vibrate = p[PrefKeys.VIBRATE] ?: true,
        papersSeeded = p[PrefKeys.PAPERS_SEEDED] ?: false
    )
}

suspend fun Context.updatePrefs(block: MutablePreferences.() -> Unit) {
    dataStore.edit { it.block() }
}
