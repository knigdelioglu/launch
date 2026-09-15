package io.github.knigdelioglu.seyir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.knigdelioglu.seyir.ui.HomeScreen
import io.github.knigdelioglu.seyir.ui.HomeViewModel
import io.github.knigdelioglu.seyir.ui.theme.SeyirTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        setContent {
            SeyirTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                HomeScreen(
                    uiState = uiState,
                    onAppClick = viewModel::openApp,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        viewModel.refresh()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
