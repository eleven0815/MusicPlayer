package com.yinqi.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataMatchScorerTest {
    private val track = TrackEntity(
        trackKey = "track",
        id = 1L,
        uri = "content://track",
        title = "Rain Song",
        artist = "Yinqi",
        album = "Forest Echo",
        albumArtist = "Yinqi",
        durationMs = 180_000L,
        albumId = 1L,
        folder = "Music",
        displayName = "Rain Song.mp3",
        dateAddedSeconds = 1L,
        sizeBytes = 100L,
        dateModifiedSeconds = 1L,
        localArtworkUri = null,
        localLyricsUri = null,
        audioVersion = "100:1:180000",
    )

    @Test
    fun normalizesExactMetadata() {
        val score = MetadataMatchScorer.score(track, "rain song", "YINQI", "Forest Echo", 180_000L)

        assertEquals(1f, score, 0.001f)
    }

    @Test
    fun toleratesMissingAlbumButKeepsDurationSignal() {
        val score = MetadataMatchScorer.score(track, "Rain Song", "Yinqi", null, 181_000L)

        assertTrue(score >= 0.9f)
    }
}
