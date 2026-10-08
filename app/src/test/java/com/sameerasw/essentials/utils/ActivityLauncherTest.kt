package com.sameerasw.essentials.utils

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityLauncherTest {
    @Test
    fun openActivityRoundTripKeepsTarget() {
        val action =
            Action.OpenActivity(
                packageName = "com.android.settings",
                className = "com.android.settings.Settings\$DevelopmentSettingsActivity",
                label = "Developer options",
                requiresRoot = true,
            )
        assertEquals(action, ActionGsonAdapter.fromJson(ActionGsonAdapter.toJson(action)))
    }

    @Test
    fun onlyRootOnlyActivitiesRequestRoot() {
        assertEquals(listOf("ROOT"), Action.OpenActivity(requiresRoot = true).permissions)
        assertTrue(Action.OpenActivity(requiresRoot = false).permissions.isEmpty())
    }

    @Test
    fun shortClassNameDropsOwnPackagePrefixOnly() {
        assertEquals(".ui.Main", ActivityLauncherUtil.shortClassName("com.app", "com.app.ui.Main"))
        assertEquals("org.other.Main", ActivityLauncherUtil.shortClassName("com.app", "org.other.Main"))
        assertEquals("com.apple.Main", ActivityLauncherUtil.shortClassName("com.app", "com.apple.Main"))
    }
}
