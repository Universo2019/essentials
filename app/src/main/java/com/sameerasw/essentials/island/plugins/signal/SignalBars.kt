package com.sameerasw.essentials.island.plugins.signal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.island.ui.IslandTextStyles

private const val LABEL_SHIFT_X = 0.15f
private const val LABEL_SHIFT_Y = 0.15f

@Composable
fun SignalBars(level: Int, color: Color, label: String?, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = IslandTextStyles.compact.copy(
        fontSize = 7.sp,
        lineHeight = 7.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val bars = 4
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val barHeight = size.height * (i + 1) / bars
            drawRoundRect(
                color = if (i < level) color else Color.White.copy(alpha = 0.3f),
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
        if (label != null) {
            drawText(measurer, label, topLeft = Offset(-size.width * LABEL_SHIFT_X, -size.height * LABEL_SHIFT_Y), style = labelStyle)
        }
    }
}
