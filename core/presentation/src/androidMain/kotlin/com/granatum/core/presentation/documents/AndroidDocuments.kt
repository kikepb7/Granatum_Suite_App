package com.granatum.core.presentation.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

internal fun Context.documentsAuthority() = "$packageName.documents"

internal fun Context.cacheFile(
    folder: String,
    name: String,
): File = File(File(cacheDir, folder).apply { mkdirs() }, name)

internal fun Context.uriFor(file: File): Uri = FileProvider.getUriForFile(this, documentsAuthority(), file)

/** Reads a picked URI and normalises it for upload; null when it cannot be read or fixed. */
internal fun Context.readDocument(
    uri: Uri,
    index: Int,
): PickedDocument? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mime = contentResolver.getType(uri) ?: guessMime(bytes)
    val name = displayName(uri)
    return when (DocumentNormalization.decide(mime, bytes.size.toLong())) {
        DocumentNormalization.Action.KEEP -> {
            val sent = mime ?: "application/octet-stream"
            PickedDocument(bytes, DocumentNormalization.fileName(name, sent, index), sent)
        }
        DocumentNormalization.Action.TO_JPEG ->
            toJpeg(bytes)?.let {
                PickedDocument(it, DocumentNormalization.fileName(name, DocumentNormalization.JPEG, index), DocumentNormalization.JPEG)
            }
        // Too big to fix on the device: sent anyway so the server reports it with the others.
        DocumentNormalization.Action.REJECT ->
            PickedDocument(bytes, DocumentNormalization.fileName(name, mime ?: "", index), mime ?: "application/octet-stream")
    }
}

private fun Context.displayName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment

private fun guessMime(bytes: ByteArray): String? =
    when {
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> DocumentNormalization.JPEG
        bytes.size > 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() -> DocumentNormalization.PDF
        else -> null
    }

/** Decodes any image Android can read (HEIC included from API 28), scaled to fit, as JPEG. */
internal fun toJpeg(bytes: ByteArray): ByteArray? =
    runCatching {
        val maxSide = DocumentNormalization.MAX_IMAGE_SIDE
        val bitmap: Bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                    val longest = max(info.size.width, info.size.height)
                    if (longest > maxSide) {
                        val scale = maxSide.toFloat() / longest
                        decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: return null
            }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, DocumentNormalization.JPEG_QUALITY, out)
            out.toByteArray()
        }
    }.getOrNull()
