/*
 * BeatBox
 *
 * Bu dosya, GPL-3.0 lisansı altında yayımlanan Metrolist projesinden
 * (https://github.com/MetrolistGroup/Metrolist) uyarlanmıştır.
 * Uyarlanmış bu dosya da GPL-3.0 koşulları altında dağıtılır.
 *
 * This file is adapted from the Metrolist project, licensed under GPL-3.0.
 * This adapted file is distributed under the terms of GPL-3.0.
 */
package com.blackstack.beatbox.library
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Utility object for reading local LRC files from the filesystem
 *
 * Handles reading LRC files that are stored alongside audio files,
 * with the same base filename but .lrc extension.
 *
 * Scoped storage (Android 10+) uyumu:
 * - `content://` ses URI'si için önce MediaStore.Files üzerinden kardeş
 *   `.lrc` satırı aranır ve ContentResolver ile okunur.
 * - Doğrudan dosya yolu yalnızca Android 9 ve öncesinde veya kullanıcı
 *   "Tüm dosyalara erişim" iznini verdiyse denenir.
 * - Son çare olarak aynı sağlayıcı üzerinden uzantısı değiştirilmiş URI
 *   denenir; sağlayıcı desteklemiyorsa null döner.
 */
object LocalFileReader {
    /**
     * Reads an LRC file from the same directory as the audio file
     *
     * @param audioUri The URI of the audio file
     * @return The LRC file content as a string, or null if the file doesn't exist or can't be read
     */
    suspend fun readLrcFile(context: Context, audioUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            when (audioUri.scheme) {
                "file" -> {
                    val path = audioUri.path ?: return@withContext null
                    val lrcPath = constructLrcPath(path)
                    val lrcFile = File(lrcPath)
                    if (lrcFile.exists() && lrcFile.canRead()) lrcFile.readText() else null
                }
                "content" -> {
                    readLrcFromContentUri(context, audioUri)
                }
                else -> null
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /**
     * Reads a sibling `.lrc` file for a `content://` audio URI.
     *
     * Sırasıyla:
     * 1. MediaStore.Files üzerinden RELATIVE_PATH + DISPLAY_NAME eşleşmesi (Q+)
     * 2. Tüm dosyalara erişim varsa DATA sütunundaki gerçek yol
     * 3. Sağlayıcının uzantı değiştirmeyi desteklediği son çare URI denemesi
     */
    private suspend fun readLrcFromContentUri(context: Context, audioUri: Uri): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            readLrcViaMediaStore(context, audioUri)?.let { return it }
        }

        if (canUseDirectFileAccess()) {
            queryDataPath(context, audioUri)?.let { audioPath ->
                val lrcFile = File(constructLrcPath(audioPath))
                if (lrcFile.exists() && lrcFile.canRead()) return lrcFile.readText()
            }
        }

        val lrcUri = constructLrcUri(audioUri) ?: return null
        return runCatching {
            context.contentResolver.openInputStream(lrcUri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull()
    }

    /**
     * Android 10+ için scoped storage dostu okuma: ses dosyasının
     * RELATIVE_PATH ve DISPLAY_NAME değerlerini alır, aynı klasördeki
     * `.lrc` dosyasını MediaStore.Files içinde arar.
     */
    private fun readLrcViaMediaStore(context: Context, audioUri: Uri): String? {
        val audioProjection = arrayOf(
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val location = runCatching {
            context.contentResolver.query(audioUri, audioProjection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val relativeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                if (relativeIndex < 0 || nameIndex < 0) return@use null
                val relativePath = cursor.getString(relativeIndex)
                val displayName = cursor.getString(nameIndex)
                if (relativePath.isNullOrBlank() || displayName.isNullOrBlank()) {
                    null
                } else {
                    relativePath to displayName
                }
            }
        }.getOrNull() ?: return null

        val (relativePath, displayName) = location
        val lrcName = displayName.substringBeforeLast('.', displayName) + ".lrc"
        val filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"

        return runCatching {
            context.contentResolver.query(
                filesUri,
                arrayOf(MediaStore.Files.FileColumns._ID),
                selection,
                arrayOf(relativePath, lrcName),
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val lrcUri = ContentUris.withAppendedId(filesUri, cursor.getLong(0))
                context.contentResolver.openInputStream(lrcUri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
            }
        }.getOrNull()
    }

    /** Android 9 ve öncesinde veya tüm dosyalara erişim verildiyse doğrudan yol kullanılabilir. */
    private fun canUseDirectFileAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())

    @Suppress("DEPRECATION")
    private fun queryDataPath(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.DATA),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    /**
     * Constructs a content:// URI for the LRC file by replacing the audio URI's
     * last path segment extension with .lrc.
     */
    private fun constructLrcUri(audioUri: Uri): Uri? {
        val path = audioUri.path ?: return null
        val lastDotIndex = path.lastIndexOf('.')
        if (lastDotIndex <= 0) return null

        val lrcPath = path.substring(0, lastDotIndex) + ".lrc"
        return audioUri.buildUpon().path(lrcPath).build()
    }

    private fun constructLrcPath(audioPath: String): String {
        val lastDotIndex = audioPath.lastIndexOf('.')

        return if (lastDotIndex > 0) {
            // Replace extension with .lrc
            audioPath.substring(0, lastDotIndex) + ".lrc"
        } else {
            // No extension found, just append .lrc
            "$audioPath.lrc"
        }
    }
}
