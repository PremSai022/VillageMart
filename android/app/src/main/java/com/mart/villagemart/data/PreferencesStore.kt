package com.mart.villagemart.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferencesStore @Inject constructor(@ApplicationContext private val context: Context) {
    val recentVillageId: Flow<Long?> = context.settingsDataStore.data.map { preferences ->
        preferences[RECENT_VILLAGE]?.toLongOrNull()
    }

    suspend fun rememberVillage(id: Long) {
        context.settingsDataStore.edit { it[RECENT_VILLAGE] = id.toString() }
    }

    private companion object {
        val RECENT_VILLAGE = stringPreferencesKey("recent_village_id")
    }
}
