package com.granatum.core.presentation.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileOpener(): FileOpener {
    val context = LocalContext.current
    return remember(context) {
        FileOpener(
            onOpen = { file ->
                val uri = context.write(file)
                val view = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, file.mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(view)
                } catch (_: ActivityNotFoundException) {
                    // No viewer for this type: let the person choose where it goes instead.
                    context.share(file)
                }
            },
            onShare = { file -> context.share(file) }
        )
    }
}

private fun Context.write(file: SharedFile) = uriFor(cacheFile("shared", file.fileName).apply { writeBytes(file.bytes) })

private fun Context.share(file: SharedFile) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(file.mimeType)
        .putExtra(Intent.EXTRA_STREAM, write(file))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
