package com.granatum.core.presentation.mediapicker

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentUriParser(
    private val context: Context
) {
    suspend fun readUri(uri: Uri): ByteArray? =
        withContext(context = Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        }

    private fun getMimeTypeFromExtension(uri: Uri): String? {
        val extension = uri.toString().substringAfterLast(delimiter = ".", missingDelimiterValue = "")
        return if (extension.isNotBlank()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) else null
    }

    fun getMimeType(uri: Uri): String? =
        context.contentResolver.getType(uri) ?: getMimeTypeFromExtension(uri = uri)
}
