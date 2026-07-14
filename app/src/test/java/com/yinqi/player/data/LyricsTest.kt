package com.yinqi.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test
    fun parsesMultipleTimestampsAndOffset() {
        val lyrics = parseLrc("[offset:-500]\n[00:01.20][00:02.00]Hello")

        assertEquals(listOf(700L, 1_500L), lyrics.lines.map { it.timestampMs })
        assertEquals("Hello\nHello", lyrics.plainText)
        assertEquals(0, lyrics.currentLineIndex(900L))
        assertEquals(1, lyrics.currentLineIndex(1_600L))
    }

    @Test
    fun keepsPlainTextWhenNoTimestampsExist() {
        val lyrics = parseLrc("第一行\n第二行")

        assertTrue(lyrics.lines.isEmpty())
        assertEquals("第一行\n第二行", lyrics.plainText)
    }
}
