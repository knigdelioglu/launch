package io.github.knigdelioglu.seyir.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
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
) : InstalledAppSource {
    private val packageManager = context.packageManager
    private val cacheLock = Any()
    private val metadataCache = mutableMapOf<AppCacheKey, AppMetadata>()
    private val launchIntentCache = mutableMapOf<AppCacheKey, Intent?>()

    override suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val candidatesByPackage = linkedMapOf<String, ResolveInfo>()
        query(Intent.CATEGORY_LEANBACK_LAUNCHER).forEach { resolveInfo ->
            addCandidate(candidatesByPackage, resolveInfo)
        }
        query(Intent.CATEGORY_LAUNCHER).forEach { resolveInfo ->
            addCandidate(candidatesByPackage, resolveInfo)
        }

        val currentKeys = linkedSetOf<AppCacheKey>()
        val candidates = linkedMapOf<String, InstalledApp>()
        candidatesByPackage.values.forEach { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@forEach
            val packageName = activityInfo.packageName
            val versionKey = packageVersionKey(packageName) ?: return@forEach
            val cacheKey = AppCacheKey(packageName, versionKey)
            currentKeys += cacheKey

            if (cachedLaunchIntent(cacheKey, packageName) == null) return@forEach

            val metadata = metadataCache(cacheKey, resolveInfo) ?: return@forEach
            candidates[packageName] = InstalledApp(
                packageName = packageName,
                label = metadata.label,
                icon = metadata.icon,
            )
        }
        invalidateCaches(currentKeys)

        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        candidates.values.sortedWith { left, right -> collator.compare(left.label, right.label) }
    }

    override fun launch(packageName: String): Boolean {
        val intent = launchIntent(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startActivity(intent)
    }

    override fun openAppInfo(packageName: String): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return startActivity(intent)
    }

    private fun addCandidate(
        candidatesByPackage: MutableMap<String, ResolveInfo>,
        resolveInfo: ResolveInfo,
    ) {
        val activityInfo = resolveInfo.activityInfo ?: return
        val packageName = activityInfo.packageName
        if (
            packageName == context.packageName ||
            packageName in candidatesByPackage ||
            !activityInfo.enabled ||
            !activityInfo.applicationInfo.enabled
        ) {
            return
        }
        candidatesByPackage[packageName] = resolveInfo
    }

    private fun metadataCache(cacheKey: AppCacheKey, resolveInfo: ResolveInfo): AppMetadata? {
        synchronized(cacheLock) {
            metadataCache[cacheKey]?.let { return it }
        }

        val packageName = cacheKey.packageName
        val label = try {
            resolveInfo.loadLabel(packageManager)
                .toString()
                .trim()
        } catch (_: Exception) {
            ""
        }.ifBlank { packageName.substringAfterLast('.') }

        val drawable = try {
            resolveInfo.loadIcon(packageManager)
        } catch (_: Exception) {
            packageManager.defaultActivityIcon
        }
        val icon = try {
            drawable.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
        } catch (_: Exception) {
            null
        } ?: return null

        val metadata = AppMetadata(label = label, icon = icon)
        synchronized(cacheLock) {
            metadataCache[cacheKey] = metadata
        }
        return metadata
    }

    private fun cachedLaunchIntent(cacheKey: AppCacheKey, packageName: String): Intent? {
        synchronized(cacheLock) {
            if (launchIntentCache.containsKey(cacheKey)) {
                return launchIntentCache[cacheKey]?.let(::Intent)
            }
        }

        val intent = launchIntent(packageName)
        synchronized(cacheLock) {
            launchIntentCache[cacheKey] = intent?.let(::Intent)
        }
        return intent?.let(::Intent)
    }

    private fun invalidateCaches(currentKeys: Set<AppCacheKey>) {
        synchronized(cacheLock) {
            metadataCache.keys.retainAll(currentKeys)
            launchIntentCache.keys.retainAll(currentKeys)
        }
    }

    private fun startActivity(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun launchIntent(packageName: String): Intent? =
        packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)

    @Suppress("DEPRECATION")
    private fun packageVersionKey(packageName: String): String? = try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        "$versionCode:${packageInfo.lastUpdateTime}"
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun query(category: String): List<ResolveInfo> =
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

    private data class AppCacheKey(
        val packageName: String,
        val versionKey: String,
    )

    private data class AppMetadata(
        val label: String,
        val icon: Bitmap,
    )

    private companion object {
        const val ICON_SIZE_PX = 160
    }
}
