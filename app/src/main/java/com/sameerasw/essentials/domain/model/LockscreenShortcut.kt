/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: LockscreenShortcut.kt
 * Description: Lock screen shortcut sides and the detected state of the system's own shortcuts.
 */

package com.sameerasw.essentials.domain.model

import com.sameerasw.essentials.data.repository.SettingsRepository

enum class LockscreenShortcutSide(
    val prefKey: String,
) {
    LEFT(SettingsRepository.KEY_LOCKSCREEN_SHORTCUT_LEFT_ACTIONS),
    RIGHT(SettingsRepository.KEY_LOCKSCREEN_SHORTCUT_RIGHT_ACTIONS),
}

/**
 * Whether System UI is showing its own lock screen shortcuts. Only known after the
 * accessibility service has seen the lock screen at least once.
 */
enum class SystemShortcutsState {
    UNKNOWN,
    NONE,
    PRESENT,
}
