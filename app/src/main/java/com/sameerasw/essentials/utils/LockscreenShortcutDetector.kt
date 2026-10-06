/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: LockscreenShortcutDetector.kt
 * Description: Reads the System UI lock screen through accessibility to find the system's own shortcut buttons.
 */

package com.sameerasw.essentials.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * System UI keeps lock screen shortcut selections in a provider guarded by a signature permission,
 * so instead we look for the shortcut buttons themselves in the lock screen's accessibility tree.
 */
object LockscreenShortcutDetector {
    private const val SYSTEM_UI = "com.android.systemui"
    private const val MAX_NODES = 600

    // Lock screen root markers: scene container builds use "element:lockscreen", legacy keyguard uses the rest
    private val LOCKSCREEN_MARKERS =
        listOf("element:lockscreen", "keyguard_root_view", "keyguard_bottom_area", "keyguard_indication_area")
    // Present only when the bouncer, shade or quick settings cover the lock screen
    private val OCCLUDING_MARKERS =
        listOf("bouncer", "element:shade", "element:quickSettings", "qs_frame", "shade_header_root", "quick_qs_panel")
    private val AFFORDANCE_IDS = listOf("start_button", "end_button")
    private val EXCLUDED_IDS = listOf("device_entry_icon", "lock_icon", "keyguard_indication", "date", "clock")

    data class Result(
        val isLockscreenVisible: Boolean,
        val hasSystemShortcuts: Boolean,
    )

    fun scan(
        service: AccessibilityService,
        screenWidth: Int,
        screenHeight: Int,
    ): Result {
        var lockscreenVisible = false
        var occluded = false
        var hasShortcuts = false

        // The service does not subscribe to content-change events, so its node cache goes stale
        // while System UI animates between the lock screen, shade and bouncer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) service.clearCache()
        val refreshEachNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

        val roots =
            try {
                service.windows.mapNotNull { it.root }.filter { it.packageName?.toString() == SYSTEM_UI }
            } catch (_: Exception) {
                emptyList()
            }

        val bounds = Rect()
        for (root in roots) {
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                if (refreshEachNode) node.refresh()
                if (!node.isVisibleToUser) continue

                val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
                if (LOCKSCREEN_MARKERS.any { id == it }) lockscreenVisible = true
                if (OCCLUDING_MARKERS.any { id.contains(it) }) occluded = true

                node.getBoundsInScreen(bounds)
                val isCandidate =
                    isShortcutCandidate(
                        id, node.isClickable, bounds.left, bounds.top, bounds.right, bounds.bottom,
                        screenWidth, screenHeight,
                    )
                if (isCandidate) {
                    hasShortcuts = true
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            }
        }

        return Result(
            isLockscreenVisible = lockscreenVisible && !occluded,
            hasSystemShortcuts = hasShortcuts,
        )
    }

    /**
     * A System UI node counts as a shortcut if it has a known affordance id, or if it is a
     * clickable element sitting in the bottom-left or bottom-right corner of the screen.
     */
    fun isShortcutCandidate(
        id: String,
        isClickable: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean {
        if (AFFORDANCE_IDS.any { id == it }) return true
        if (!isClickable || screenWidth <= 0 || screenHeight <= 0) return false
        if (EXCLUDED_IDS.any { id.contains(it) }) return false
        val width = right - left
        if (width <= 0 || bottom - top <= 0) return false
        // Full-width rows (notifications, indication text) are never corner shortcuts
        if (width > screenWidth / 3) return false

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val inBottomBand = centerY > screenHeight * 0.85f
        val inCorner = centerX < screenWidth * 0.25f || centerX > screenWidth * 0.75f
        return inBottomBand && inCorner
    }
}
