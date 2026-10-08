package com.granatum.core.presentation.documents

/**
 * What to do with a picked file before uploading it. Servers that sniff the bytes usually take
 * JPEG, PNG, WebP and PDF only, with a size cap per file; anything else that is an image becomes a
 * JPEG, and an image over the cap is re-encoded smaller. WebP cannot be shrunk by the receiving
 * side, so a large one becomes a JPEG too.
 */
object DocumentNormalization {

    const val MAX_BYTES = 10L * 1024 * 1024
    const val MAX_WEBP_BYTES = 7L * 1024 * 1024
    const val MAX_IMAGE_SIDE = 3000
    const val JPEG_QUALITY = 85

    enum class Action { KEEP, TO_JPEG, REJECT }

    fun decide(mimeType: String?, size: Long): Action {
        val mime = mimeType?.lowercase().orEmpty()
        return when {
            mime == PDF -> if (size <= MAX_BYTES) Action.KEEP else Action.REJECT
            mime == JPEG || mime == PNG -> if (size <= MAX_BYTES) Action.KEEP else Action.TO_JPEG
            mime == WEBP -> if (size <= MAX_WEBP_BYTES) Action.KEEP else Action.TO_JPEG
            mime.startsWith("image/") -> Action.TO_JPEG
            // Unknown: let the server say why, file by file.
            else -> Action.KEEP
        }
    }

    /** The name to upload under, with the extension of what is actually sent. */
    fun fileName(original: String?, mimeType: String, index: Int): String {
        val base = original?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "documento-${index + 1}"
        val extension = when (mimeType) {
            JPEG -> "jpg"
            PNG -> "png"
            WEBP -> "webp"
            PDF -> "pdf"
            else -> original?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() } ?: "bin"
        }
        return "$base.$extension"
    }

    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    const val WEBP = "image/webp"
    const val PDF = "application/pdf"
}
