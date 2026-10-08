package com.granatum.core.presentation.documents

import androidx.compose.runtime.Composable

/** A file the app has in memory (a downloaded original, a report) and wants to hand to the system. */
class SharedFile(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

class FileOpener(
    private val onOpen: (SharedFile) -> Unit,
    private val onShare: (SharedFile) -> Unit,
) {
    /** Shows the file with the system viewer (a PDF reader, for instance). */
    fun open(file: SharedFile) = onOpen(file)

    /** Offers the system share sheet: save to files, send by mail, and so on. */
    fun share(file: SharedFile) = onShare(file)
}

/** The file is written to the app's cache only, never to shared storage. */
@Composable
expect fun rememberFileOpener(): FileOpener
