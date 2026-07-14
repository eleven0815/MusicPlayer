package com.yinqi.player.data

data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

data class LyricsDocument(
    val lines: List<LyricLine>,
    val plainText: String,
) {
    val isSynced: Boolean
        get() = lines.isNotEmpty()

    fun currentLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        return lines.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
    }
}

fun parseLrc(raw: String): LyricsDocument {
    val lines = mutableListOf<LyricLine>()
    var offsetMs = 0L
    val timestampPattern = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    raw.lineSequence().forEach { sourceLine ->
        val offsetMatch = Regex("""\[offset:([+-]?\d+)\]""", RegexOption.IGNORE_CASE).find(sourceLine)
        if (offsetMatch != null) {
            offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
            return@forEach
        }
        val matches = timestampPattern.findAll(sourceLine).toList()
        if (matches.isEmpty()) return@forEach
        val text = sourceLine.replace(timestampPattern, "").trim()
        matches.forEach { match ->
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                1 -> fraction.toLong() * 100L
                2 -> fraction.toLong() * 10L
                3 -> fraction.toLong()
                else -> 0L
            }
            lines += LyricLine(
                timestampMs = (minutes * 60_000L + seconds * 1_000L + fractionMs + offsetMs).coerceAtLeast(0L),
                text = text,
            )
        }
    }

    val parsedLines = lines.sortedWith(compareBy<LyricLine> { it.timestampMs }.thenBy { it.text })
    val plainText = if (parsedLines.isNotEmpty()) {
        parsedLines.joinToString("\n") { it.text }.trim()
    } else {
        raw.lineSequence()
            .filterNot { it.trimStart().startsWith("[") }
            .joinToString("\n")
            .trim()
    }
    return LyricsDocument(parsedLines, plainText)
}
