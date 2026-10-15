package app.receptnik

import android.app.Application
import android.content.Context
import app.receptnik.data.AppDatabase
import app.receptnik.data.AppSettings
import app.receptnik.data.ImageStore
import app.receptnik.data.Repository
import app.receptnik.data.SettingsStore
import app.receptnik.importer.GeminiClient
import app.receptnik.importer.LinkImporter
import app.receptnik.importer.PhotoImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReceptnikApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Простое ручное внедрение зависимостей. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.create(context)
    val images = ImageStore(context)
    val settingsStore = SettingsStore(context)
    val settings: StateFlow<AppSettings> = settingsStore.flow.stateIn(scope, SharingStarted.Eagerly, AppSettings())
    val repo = Repository(db, images)

    private val gemini: () -> GeminiClient? = {
        val s = settings.value
        if (s.geminiKey.isBlank()) null else GeminiClient(s.geminiKey, s.geminiModel, s.translateToRussian)
    }
    val links = LinkImporter(gemini)
    val photos = PhotoImporter(context, gemini, links)
}

val Context.container: AppContainer get() = (applicationContext as ReceptnikApp).container
