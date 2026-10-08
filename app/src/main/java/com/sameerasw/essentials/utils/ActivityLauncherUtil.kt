/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: ActivityLauncherUtil.kt
 * Description: Lists an app's activities for the Open activity action and resolves their icons.
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

object ActivityLauncherUtil {
    data class LaunchableActivity(
        val className: String,
        val label: String,
        val requiresRoot: Boolean,
    )

    // Non-exported or permission-guarded activities need root
    fun getActivities(
        context: Context,
        packageName: String,
    ): List<LaunchableActivity> {
        val pm = context.packageManager
        val info =
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            } catch (_: Exception) {
                return emptyList()
            }
        return info.activities
            .orEmpty()
            .map { activity ->
                val isGuarded =
                    activity.permission != null &&
                        context.checkSelfPermission(activity.permission) != PackageManager.PERMISSION_GRANTED
                LaunchableActivity(
                    className = activity.name,
                    label = activity.loadLabel(pm).toString().ifBlank { activity.name.substringAfterLast('.') },
                    requiresRoot = !activity.exported || isGuarded,
                )
            }.sortedWith(compareBy({ it.requiresRoot }, { it.label.lowercase() }, { it.className }))
    }

    fun shortClassName(
        packageName: String,
        className: String,
    ): String = if (className.startsWith("$packageName.")) className.removePrefix(packageName) else className

    fun loadIcon(
        context: Context,
        packageName: String,
        className: String,
        sizePx: Int,
    ): Bitmap? =
        try {
            context.packageManager
                .getActivityIcon(ComponentName(packageName, className))
                .toBitmap(sizePx, sizePx)
        } catch (_: Exception) {
            null
        }
}
