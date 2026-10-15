package app.receptnik

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.receptnik.ui.AppNav
import app.receptnik.ui.ReceptnikTheme

/** Content received through the system share sheet. */
sealed interface Shared {
    data class Text(val text: String) : Shared
    data class Image(val uri: Uri) : Shared
}

class MainActivity : AppCompatActivity() {
    private val shared = mutableStateOf<Shared?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            ReceptnikTheme {
                AppNav(shared = shared.value, onSharedConsumed = { shared.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val type = intent.type ?: return
        if (type.startsWith("image/")) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) shared.value = Shared.Image(uri)
        } else {
            val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_TEXT), intent.getStringExtra(Intent.EXTRA_SUBJECT))
                .joinToString(" ")
            if (text.isNotBlank()) shared.value = Shared.Text(text)
        }
    }
}
