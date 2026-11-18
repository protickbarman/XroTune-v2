package com.xrotune.app.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
data class XroTuneProfileData(
    val displayName: String = "XroTune Listener",
    val bio: String = "Streaming & local playlists",
    val favoriteGenre: String = "All Genres",
    val avatarColorIndex: Int = 0,
    val totalTracksPlayed: Int = 0
)

@JsonClass(generateAdapter = true)
data class XroTuneSettingsData(
    val preferredContainer: String = "any",
    val preferredDownloadQuality: String = "high",
    val askDownloadQuality: Boolean = true,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0
)

@JsonClass(generateAdapter = true)
data class XroTuneDocument(
    val formatVersion: Int = 1,
    val appName: String = "XroTune",
    val updatedAt: Long = System.currentTimeMillis(),
    val profile: XroTuneProfileData = XroTuneProfileData(),
    val settings: XroTuneSettingsData = XroTuneSettingsData()
)

class XroTuneFileStorage(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(XroTuneDocument::class.java).indent("  ")

    val storageFile: File
        get() = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun loadOrCreate(): XroTuneDocument {
        val target = storageFile
        if (target.exists() && target.length() > 0L) {
            try {
                val raw = target.readText(Charsets.UTF_8)
                val parsed = adapter.fromJson(raw)
                if (parsed != null) {
                    return parsed
                }
            } catch (e: Exception) {
                Log.w("XroTuneFileStorage", "Failed to parse .xrotune file, recreating", e)
            }
        }
        val initial = XroTuneDocument()
        saveDocument(initial)
        return initial
    }

    @Synchronized
    fun saveDocument(document: XroTuneDocument): XroTuneDocument {
        val stamped = document.copy(updatedAt = System.currentTimeMillis())
        try {
            context.filesDir.mkdirs()
            val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
            tempFile.writeText(adapter.toJson(stamped), Charsets.UTF_8)
            if (!tempFile.renameTo(storageFile)) {
                storageFile.writeText(adapter.toJson(stamped), Charsets.UTF_8)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("XroTuneFileStorage", "Error writing .xrotune file", e)
        }
        return stamped
    }

    fun exportToUri(uri: Uri, document: XroTuneDocument): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(adapter.toJson(document).toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return Result.failure(Exception("Unable to open destination file"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importFromUri(uri: Uri): Result<XroTuneDocument> {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(Exception("Unable to read selected file"))
            val parsed = adapter.fromJson(raw)
                ?: return Result.failure(Exception("Invalid .xrotune file format"))
            val saved = saveDocument(parsed)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileSizeBytes(): Long {
        val f = storageFile
        return if (f.exists()) f.length() else 0L
    }

    companion object {
        const val FILE_NAME = ".xrotune"
    }
}
