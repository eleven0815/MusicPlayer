package com.yinqi.player.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers

data class LocalSong(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumId: Long,
    val folder: String,
    val dateAddedSeconds: Long,
    val trackKey: String = uri.toString(),
    val albumArtist: String = artist,
    val displayName: String = title,
    val sizeBytes: Long = 0L,
    val dateModifiedSeconds: Long = 0L,
    val localArtworkUri: Uri? = null,
    val remoteArtworkUri: Uri? = null,
    val localLyricsUri: Uri? = null,
) {
    val audioVersion: String
        get() = "$sizeBytes:$dateModifiedSeconds:$durationMs"

    val albumArtUri: Uri?
        get() = albumId.takeIf { it > 0 }?.let {
            ContentUris.withAppendedId(ALBUM_ART_URI, it)
        }

    val effectiveArtworkUri: Uri?
        get() = remoteArtworkUri ?: localArtworkUri ?: albumArtUri

    companion object {
        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
    }
}

class MediaRepository(private val context: Context) {

    fun readLyrics(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText().takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    fun scanLyricsTree(treeUri: Uri): Map<String, Uri> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyMap()
        val result = mutableMapOf<String, Uri>()
        collectLyricsFiles(root, result)
        return result
    }

    fun scanMediaStore(includeShortAudio: Boolean = false): List<LocalSong> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM_ARTIST,
        )
        val minimumDuration = if (includeShortAudio) 1_000L else 30_000L
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minimumDuration"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return buildList {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val albumArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(dataColumn).orEmpty()
                    val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val sizeBytes = cursor.getLong(sizeColumn)
                    val dateModifiedSeconds = cursor.getLong(modifiedColumn)
                    val localLyricsUri = path.takeIf { it.isNotBlank() }
                        ?.let { audioPath ->
                            val lyrics = File(audioPath).parentFile?.resolve("${File(audioPath).nameWithoutExtension}.lrc")
                            lyrics?.takeIf(File::exists)?.let(Uri::fromFile)
                        }
                    add(
                        LocalSong(
                            id = id,
                            uri = audioUri,
                            title = cursor.getString(titleColumn).orFallback("未知歌曲"),
                            artist = cursor.getString(artistColumn).orFallback("未知艺人"),
                            album = cursor.getString(albumColumn).orFallback("未知专辑"),
                            durationMs = cursor.getLong(durationColumn),
                            albumId = cursor.getLong(albumIdColumn),
                            folder = File(path).parentFile?.name.orFallback("Music"),
                            dateAddedSeconds = cursor.getLong(addedColumn),
                            trackKey = audioUri.toString(),
                            albumArtist = cursor.getString(albumArtistColumn).orFallback(cursor.getString(artistColumn).orFallback("未知艺人")),
                            displayName = cursor.getString(displayNameColumn).orFallback(cursor.getString(titleColumn).orFallback("未知歌曲")),
                            sizeBytes = sizeBytes,
                            dateModifiedSeconds = dateModifiedSeconds,
                            localLyricsUri = localLyricsUri,
                            localArtworkUri = null,
                        ),
                    )
                }
            }
        }
    }

    fun scanDocumentTree(treeUri: Uri, includeShortAudio: Boolean = false): List<LocalSong> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val files = mutableListOf<Pair<DocumentFile, DocumentFile?>>()
        collectAudioFiles(root, files)

        return files.mapNotNull { (file, lyricFile) ->
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, file.uri)
                    val trackKey = file.uri.toString()
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val sizeBytes = file.length()
                    val dateModifiedSeconds = file.lastModified() / 1_000L
                    if (!includeShortAudio && durationMs < 30_000L) return@runCatching null
                    LocalSong(
                        id = trackKey.hashCode().toLong(),
                        uri = file.uri,
                        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orFallback(file.name.orFallback("未知歌曲")),
                        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orFallback("本地文件"),
                        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orFallback(root.name.orFallback("本地文件夹")),
                        durationMs = durationMs,
                        albumId = 0L,
                        folder = root.name.orFallback("本地文件夹"),
                        dateAddedSeconds = dateModifiedSeconds,
                        trackKey = trackKey,
                        albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orFallback(
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orFallback("本地文件"),
                        ),
                        displayName = file.name.orFallback("未知歌曲"),
                        sizeBytes = sizeBytes,
                        dateModifiedSeconds = dateModifiedSeconds,
                        localArtworkUri = retriever.embeddedPicture?.let { picture -> writeEmbeddedArtwork(trackKey, picture) },
                        localLyricsUri = lyricFile?.uri,
                    )
                } finally {
                    retriever.release()
                }
            }.getOrNull()
        }.filterNotNull().sortedByDescending { it.dateAddedSeconds }
    }

    suspend fun extractEmbeddedArtwork(songs: List<LocalSong>): Map<String, Uri> = coroutineScope {
        val groups = songs.groupBy { song ->
            if (song.albumId > 0L) "album:${song.albumId}" else song.trackKey
        }
        val extracted = groups.map { (groupKey, group) ->
            async(Dispatchers.IO.limitedParallelism(4)) {
                groupKey to extractEmbeddedArtwork(group.first().uri)
            }
        }.awaitAll()
        val artworkByGroup = extracted.toMap()
        groups.flatMap { (groupKey, group) ->
            artworkByGroup[groupKey]?.let { uri -> group.map { it.trackKey to uri } }.orEmpty()
        }.toMap()
    }

    private fun collectAudioFiles(directory: DocumentFile, destination: MutableList<Pair<DocumentFile, DocumentFile?>>) {
        val children = directory.listFiles().toList()
        children.forEach { file ->
            when {
                file.isDirectory -> collectAudioFiles(file, destination)
                file.isFile && file.isAudioFile() -> {
                    val stem = file.name?.substringBeforeLast('.', missingDelimiterValue = "").orEmpty()
                    val lyrics = children.firstOrNull { sibling ->
                        sibling.isFile && sibling.name?.substringBeforeLast('.', missingDelimiterValue = "")?.equals(stem, ignoreCase = true) == true &&
                            sibling.name?.substringAfterLast('.', missingDelimiterValue = "")?.equals("lrc", ignoreCase = true) == true
                    }
                    destination += file to lyrics
                }
            }
        }
    }

    private fun DocumentFile.isAudioFile(): Boolean {
        if (type?.startsWith("audio/") == true) return true
        val extension = name?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
        return extension in setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")
    }

    private fun collectLyricsFiles(directory: DocumentFile, destination: MutableMap<String, Uri>) {
        directory.listFiles().forEach { file ->
            when {
                file.isDirectory -> collectLyricsFiles(file, destination)
                file.isFile && file.name?.substringAfterLast('.', missingDelimiterValue = "")?.equals("lrc", ignoreCase = true) == true -> {
                    file.name?.substringBeforeLast('.', missingDelimiterValue = "")?.let { stem ->
                        destination[normalizeFileStem(stem)] = file.uri
                    }
                }
            }
        }
    }

    private fun writeEmbeddedArtwork(trackKey: String, bytes: ByteArray): Uri {
        val directory = File(context.filesDir, "embedded-art").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(trackKey.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        val file = File(directory, "$digest.jpg")
        if (!file.exists()) file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    private fun extractEmbeddedArtwork(uri: Uri): Uri? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { writeEmbeddedArtwork(uri.toString(), it) }
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun String?.orFallback(fallback: String): String =
    this?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback

private fun normalizeFileStem(value: String): String = TextNormalizer.normalizeFileStem(value)
