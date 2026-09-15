package io.github.knigdelioglu.seyir.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
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

                if (launchIntent(packageName) == null) {
                    return@forEach
                }

                val label = resolveInfo.loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { packageName.substringAfterLast('.') }

                val icon = resolveInfo.loadIcon(packageManager)
                    .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)

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
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

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
