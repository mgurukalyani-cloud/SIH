package com.itantra.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.TransportType
import java.util.UUID

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("itantra_prefs", Context.MODE_PRIVATE)

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: generateAndSaveDeviceId()
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var nodeCallsign: String
        get() = prefs.getString(KEY_CALLSIGN, "Node Alpha") ?: "Node Alpha"
        set(value) = prefs.edit().putString(KEY_CALLSIGN, value).apply()

    var selectedLanguage: AppLanguage
        get() = AppLanguage.fromCode(prefs.getString(KEY_LANG, "te") ?: "te")
        set(value) = prefs.edit().putString(KEY_LANG, value.code).apply()

    var selectedTransport: TransportType
        get() = TransportType.valueOf(prefs.getString(KEY_TRANSPORT, TransportType.MOCK.name) ?: TransportType.MOCK.name)
        set(value) = prefs.edit().putString(KEY_TRANSPORT, value.name).apply()

    var vadThreshold: Float
        get() = prefs.getFloat(KEY_VAD_THRESH, 450.0f)
        set(value) = prefs.edit().putFloat(KEY_VAD_THRESH, value).apply()

    var isAutoPlayTts: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PLAY, value).apply()

    var isCompressionEnabled: Boolean
        get() = prefs.getBoolean(KEY_COMPRESSION, true)
        set(value) = prefs.edit().putBoolean(KEY_COMPRESSION, value).apply()

    private fun generateAndSaveDeviceId(): String {
        val newId = UUID.randomUUID().toString().take(8).uppercase()
        deviceId = newId
        return newId
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CALLSIGN = "node_callsign"
        private const val KEY_LANG = "selected_lang"
        private const val KEY_TRANSPORT = "selected_transport"
        private const val KEY_VAD_THRESH = "vad_threshold"
        private const val KEY_AUTO_PLAY = "auto_play_tts"
        private const val KEY_COMPRESSION = "compression_enabled"
    }
}
