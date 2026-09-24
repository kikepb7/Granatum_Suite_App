package com.granatum.core.presentation.mediapicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (PickedImage) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val parser = ContentUriParser(context = context)
            val mimeType = parser.getMimeType(uri = uri)
            scope.launch {
                val image = PickedImage(
                    bytes = parser.readUri(uri = uri) ?: return@launch,
                    mimeType = mimeType
                )
                onResult(image)
            }
        }
    }

    return remember {
        ImagePickerLauncher(
            onLaunch = {
                photoPickerLauncher.launch(
                    input = PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        )
    }
}
