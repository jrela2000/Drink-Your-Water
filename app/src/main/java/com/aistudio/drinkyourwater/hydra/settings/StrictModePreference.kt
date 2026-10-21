package com.aistudio.drinkyourwater.hydra.settings

import android.content.Context

/**
 * Strict mode: pin the screen (Android screen pinning) while a check-in is showing.
 *
 * Off by default. Android shows its own "App is pinned" dialog every time pinning starts,
 * and "No thanks" skips it anyway, so pinning adds friction without real enforcement for
 * most people; escalating re-alerts do the accountability work instead. Stored per device
 * (not in the database) because it's a phone behavior, not habit data.
 */
object StrictModePreference {
    private const val PREFS = "device_preferences"
    private const val KEY_STRICT_MODE = "strict_mode_screen_pinning"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_STRICT_MODE, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STRICT_MODE, enabled)
            .apply()
    }
}
