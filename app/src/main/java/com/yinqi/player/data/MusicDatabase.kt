package com.yinqi.player.data

import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class EnrichmentStatus {
    Missing,
    Resolving,
    Ready,
    ReviewRequired,
    NotFound,
    Failed,
}

@Entity(tableName = "tracks")
data class TrackEntity(
    @androidx.room.PrimaryKey val trackKey: String,
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val durationMs: Long,
    val albumId: Long,
    val folder: String,
    val displayName: String,
    val dateAddedSeconds: Long,
    val sizeBytes: Long,
    val dateModifiedSeconds: Long,
    val localArtworkUri: String?,
    val localLyricsUri: String?,
    val audioVersion: String,
)

@Entity(tableName = "enrichments")
data class EnrichmentEntity(
    @androidx.room.PrimaryKey val trackKey: String,
    val artworkUri: String? = null,
    val artworkSource: String? = null,
    val artworkStatus: String = EnrichmentStatus.Missing.name,
    val lyricsRaw: String? = null,
    val lyricsSource: String? = null,
    val lyricsStatus: String = EnrichmentStatus.Missing.name,
    val confidence: Float = 0f,
    val manualArtwork: Boolean = false,
    val manualLyrics: Boolean = false,
    val candidateArtworkUri: String? = null,
    val candidateLyricsRaw: String? = null,
    val candidateSource: String? = null,
    val audioVersion: String = "",
    val errorMessage: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY dateAddedSeconds DESC, title COLLATE NOCASE")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAddedSeconds DESC, title COLLATE NOCASE")
    suspend fun getAll(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT trackKey FROM tracks")
    suspend fun getKeys(): List<String>

    @Query("DELETE FROM tracks WHERE trackKey NOT IN (:keys)")
    suspend fun deleteExcept(keys: List<String>)

    @Query("DELETE FROM tracks")
    suspend fun clear()

    @Query("DELETE FROM tracks WHERE trackKey = :key")
    suspend fun deleteByKey(key: String)
}

@Dao
interface EnrichmentDao {
    @Query("SELECT * FROM enrichments")
    fun observeAll(): Flow<List<EnrichmentEntity>>

    @Query("SELECT * FROM enrichments WHERE trackKey = :trackKey LIMIT 1")
    suspend fun get(trackKey: String): EnrichmentEntity?

    @Query("SELECT * FROM enrichments")
    suspend fun getAll(): List<EnrichmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EnrichmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EnrichmentEntity)

    @Query("UPDATE enrichments SET artworkStatus = CASE WHEN manualArtwork = 1 THEN artworkStatus ELSE :missing END, lyricsStatus = CASE WHEN manualLyrics = 1 THEN lyricsStatus ELSE :missing END, errorMessage = NULL")
    suspend fun resetPendingBulk(missing: String)

    @Query("UPDATE enrichments SET artworkUri = CASE WHEN manualArtwork = 1 THEN artworkUri ELSE NULL END, artworkSource = CASE WHEN manualArtwork = 1 THEN artworkSource ELSE NULL END, artworkStatus = CASE WHEN manualArtwork = 1 THEN artworkStatus ELSE :missing END, lyricsRaw = CASE WHEN manualLyrics = 1 THEN lyricsRaw ELSE NULL END, lyricsSource = CASE WHEN manualLyrics = 1 THEN lyricsSource ELSE NULL END, lyricsStatus = CASE WHEN manualLyrics = 1 THEN lyricsStatus ELSE :missing END, candidateArtworkUri = NULL, candidateLyricsRaw = NULL, candidateSource = NULL")
    suspend fun clearOnlineContentBulk(missing: String)

    @Query("SELECT trackKey FROM enrichments")
    suspend fun getKeys(): List<String>

    @Query("DELETE FROM enrichments WHERE trackKey NOT IN (:keys)")
    suspend fun deleteExcept(keys: List<String>)

    @Query("DELETE FROM enrichments")
    suspend fun clear()

    @Query("DELETE FROM enrichments WHERE trackKey = :key")
    suspend fun deleteByKey(key: String)
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @androidx.room.PrimaryKey val trackKey: String,
)

@Entity(
    tableName = "playlists",
    indices = [androidx.room.Index(value = ["name"], unique = true)],
)
data class PlaylistEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackKey"],
    indices = [androidx.room.Index("playlistId"), androidx.room.Index("trackKey")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackKey: String,
    val position: Int,
)

@androidx.room.Dao
interface CollectionDao {
    @Query("SELECT trackKey FROM favorites ORDER BY trackKey")
    fun observeFavoriteKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackKey = :trackKey")
    suspend fun removeFavorite(trackKey: String)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position")
    fun observePlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(item: PlaylistEntity): Long

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylist(name: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistTracks(items: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackKey = :trackKey")
    suspend fun removePlaylistTrack(playlistId: Long, trackKey: String)
}

@Database(
    entities = [TrackEntity::class, EnrichmentEntity::class, FavoriteEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun enrichmentDao(): EnrichmentDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS favorites (trackKey TEXT NOT NULL, PRIMARY KEY(trackKey))")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playlists_name ON playlists(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_tracks (playlistId INTEGER NOT NULL, trackKey TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(playlistId, trackKey))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackKey ON playlist_tracks(trackKey)")
            }
        }
    }
}

fun LocalSong.toTrackEntity(): TrackEntity = TrackEntity(
    trackKey = trackKey,
    id = id,
    uri = uri.toString(),
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    durationMs = durationMs,
    albumId = albumId,
    folder = folder,
    displayName = displayName,
    dateAddedSeconds = dateAddedSeconds,
    sizeBytes = sizeBytes,
    dateModifiedSeconds = dateModifiedSeconds,
    localArtworkUri = localArtworkUri?.toString(),
    localLyricsUri = localLyricsUri?.toString(),
    audioVersion = audioVersion,
)

fun TrackEntity.toLocalSong(enrichment: EnrichmentEntity?): LocalSong = LocalSong(
    id = id,
    uri = Uri.parse(uri),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    albumId = albumId,
    folder = folder,
    dateAddedSeconds = dateAddedSeconds,
    trackKey = trackKey,
    albumArtist = albumArtist,
    displayName = displayName,
    sizeBytes = sizeBytes,
    dateModifiedSeconds = dateModifiedSeconds,
    localArtworkUri = localArtworkUri?.let(Uri::parse),
    remoteArtworkUri = enrichment?.artworkUri?.let(Uri::parse),
    localLyricsUri = localLyricsUri?.let(Uri::parse),
)
