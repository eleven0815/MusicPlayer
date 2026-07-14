package com.yinqi.player.data

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\([^)]*(feat|ft\\.?|remaster|live|version)[^)]*\\)"), "")
        .replace(Regex("\\[[^]]*(feat|ft\\.?|remaster|live|version)[^]]*\\]"), "")
        .filterNot { it.isWhitespace() || it in "-_,.!?·:;()/[]{}'\"" }

    fun normalizeFileStem(value: String): String = normalize(value)

    fun normalizeLyricsKey(value: String): String = normalize(value)
}
