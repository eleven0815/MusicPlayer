package com.yinqi.player.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackModeTest {
    @Test
    fun cyclesThroughAllPlaybackModes() {
        assertEquals(PlaybackMode.RepeatOne, PlaybackMode.Sequential.next())
        assertEquals(PlaybackMode.Shuffle, PlaybackMode.RepeatOne.next())
        assertEquals(PlaybackMode.Sequential, PlaybackMode.Shuffle.next())
    }

    @Test
    fun mapsModesToMedia3Settings() {
        assertEquals(Player.REPEAT_MODE_OFF, PlaybackMode.Sequential.repeatMode)
        assertFalse(PlaybackMode.Sequential.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, PlaybackMode.RepeatOne.repeatMode)
        assertFalse(PlaybackMode.RepeatOne.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_OFF, PlaybackMode.Shuffle.repeatMode)
        assertTrue(PlaybackMode.Shuffle.shuffleEnabled)
    }
}
