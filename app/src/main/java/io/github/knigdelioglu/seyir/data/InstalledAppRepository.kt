package io.github.knigdelioglu.seyir.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale


data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
)

class InstalledAppRepository(
    private val context: Context,
) {
    private val packageManager = context.packageManager

    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val candidates = linkedMapOf<String, InstalledApp>()

        query(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .plus(query(Intent.CATEGORY_LAUNCHER))
            .forEach { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@forEach
                val packageName = activityInfo.packageName

                if (packageName == context.packageName || packageName in candidates) {
                    return@forEach
                }

                if (!activityInfo.enabled || !activityInfo.applicationInfo.enabled) {
                    return@forEach
                }

                if (launchIntent(packageName) == null) {
                    return@forEach
                }

                val label = runCatching {
                    resolveInfo.loadLabel(packageManager)
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                }.getOrDefault("")
                    .ifBlank { packageName.substringAfterLast('.') }

                val drawable = runCatching {
                    resolveInfo.loadIcon(packageManager)
                }.getOrNull() ?: packageManager.defaultActivityIcon

                val icon = runCatching {
                    drawable.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                }.getOrNull() ?: return@forEach

                candidates[packageName] = InstalledApp(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                )
            }

        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

        candidates.values.sortedWith { left, right ->
            collator.compare(left.label, right.label)
        }
    }

    fun launch(packageName: String): Boolean {
        val intent = launchIntent(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startActivity(intent)
    }

    fun openAppInfo(packageName: String): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return startActivity(intent)
    }

    private fun startActivity(intent: Intent): Boolean = runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun launchIntent(packageName: String): Intent? =
        packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)

    private fun query(category: String) =
        Intent(Intent.ACTION_MAIN).addCategory(category).let { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        }

    private companion object {
        const val ICON_SIZE_PX = 160
    }
}
