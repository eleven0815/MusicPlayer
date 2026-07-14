package com.yinqi.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun removesVersionNoiseAndPunctuation() {
        assertEquals("song", TextNormalizer.normalize("Song (feat. Artist) [Live]"))
    }

    @Test
    fun normalizesLyricsFileStemsLikeMetadata() {
        assertEquals(TextNormalizer.normalize("Forest Echo"), TextNormalizer.normalizeLyricsKey("Forest-Echo"))
    }
}
