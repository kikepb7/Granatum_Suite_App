package com.granatum.core.presentation.documents

import androidx.compose.runtime.Composable

/** Where the person takes the documents from. */
enum class DocumentSource { CAMERA, GALLERY, FILES }

/**
 * A document ready to upload: a JPEG, PNG, WebP or PDF, already converted when the original was
 * in a format servers rarely take (HEIC from an iPhone, for instance). See [DocumentNormalization].
 */
class PickedDocument(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

class DocumentPicker(
    private val onLaunch: (DocumentSource) -> Unit,
) {
    fun launch(source: DocumentSource) = onLaunch(source)
}

/**
 * Takes a photo or picks images or PDFs (several at once where the platform allows it) and hands
 * back what was chosen, normalised for upload. Cancelling delivers nothing.
 */
@Composable
expect fun rememberDocumentPicker(onResult: (List<PickedDocument>) -> Unit): DocumentPicker
