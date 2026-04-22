package com.michaelmoros.debttracker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.michaelmoros.debttracker.ui.settings.ExportNamingConvention
import com.michaelmoros.debttracker.ui.settings.ItemSize

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var itemSize: ItemSize
        get() {
            val name = prefs.getString("item_size", ItemSize.MEDIUM.name)
            return try { ItemSize.valueOf(name ?: ItemSize.MEDIUM.name) } catch (e: Exception) { ItemSize.MEDIUM }
        }
        set(value) = prefs.edit { putString("item_size", value.name) }

    var currencySymbol: String
        get() = prefs.getString("currency_symbol", "₱") ?: "₱"
        set(value) = prefs.edit { putString("currency_symbol", value) }

    var exportNamingConvention: ExportNamingConvention
        get() {
            val name = prefs.getString("export_naming", ExportNamingConvention.DEFAULT.name)
            return try { ExportNamingConvention.valueOf(name ?: ExportNamingConvention.DEFAULT.name) } catch (e: Exception) { ExportNamingConvention.DEFAULT }
        }
        set(value) = prefs.edit { putString("export_naming", value.name) }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            return try { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit { putString("theme_mode", value.name) }

    fun resetToDefaults() {
        prefs.edit { clear() }
    }
}
