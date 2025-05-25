package com.myworldtech.meet.data.preferences

// UserPreferences.kt
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.myworldtech.meet.data.preferences.UserPrefsKeys.IS_LOGGED_IN
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

val Context.dataStore by preferencesDataStore("user_prefs")

object UserPrefsKeys {
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val NAME = stringPreferencesKey("name")
    val PHOTO_URL = stringPreferencesKey("photo_url")
    val PEER_ID = stringPreferencesKey("peer_id")
    val EMAIL = stringPreferencesKey("email")
}

suspend fun saveUserLocally(
    context: Context,
    name: String,
    photoUrl: String,
    peerId: String,
    email: String
) {
    context.dataStore.edit { prefs ->
        prefs[UserPrefsKeys.IS_LOGGED_IN] = true
        prefs[UserPrefsKeys.NAME] = name
        prefs[UserPrefsKeys.PHOTO_URL] = photoUrl
        prefs[UserPrefsKeys.PEER_ID] = peerId
        prefs[UserPrefsKeys.EMAIL] = email
    }
    Log.d("auth", "User saved locally: $name, $photoUrl, $peerId, $email")
}
suspend fun isUserLoggedIn(context: Context): Boolean {
    return context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) }
        .first()[IS_LOGGED_IN] == true
}
suspend fun getUserInfo(context: Context): Pair<String?, String?> {
    val prefs = context.dataStore.data.first()
    val name = prefs[UserPrefsKeys.NAME]
    val photoUrl = prefs[UserPrefsKeys.PHOTO_URL]
    return Pair(name, photoUrl)
}
suspend fun getPeerId(context: Context): String? {
    val prefs = context.dataStore.data.first()
    val peerId = prefs[UserPrefsKeys.PEER_ID]
    return peerId
}

