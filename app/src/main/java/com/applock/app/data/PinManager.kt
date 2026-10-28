package com.applock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "applock_prefs")

class PinManager(private val context: Context) {

    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val LOCKED_APPS_KEY = stringSetPreferencesKey("locked_apps")
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun savePin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[PIN_HASH_KEY] = hashPin(pin)
        }
    }

    suspend fun hasPin(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[PIN_HASH_KEY] != null
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val stored = prefs[PIN_HASH_KEY] ?: return false
        return stored == hashPin(pin)
    }

    suspend fun addLockedApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[LOCKED_APPS_KEY] ?: emptySet()
            prefs[LOCKED_APPS_KEY] = current + packageName
        }
    }

    suspend fun removeLockedApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[LOCKED_APPS_KEY] ?: emptySet()
            prefs[LOCKED_APPS_KEY] = current - packageName
        }
    }

    fun getLockedApps(): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            prefs[LOCKED_APPS_KEY] ?: emptySet()
        }
    }

    suspend fun isAppLocked(packageName: String): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[LOCKED_APPS_KEY]?.contains(packageName) == true
    }
}
