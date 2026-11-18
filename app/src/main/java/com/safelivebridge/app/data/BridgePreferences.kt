package com.safelivebridge.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BridgePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "safe_livebridge_prefs"
        private const val KEY_BRIDGE_ENABLED = "bridge_enabled"
        private const val KEY_OTP_ENABLED = "otp_enabled"
        private const val KEY_EXPAND_ON_CLICK = "expand_on_click"
        private const val KEY_MEDIA_ENABLED = "media_enabled"
        private const val KEY_KEEP_ALIVE = "keep_alive"
        private const val KEY_ALL_APPS_ENABLED = "all_apps_enabled"
        private const val KEY_WHITELIST = "whitelist_packages"
        private const val KEY_BLACKLIST = "blacklist_packages"

        @Volatile
        private var instance: BridgePreferences? = null

        fun getInstance(context: Context): BridgePreferences {
            return instance ?: synchronized(this) {
                instance ?: BridgePreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _isBridgeEnabled = MutableStateFlow(prefs.getBoolean(KEY_BRIDGE_ENABLED, true))
    val isBridgeEnabled: StateFlow<Boolean> = _isBridgeEnabled.asStateFlow()

    private val _isOtpEnabled = MutableStateFlow(prefs.getBoolean(KEY_OTP_ENABLED, true))
    val isOtpEnabled: StateFlow<Boolean> = _isOtpEnabled.asStateFlow()

    private val _isExpandOnClick = MutableStateFlow(prefs.getBoolean(KEY_EXPAND_ON_CLICK, true))
    val isExpandOnClick: StateFlow<Boolean> = _isExpandOnClick.asStateFlow()

    private val _isMediaEnabled = MutableStateFlow(prefs.getBoolean(KEY_MEDIA_ENABLED, true))
    val isMediaEnabled: StateFlow<Boolean> = _isMediaEnabled.asStateFlow()

    private val _isKeepAliveEnabled = MutableStateFlow(prefs.getBoolean(KEY_KEEP_ALIVE, true))
    val isKeepAliveEnabled: StateFlow<Boolean> = _isKeepAliveEnabled.asStateFlow()

    private val _isAllAppsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALL_APPS_ENABLED, true))
    val isAllAppsEnabled: StateFlow<Boolean> = _isAllAppsEnabled.asStateFlow()

    private val _whitelist = MutableStateFlow(prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    private val _blacklist = MutableStateFlow(prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet())
    val blacklist: StateFlow<Set<String>> = _blacklist.asStateFlow()

    fun setBridgeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BRIDGE_ENABLED, enabled).apply()
        _isBridgeEnabled.value = enabled
    }

    fun setOtpEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OTP_ENABLED, enabled).apply()
        _isOtpEnabled.value = enabled
    }

    fun setExpandOnClick(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXPAND_ON_CLICK, enabled).apply()
        _isExpandOnClick.value = enabled
    }

    fun setMediaEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEDIA_ENABLED, enabled).apply()
        _isMediaEnabled.value = enabled
    }

    fun setKeepAliveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ALIVE, enabled).apply()
        _isKeepAliveEnabled.value = enabled
    }

    fun setAllAppsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALL_APPS_ENABLED, enabled).apply()
        _isAllAppsEnabled.value = enabled
    }

    fun toggleAppWhitelist(packageName: String, isAllowed: Boolean) {
        val current = _whitelist.value.toMutableSet()
        if (isAllowed) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
        _whitelist.value = current
    }

    fun toggleAppBlacklist(packageName: String, isBlocked: Boolean) {
        val current = _blacklist.value.toMutableSet()
        if (isBlocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_BLACKLIST, current).apply()
        _blacklist.value = current
    }

    fun isAppAllowed(packageName: String): Boolean {
        if (_blacklist.value.contains(packageName)) return false
        if (_isAllAppsEnabled.value) return true
        return _whitelist.value.contains(packageName)
    }
}
