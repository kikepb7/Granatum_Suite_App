package com.granatum.core.presentation.mediapicker

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(
    onResult: (PickedImage) -> Unit
): ImagePickerLauncher

class ImagePickerLauncher(
    private val onLaunch: () -> Unit
) {
    fun launch() = onLaunch()
}

class PickedImage(
    val bytes: ByteArray,
    val mimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PickedImage
        return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + (mimeType?.hashCode() ?: 0)
}
