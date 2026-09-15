package io.github.knigdelioglu.seyir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.knigdelioglu.seyir.ui.AllAppsScreen
import io.github.knigdelioglu.seyir.ui.HomeScreen
import io.github.knigdelioglu.seyir.ui.HomeViewModel
import io.github.knigdelioglu.seyir.ui.theme.SeyirTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()
    private var packageReceiverRegistered = false

    private val packageChangesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        setContent {
            SeyirTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                var showAllApps by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = showAllApps) {
                    showAllApps = false
                }

                if (showAllApps) {
                    AllAppsScreen(
                        apps = uiState.apps,
                        transientMessage = uiState.transientMessage,
                        onAppClick = viewModel::openApp,
                        onBack = { showAllApps = false },
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )
                } else {
                    HomeScreen(
                        uiState = uiState,
                        onAppClick = viewModel::openApp,
                        onOpenAllApps = { showAllApps = true },
                        onRetry = viewModel::refresh,
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        viewModel.refresh()
    }

    override fun onStop() {
        if (packageReceiverRegistered) {
            unregisterReceiver(packageChangesReceiver)
            packageReceiverRegistered = false
        }
        super.onStop()
    }

    private fun registerPackageReceiver() {
        if (packageReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(
            this,
            packageChangesReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        packageReceiverRegistered = true
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
