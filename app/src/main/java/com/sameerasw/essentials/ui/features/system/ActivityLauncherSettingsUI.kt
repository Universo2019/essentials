/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: System Features
 * File: ActivityLauncherSettingsUI.kt
 * Description: Standalone activity launcher: browse any app's activities, open them or pin them to the home screen.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.OpenActivityPicker
import com.sameerasw.essentials.ui.modifiers.highlight
import kotlinx.coroutines.launch

@Composable
fun ActivityLauncherSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_app_registration_24,
                title = stringResource(R.string.activity_launcher_browse_title),
                description = stringResource(R.string.activity_launcher_browse_desc),
                showToggle = false,
                onClick = { showPicker = true },
                modifier = Modifier.highlight(highlightSetting == "browse_activities"),
            )
        }

        RoundedCardContainer {
            Text(
                text = stringResource(R.string.activity_launcher_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPicker) {
        // Stays open so several activities can be tried in a row
        OpenActivityPicker(
            onDismiss = { showPicker = false },
            onActivitySelected = { action -> scope.launch { CombinedActionExecutor.execute(context, action) } },
            closeOnSelect = false,
        )
    }
}
