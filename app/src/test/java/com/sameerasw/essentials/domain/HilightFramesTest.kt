package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.domain.model.HilightEffect
import com.sameerasw.essentials.domain.model.HilightFrames
import com.sameerasw.essentials.domain.model.HilightPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HilightFramesTest {
    private val red = 0xFFFF0000.toInt()

    private fun frame(
        pattern: HilightPattern,
        t: Long,
        leds: Int = 8,
    ) = HilightFrames.frame(HilightEffect(pattern, red), t, leds)

    @Test
    fun solidLightsEveryLed() {
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.SOLID, 1234))
    }

    @Test
    fun blinkAlternatesEveryHalfSecond() {
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.BLINK, 100))
        assertArrayEquals(IntArray(8) { HilightFrames.OFF }, frame(HilightPattern.BLINK, 600))
    }

    @Test
    fun breatheStartsDarkAndPeaksMidCycle() {
        assertArrayEquals(IntArray(8) { HilightFrames.OFF }, frame(HilightPattern.BREATHE, 0))
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.BREATHE, 1000))
    }

    @Test
    fun pulseFlashesTwiceThenRests() {
        assertEquals(red, frame(HilightPattern.PULSE, 50)[0])
        assertEquals(HilightFrames.OFF, frame(HilightPattern.PULSE, 180)[0])
        assertEquals(red, frame(HilightPattern.PULSE, 300)[0])
        assertEquals(HilightFrames.OFF, frame(HilightPattern.PULSE, 800)[0])
    }

    @Test
    fun chaseHasOneHeadAndADimTail() {
        val colors = frame(HilightPattern.CHASE, 3 * 90L)
        assertEquals(red, colors[3])
        assertEquals(HilightFrames.scale(red, 0.3f), colors[2])
        assertEquals(6, colors.count { it == HilightFrames.OFF })
    }

    @Test
    fun rainbowGivesEachLedADifferentOpaqueColour() {
        val colors = frame(HilightPattern.RAINBOW, 0)
        assertEquals(8, colors.toSet().size)
        assertTrue(colors.all { it ushr 24 == 0xFF })
    }

    @Test
    fun scaleDimsChannelsAndZeroIsOff() {
        assertEquals(0xFF7F0000.toInt(), HilightFrames.scale(red, 0.5f))
        assertEquals(HilightFrames.OFF, HilightFrames.scale(red, 0f))
    }

    @Test
    fun hueToColorMatchesPrimaries() {
        assertEquals(0xFFFF0000.toInt(), HilightFrames.hueToColor(0f))
        assertEquals(0xFF00FF00.toInt(), HilightFrames.hueToColor(120f))
        assertEquals(0xFF0000FF.toInt(), HilightFrames.hueToColor(240f))
    }

    @Test
    fun hilightActionRoundTrips() {
        val action = Action.Hilight(HilightPattern.CHASE, red, 5_000)
        assertEquals(action, ActionGsonAdapter.fromJson(ActionGsonAdapter.toJson(action)))
    }
}
