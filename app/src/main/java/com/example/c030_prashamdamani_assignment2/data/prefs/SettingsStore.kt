package com.example.c030_prashamdamani_assignment2.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.c030_prashamdamani_assignment2.util.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "todo_settings")

/** Local-only preferences (DataStore). */
class SettingsStore(private val context: Context) {
    private val reduceKey = booleanPreferencesKey("reduce_motion")
    private val sortKey = stringPreferencesKey("default_sort")

    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { it[reduceKey] ?: false }

    val defaultSort: Flow<SortMode> = context.dataStore.data.map { p ->
        SortMode.values().firstOrNull { it.name == p[sortKey] } ?: SortMode.DEFAULT
    }

    suspend fun setReduceMotion(value: Boolean) {
        context.dataStore.edit { it[reduceKey] = value }
    }

    suspend fun setDefaultSort(mode: SortMode) {
        context.dataStore.edit { it[sortKey] = mode.name }
    }
}
