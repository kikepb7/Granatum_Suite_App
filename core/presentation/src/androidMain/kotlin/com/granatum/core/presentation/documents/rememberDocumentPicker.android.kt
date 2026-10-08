package com.granatum.core.presentation.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PICKED = 20

@Composable
actual fun rememberDocumentPicker(onResult: (List<PickedDocument>) -> Unit): DocumentPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onResult)
    // Survives the activity being recreated while the camera app is in front.
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    fun read(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val documents = withContext(Dispatchers.IO) { uris.mapIndexedNotNull { i, uri -> context.readDocument(uri, i) } }
            if (documents.isNotEmpty()) deliver(documents)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKED)) { read(it) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { read(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val photo = pendingPhoto
        pendingPhoto = null
        if (taken && photo != null) read(listOf(Uri.parse(photo)))
    }

    return remember {
        DocumentPicker { source ->
            when (source) {
                DocumentSource.GALLERY -> gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                DocumentSource.FILES -> files.launch(arrayOf(DocumentNormalization.PDF, "image/*"))
                DocumentSource.CAMERA -> {
                    val file = context.cacheFile("camera", "foto-${System.currentTimeMillis()}.jpg")
                    val uri = context.uriFor(file)
                    pendingPhoto = uri.toString()
                    camera.launch(uri)
                }
            }
        }
    }
}
