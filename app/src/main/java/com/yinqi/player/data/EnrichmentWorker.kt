package com.yinqi.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.net.Uri
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max

data class LyricsMatch(
    val raw: String,
    val synced: Boolean,
    val score: Float,
)

data class ArtworkMatch(
    val bytes: ByteArray,
    val source: String,
    val score: Float,
)

object MetadataMatchScorer {
    fun score(
        track: TrackEntity,
        title: String?,
        artist: String?,
        album: String?,
        durationMs: Long?,
    ): Float {
        val parts = listOf(
            0.40f to similarity(track.title, title),
            0.30f to similarity(track.artist, artist),
            0.15f to similarity(track.album, album),
            0.15f to durationSimilarity(track.durationMs, durationMs),
        )
        val available = parts.filter { it.second >= 0f }
        if (available.isEmpty()) return 0f
        val totalWeight = available.sumOf { it.first.toDouble() }.toFloat()
        return (available.sumOf { (it.first * it.second).toDouble() } / totalWeight).toFloat()
    }

    private fun similarity(left: String?, right: String?): Float {
        if (left.isNullOrBlank() || right.isNullOrBlank()) return -1f
        val normalizedLeft = normalize(left)
        val normalizedRight = normalize(right)
        return when {
            normalizedLeft == normalizedRight -> 1f
            normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft) -> 0.82f
            else -> 0f
        }
    }

    private fun durationSimilarity(left: Long, right: Long?): Float {
        if (right == null || left <= 0L || right <= 0L) return -1f
        val delta = abs(left - right)
        return when {
            delta <= 2_000L -> 1f
            delta <= 5_000L -> 0.65f
            delta <= 10_000L -> 0.25f
            else -> 0f
        }
    }

    fun normalize(value: String): String = TextNormalizer.normalize(value)
}

class MetadataEnrichmentService(private val context: Context) {
    private val userAgent = "Yinqi/1.1 (https://github.com/eleven0815/MusicPlayer)"

    suspend fun enrich(track: TrackEntity, current: EnrichmentEntity): EnrichmentEntity {
        var result = current.copy(errorMessage = null)
        if (!current.manualLyrics && current.lyricsStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Failed.name, EnrichmentStatus.Resolving.name)) {
            val lyrics = resolveLocalLyrics(track) ?: resolveLyrics(track)
            result = if (lyrics == null) {
                result.copy(lyricsStatus = EnrichmentStatus.NotFound.name, lyricsSource = null)
            } else if (lyrics.score < 0.9f) {
                result.copy(
                    candidateLyricsRaw = lyrics.raw,
                    candidateSource = "LRCLIB",
                    lyricsStatus = EnrichmentStatus.ReviewRequired.name,
                    confidence = max(result.confidence, lyrics.score),
                )
            } else {
                result.copy(
                    lyricsRaw = lyrics.raw,
                    lyricsSource = if (track.localLyricsUri != null && lyrics.score >= 1f) "local" else "LRCLIB",
                    lyricsStatus = EnrichmentStatus.Ready.name,
                    candidateLyricsRaw = null,
                    candidateSource = null,
                    confidence = max(result.confidence, lyrics.score),
                )
            }
        }
        if (!current.manualArtwork && current.artworkStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Failed.name, EnrichmentStatus.Resolving.name)) {
            val artwork = resolveArtwork(track)
            result = if (artwork == null) {
                result.copy(
                    artworkUri = ArtworkGenerator.write(context, track.albumArtist, track.album, track.title).toString(),
                    artworkSource = "generated",
                    artworkStatus = EnrichmentStatus.Ready.name,
                    candidateArtworkUri = null,
                    candidateSource = null,
                    confidence = max(result.confidence, 1f),
                )
            } else {
                val stored = saveArtwork(track, artwork.bytes, artwork.source)
                if (artwork.score < 0.9f) {
                    result.copy(
                        candidateArtworkUri = stored.toString(),
                        candidateSource = artwork.source,
                        artworkStatus = EnrichmentStatus.ReviewRequired.name,
                        confidence = max(result.confidence, artwork.score),
                    )
                } else {
                    result.copy(
                        artworkUri = stored.toString(),
                        artworkSource = artwork.source,
                        artworkStatus = EnrichmentStatus.Ready.name,
                        candidateArtworkUri = null,
                        candidateSource = null,
                        confidence = max(result.confidence, artwork.score),
                    )
                }
            }
        }
        return result
    }

    private suspend fun resolveLocalLyrics(track: TrackEntity): LyricsMatch? = withContext(Dispatchers.IO) {
        val uri = track.localLyricsUri?.let(Uri::parse) ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val raw = input.bufferedReader().readText()
                raw.takeIf { it.isNotBlank() }?.let { LyricsMatch(it, it.contains(Regex("\\[\\d{1,3}:\\d{2}")), 1f) }
            }
        }.getOrNull()
    }

    private suspend fun resolveLyrics(track: TrackEntity): LyricsMatch? {
        val exactUrl = "https://lrclib.net/api/get?track_name=${encode(track.title)}&artist_name=${encode(track.artist)}&album_name=${encode(track.album)}&duration=${track.durationMs / 1000.0}"
        val exact = requestJson(exactUrl)
        parseLyricsCandidate(track, exact)?.let { return it }

        val searchUrl = "https://lrclib.net/api/search?track_name=${encode(track.title)}&artist_name=${encode(track.artist)}"
        val search = requestText(searchUrl) ?: return null
        val candidates = if (search.trimStart().startsWith("[")) {
            JSONArray(search).toListOfObjects()
        } else {
            JSONObject(search).optJSONArray("data")?.toListOfObjects().orEmpty()
        }
        val ranked = candidates.mapNotNull { parseLyricsCandidate(track, it) }.sortedByDescending { it.score }
        val best = ranked.firstOrNull() ?: return null
        val margin = best.score - (ranked.getOrNull(1)?.score ?: 0f)
        return if (ranked.size > 1 && margin < 0.1f) best.copy(score = minOf(best.score, 0.89f)) else best
    }

    private suspend fun resolveArtwork(track: TrackEntity): ArtworkMatch? {
        val query = "recording:\"${escapeQuery(track.title)}\" AND artist:\"${escapeQuery(track.artist)}\""
        val url = "https://musicbrainz.org/ws/2/recording/?query=${encode(query)}&fmt=json&limit=5&inc=releases"
        MusicBrainzRateLimiter.awaitPermit()
        val json = requestJson(url, timeoutMs = 4_000) ?: return null
        val recordings = json.optJSONArray("recordings") ?: return null
        var bestReleaseId: String? = null
        var bestScore = 0f
        var secondScore = 0f
        for (index in 0 until recordings.length()) {
            val recording = recordings.optJSONObject(index) ?: continue
            val score = MetadataMatchScorer.score(
                track,
                recording.optString("title"),
                recording.optJSONArray("artist-credit")?.optJSONObject(0)?.optJSONObject("artist")?.optString("name"),
                recording.optJSONArray("releases")?.optJSONObject(0)?.optString("title"),
                recording.optLong("length").takeIf { it > 0L },
            )
            val release = recording.optJSONArray("releases")?.optJSONObject(0)
            val releaseId = release?.optJSONObject("release-group")?.optString("id")
                ?.takeIf { it.isNotBlank() }
                ?: release?.optString("id")?.takeIf { it.isNotBlank() }
            if (releaseId != null && score > bestScore) {
                secondScore = bestScore
                bestReleaseId = releaseId
                bestScore = score
            } else if (score > secondScore) {
                secondScore = score
            }
        }
        val releaseId = bestReleaseId ?: return null
        val coverBytes = requestBytes("https://coverartarchive.org/release-group/$releaseId/front-500", timeoutMs = 3_000)
            ?: requestBytes("https://coverartarchive.org/release/$releaseId/front-500", timeoutMs = 3_000)
            ?: return null
        val confidence = if (bestScore - secondScore < 0.1f && secondScore > 0f) minOf(bestScore, 0.89f) else bestScore
        return ArtworkMatch(coverBytes, "Cover Art Archive", confidence)
    }

    private fun parseLyricsCandidate(track: TrackEntity, json: JSONObject?): LyricsMatch? {
        if (json == null) return null
        val raw = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
            ?: json.optString("plainLyrics").takeIf { it.isNotBlank() }
            ?: return null
        val score = MetadataMatchScorer.score(
            track,
            json.optString("trackName"),
            json.optString("artistName"),
            json.optString("albumName"),
            json.optDouble("duration").takeIf { it > 0.0 }?.times(1_000.0)?.toLong(),
        )
        return LyricsMatch(raw, json.optString("syncedLyrics").isNotBlank(), score)
    }

    private fun saveArtwork(track: TrackEntity, bytes: ByteArray, source: String): Uri {
        val key = "${track.albumArtist}|${track.album}".ifBlank { track.trackKey }
        val name = MetadataMatchScorer.normalize(key).ifBlank { track.trackKey.hashCode().toString() }
        val directory = File(context.filesDir, "artwork/remote").apply { mkdirs() }
        val file = File(directory, "${name.hashCode().toUInt().toString(16)}.jpg")
        if (!file.exists()) file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    private suspend fun requestJson(url: String, timeoutMs: Int = 6_000): JSONObject? =
        requestText(url, timeoutMs)?.let { body -> runCatching { JSONObject(body) }.getOrNull() }

    private suspend fun requestText(url: String, timeoutMs: Int = 6_000): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299 || connection.contentLengthLong > 512_000L) return@runCatching null
            connection.inputStream.use { it.readLimited(512_000).toString(StandardCharsets.UTF_8) }
        }.getOrNull()
    }

    private suspend fun requestBytes(url: String, timeoutMs: Int): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            if (connection.responseCode !in 200..299) return@runCatching null
            if (!connection.contentType.orEmpty().startsWith("image/", ignoreCase = true)) return@runCatching null
            if (connection.contentLengthLong > 8_000_000L) return@runCatching null
            connection.inputStream.use { it.readLimited(8_000_000) }
        }.getOrNull()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun escapeQuery(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

object ArtworkGenerator {
    fun write(context: Context, artist: String, album: String, title: String): Uri {
        val key = "$artist|$album|$title"
        val directory = File(context.filesDir, "artwork/generated").apply { mkdirs() }
        val file = File(directory, "generated-${key.hashCode().toUInt().toString(16)}.png")
        if (file.exists()) return Uri.fromFile(file)

        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val seed = abs(key.hashCode())
        val palettes = listOf(
            intArrayOf(0xFF123D2B.toInt(), 0xFF81C98F.toInt(), 0xFF0B1711.toInt()),
            intArrayOf(0xFF0B2422.toInt(), 0xFF4F9A8B.toInt(), 0xFF101613.toInt()),
            intArrayOf(0xFF2A183D.toInt(), 0xFFC68CE8.toInt(), 0xFF130E1C.toInt()),
            intArrayOf(0xFF3D2A18.toInt(), 0xFFE4A96B.toInt(), 0xFF17100B.toInt()),
        )[seed % 4]
        canvas.drawColor(palettes[0])
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palettes[1]; alpha = 210 }
        val path = Path().apply {
            moveTo(0f, 360f)
            cubicTo(120f, 230f, 220f, 520f, 360f, 330f)
            cubicTo(430f, 235f, 480f, 270f, 512f, 220f)
            lineTo(512f, 512f)
            lineTo(0f, 512f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = palettes[2]
        paint.alpha = 170
        canvas.drawCircle(380f, 125f, 115f, paint)
        paint.color = 0xCCFFFFFF.toInt()
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 42f
        val label = album.takeIf { it.isNotBlank() } ?: title
        canvas.drawText(label.take(18), 34f, 448f, paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }
}

class EnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? com.yinqi.player.YinqiApplication ?: return Result.failure()
        val tracks = application.database.trackDao().getAll()
        val service = MetadataEnrichmentService(application)
        val artworkByAlbum = mutableMapOf<String, EnrichmentEntity>()
        var completed = 0
        setForeground(createForegroundInfo(completed, tracks.size))
        tracks.forEach { track ->
            val stored = application.database.enrichmentDao().get(track.trackKey) ?: EnrichmentEntity(track.trackKey)
            val current = stored
            val artworkPending = !current.manualArtwork && current.artworkStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Failed.name, EnrichmentStatus.Resolving.name)
            val lyricsPending = !current.manualLyrics && current.lyricsStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Failed.name, EnrichmentStatus.Resolving.name)
            if (!artworkPending && !lyricsPending) {
                completed++
                setProgress(progressData(completed, tracks.size))
                setForeground(createForegroundInfo(completed, tracks.size))
                return@forEach
            }
            application.database.enrichmentDao().upsert(
                current.copy(
                    artworkStatus = if (artworkPending) EnrichmentStatus.Resolving.name else current.artworkStatus,
                    lyricsStatus = if (lyricsPending) EnrichmentStatus.Resolving.name else current.lyricsStatus,
                ),
            )
            val albumKey = MetadataMatchScorer.normalize("${track.albumArtist}|${track.album}")
            val prepared = if (current.manualArtwork) {
                current
            } else artworkByAlbum[albumKey]?.let { cached ->
                current.copy(
                    artworkUri = cached.artworkUri,
                    artworkSource = cached.artworkSource,
                    artworkStatus = cached.artworkStatus,
                    candidateArtworkUri = cached.candidateArtworkUri,
                    candidateSource = cached.candidateSource,
                    confidence = max(current.confidence, cached.confidence),
                )
            } ?: current
            val updated = try {
                service.enrich(track, prepared)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                current.copy(errorMessage = error.message ?: "在线补全失败", artworkStatus = EnrichmentStatus.Failed.name, lyricsStatus = EnrichmentStatus.Failed.name)
            }
            application.database.enrichmentDao().upsert(updated)
            if (updated.artworkUri != null || updated.candidateArtworkUri != null) artworkByAlbum[albumKey] = updated
            completed++
            setProgress(progressData(completed, tracks.size))
            setForeground(createForegroundInfo(completed, tracks.size))
        }
        return Result.success(progressData(completed, tracks.size))
    }

    private fun createForegroundInfo(completed: Int, total: Int): androidx.work.ForegroundInfo {
        val channelId = "enrichment"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(channelId, "歌词与封面补全", android.app.NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(com.yinqi.player.R.drawable.ic_music_notification)
            .setContentTitle("正在补全歌词与封面")
            .setContentText("已完成 $completed/$total")
            .setProgress(total.coerceAtLeast(1), completed, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.work.ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            androidx.work.ForegroundInfo(1001, notification)
        }
    }

    private fun progressData(completed: Int, total: Int): Data = Data.Builder()
        .putInt("completed", completed)
        .putInt("total", total)
        .build()
}

object EnrichmentScheduler {
    const val WORK_NAME = "library-enrichment-v1"

    fun schedule(context: Context, allowMetered: Boolean = false, replace: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<EnrichmentWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

object MusicBrainzRateLimiter {
    private val mutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun awaitPermit() {
        mutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestAt
            if (elapsed < 1_000L) kotlinx.coroutines.delay(1_000L - elapsed)
            lastRequestAt = System.currentTimeMillis()
        }
    }
}

private fun JSONArray.toListOfObjects(): List<JSONObject> = buildList {
    for (index in 0 until length()) optJSONObject(index)?.let(::add)
}

private fun java.io.InputStream.readLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        if (output.size() + read > limit) throw IOException("响应超过大小限制")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
