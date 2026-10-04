package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ShellUtils

object LiveUpdateSnoozer {
    private const val SNOOZE_MS = 6 * 60 * 60 * 1000L

    private val liveKeys = HashSet<String>()
    private val snoozed = HashSet<String>()
    private var islandVisible = false

    fun isLiveUpdate(sbn: StatusBarNotification): Boolean =
        Build.VERSION.SDK_INT >= 36 &&
            sbn.notification.extras?.getBoolean("android.requestPromotedOngoing", false) == true

    @Synchronized
    fun onPosted(
        listener: NotificationListener,
        sbn: StatusBarNotification,
    ) {
        if (!isLiveUpdate(sbn)) return
        liveKeys += sbn.key
        apply(listener)
    }

    @Synchronized
    fun onRemoved(
        key: String,
        reason: Int,
    ) {
        if (reason == android.service.notification.NotificationListenerService.REASON_SNOOZED && key in snoozed) return
        liveKeys -= key
        snoozed -= key
    }

    @Synchronized
    fun onIslandVisibility(
        context: Context,
        visible: Boolean,
    ) {
        islandVisible = visible
        NotificationListener.instance?.let { apply(it) }
    }

    @Synchronized
    fun onSettingChanged() {
        NotificationListener.instance?.let { apply(it) }
    }

    @Synchronized
    fun release() {
        NotificationListener.instance?.let { unsnoozeAll(it) }
    }

    private fun apply(listener: NotificationListener) {
        val enabled = SettingsRepository(listener).isIslandHideLiveUpdatesEnabled()
        if (enabled && islandVisible) snoozeAll(listener) else unsnoozeAll(listener)
    }

    private fun snoozeAll(listener: NotificationListener) {
        if (!ShellUtils.isAvailable(listener) || !ShellUtils.hasPermission(listener)) return
        liveKeys.filter { it !in snoozed }.forEach { key ->
            try {
                listener.snoozeNotification(key, SNOOZE_MS)
                snoozed += key
            } catch (_: Exception) {
            }
        }
    }

    private fun unsnoozeAll(listener: NotificationListener) {
        if (snoozed.isEmpty()) return
        val keys = snoozed.toList()
        snoozed.clear()
        keys.forEach { key ->
            ShellUtils.runCommand(listener, "cmd notification unsnooze '${key.replace("'", "'\\''")}'", notifyOnError = false)
        }
    }
}
