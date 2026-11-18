package com.xrotune.app.ui.util

import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.model.MusicItem
import com.xrotune.app.data.model.SearchResultItem

fun formatDurationMs(ms: Long): String {
    val totalSeconds = maxOf(0L, ms) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatDurationSeconds(seconds: Int): String {
    return formatDurationMs(seconds.toLong() * 1000L)
}

fun parseDurationTextToSeconds(durationText: String?): Int {
    val clean = durationText?.trim()?.takeIf { it.isNotBlank() } ?: return 0
    val parts = clean.split(":").mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0
    }
}

fun formatByteSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

fun formatBitrate(bps: Long?): String {
    if (bps == null || bps <= 0L) return ""
    return "${bps / 1000L} kbps"
}

fun formatCompactCount(count: Long?): String {
    if (count == null || count <= 0L) return ""
    return when {
        count >= 1_000_000_000L -> "%.1fB".format(count / 1_000_000_000.0)
        count >= 1_000_000L -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000L -> "%.1fK".format(count / 1_000.0)
        else -> count.toString()
    }
}

/**
 * Generates an ordered list of artwork candidates prioritizing clean un-letterboxed 16:9 HD
 * artwork before falling back to the raw API thumbnail or standard quality URL.
 */
fun buildArtworkCandidates(videoId: String?, rawThumbnail: String?): List<String> {
    val candidates = mutableListOf<String>()
    val cleanId = videoId?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("http") }
    if (cleanId != null) {
        candidates.add("https://i.ytimg.com/vi/$cleanId/maxresdefault.jpg")
        candidates.add("https://i.ytimg.com/vi/$cleanId/hq720.jpg")
        candidates.add("https://i.ytimg.com/vi/$cleanId/sddefault.jpg")
    }
    rawThumbnail?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
        candidates.add(raw)
    }
    if (cleanId != null) {
        candidates.add("https://i.ytimg.com/vi/$cleanId/hqdefault.jpg")
    }
    return candidates.distinct()
}

fun SongEntity.toSearchResultItem(): SearchResultItem = SearchResultItem(
    kind = "video",
    videoId = videoId,
    title = title,
    channel = author,
    lengthSeconds = durationSeconds,
    lengthText = if (durationSeconds > 0) formatDurationSeconds(durationSeconds) else null,
    thumbnail = thumbnail,
    audioUrl = audioUrl
)

fun MusicItem.toSearchResultItem(): SearchResultItem {
    val cleanVideoId = videoId?.trim()?.takeIf { it.isNotBlank() }
    val resolvedKind = when (kind?.lowercase()) {
        "artist" -> "channel"
        "album", "playlist" -> "playlist"
        else -> "video"
    }
    val secs = durationSeconds ?: parseDurationTextToSeconds(duration)
    return SearchResultItem(
        kind = resolvedKind,
        videoId = cleanVideoId,
        title = title,
        channel = subtitle,
        channelId = if (kind.equals("artist", ignoreCase = true)) browseId else null,
        lengthSeconds = secs,
        lengthText = duration ?: if (secs > 0) formatDurationSeconds(secs) else null,
        thumbnail = thumbnail ?: cleanVideoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" },
        playlistId = playlistId ?: if (kind.equals("playlist", ignoreCase = true) || kind.equals("album", ignoreCase = true)) browseId else null,
        browseId = browseId
    )
}
