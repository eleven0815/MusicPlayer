package com.yinqi.player.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val database: MusicDatabase) {
    fun observeTracks(): Flow<List<TrackEntity>> = database.trackDao().observeAll()

    fun observeEnrichments(): Flow<List<EnrichmentEntity>> = database.enrichmentDao().observeAll()

    suspend fun replaceScan(songs: List<LocalSong>) {
        val keys = songs.map { it.trackKey }
        database.withTransaction {
            val previous = database.enrichmentDao().getAll().associateBy { it.trackKey }
            database.trackDao().upsertAll(songs.map(LocalSong::toTrackEntity))
            if (keys.isEmpty()) {
                database.trackDao().clear()
                database.enrichmentDao().clear()
            } else {
                database.trackDao().deleteExcept(keys)
                database.enrichmentDao().deleteExcept(keys)
            }
            database.enrichmentDao().upsertAll(
                songs.map { song ->
                    val existing = previous[song.trackKey]
                    if (existing != null && existing.audioVersion == song.audioVersion) {
                        existing
                    } else {
                        EnrichmentEntity(
                            trackKey = song.trackKey,
                            artworkUri = existing?.artworkUri?.takeIf { existing.manualArtwork },
                            artworkSource = existing?.artworkSource?.takeIf { existing.manualArtwork },
                            artworkStatus = when {
                                existing?.manualArtwork == true -> existing.artworkStatus
                                song.effectiveArtworkUri != null -> EnrichmentStatus.Ready.name
                                else -> EnrichmentStatus.Missing.name
                            },
                            lyricsStatus = if (existing?.manualLyrics == true) existing.lyricsStatus else EnrichmentStatus.Missing.name,
                            manualArtwork = existing?.manualArtwork == true,
                            manualLyrics = existing?.manualLyrics == true,
                            lyricsRaw = existing?.lyricsRaw?.takeIf { existing.manualLyrics },
                            lyricsSource = existing?.lyricsSource?.takeIf { existing.manualLyrics },
                            audioVersion = song.audioVersion,
                        )
                    }
                },
            )
        }
    }

    suspend fun updateEnrichment(trackKey: String, update: (EnrichmentEntity) -> EnrichmentEntity) {
        val current = database.enrichmentDao().get(trackKey) ?: EnrichmentEntity(trackKey)
        database.enrichmentDao().upsert(update(current).copy(updatedAtMillis = System.currentTimeMillis()))
    }
}
