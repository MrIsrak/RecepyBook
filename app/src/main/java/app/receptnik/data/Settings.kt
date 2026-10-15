package app.receptnik.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.receptnik.core.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class AppSettings(
    val geminiKey: String = "",
    val geminiModel: String = DEFAULT_MODEL,
    val translateToRussian: Boolean = true,
    val units: UnitSystem = UnitSystem.ORIGINAL,
    val groupGroceryByAisle: Boolean = true,
) {
    companion object { const val DEFAULT_MODEL = "gemini-2.5-flash" }
}

class SettingsStore(private val context: Context) {
    private val kKey = stringPreferencesKey("gemini_key")
    private val kModel = stringPreferencesKey("gemini_model")
    private val kTranslate = booleanPreferencesKey("translate_ru")
    private val kUnits = stringPreferencesKey("units")
    private val kAisle = booleanPreferencesKey("grocery_by_aisle")

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            geminiKey = p[kKey] ?: "",
            geminiModel = p[kModel]?.takeIf { it.isNotBlank() } ?: AppSettings.DEFAULT_MODEL,
            translateToRussian = p[kTranslate] ?: true,
            units = runCatching { UnitSystem.valueOf(p[kUnits] ?: "") }.getOrDefault(UnitSystem.ORIGINAL),
            groupGroceryByAisle = p[kAisle] ?: true,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val cur = AppSettings(
                geminiKey = p[kKey] ?: "",
                geminiModel = p[kModel] ?: AppSettings.DEFAULT_MODEL,
                translateToRussian = p[kTranslate] ?: true,
                units = runCatching { UnitSystem.valueOf(p[kUnits] ?: "") }.getOrDefault(UnitSystem.ORIGINAL),
                groupGroceryByAisle = p[kAisle] ?: true,
            )
            val n = transform(cur)
            p[kKey] = n.geminiKey.trim()
            p[kModel] = n.geminiModel.trim()
            p[kTranslate] = n.translateToRussian
            p[kUnits] = n.units.name
            p[kAisle] = n.groupGroceryByAisle
        }
    }
}
