package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SyncPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_JIANGUO_USER = "jianguo_user"
        private const val KEY_JIANGUO_PASS = "jianguo_pass"
        private const val KEY_JIANGUO_URL = "jianguo_url"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val DEFAULT_URL = "https://dav.jianguoyun.com/dav/"
    }

    var jianguoUser: String
        get() = prefs.getString(KEY_JIANGUO_USER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JIANGUO_USER, value).apply()

    var jianguoPass: String
        get() = prefs.getString(KEY_JIANGUO_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JIANGUO_PASS, value).apply()

    var jianguoUrl: String
        get() = prefs.getString(KEY_JIANGUO_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_JIANGUO_URL, value).apply()

    var lastSyncTime: String
        get() = prefs.getString(KEY_LAST_SYNC_TIME, "无记录") ?: "无记录"
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_TIME, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    val isConfigured: Boolean
        get() = jianguoUser.isNotEmpty() && jianguoPass.isNotEmpty()
}
