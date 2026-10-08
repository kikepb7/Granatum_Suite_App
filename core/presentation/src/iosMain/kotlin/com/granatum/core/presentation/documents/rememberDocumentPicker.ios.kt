@file:OptIn(ExperimentalForeignApi::class)

package com.granatum.core.presentation.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.conformsToType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeJPEG
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePNG
import platform.UniformTypeIdentifiers.UTTypeWebP
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify

private const val MAX_PICKED = 20L

/** The types sent as they are; anything else that is an image is converted to JPEG. */
private val PASS_THROUGH = listOf(UTTypeJPEG, UTTypePNG, UTTypeWebP)

@Composable
actual fun rememberDocumentPicker(onResult: (List<PickedDocument>) -> Unit): DocumentPicker {
    val deliver by rememberUpdatedState(onResult)
    val handlers = remember { PickerHandlers { documents -> if (documents.isNotEmpty()) deliver(documents) } }
    return remember {
        DocumentPicker { source ->
            val presenter = topViewController() ?: return@DocumentPicker
            val controller = when (source) {
                DocumentSource.CAMERA -> {
                    if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                        return@DocumentPicker
                    }
                    UIImagePickerController().apply {
                        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                        delegate = handlers.camera
                    }
                }
                DocumentSource.GALLERY -> PHPickerViewController(
                    configuration = PHPickerConfiguration().apply {
                        setSelectionLimit(MAX_PICKED)
                        setFilter(PHPickerFilter.imagesFilter)
                    }
                ).apply { delegate = handlers.gallery }
                DocumentSource.FILES -> UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypePDF, UTTypeImage),
                    asCopy = true
                ).apply {
                    allowsMultipleSelection = true
                    delegate = handlers.files
                }
            }
            presenter.presentViewController(controller, animated = true, completion = null)
        }
    }
}

/** UIKit holds its delegates weakly: this keeps them alive for as long as the composable is. */
private class PickerHandlers(private val deliver: (List<PickedDocument>) -> Unit) {

    private fun onMain(documents: List<PickedDocument>) = dispatch_async(dispatch_get_main_queue()) { deliver(documents) }

    val camera = CameraDelegate(::onMain)
    val gallery = GalleryDelegate(deliver)
    val files = FilesDelegate(::onMain)
}

private class CameraDelegate(private val onMain: (List<PickedDocument>) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
            picker.dismissViewControllerAnimated(true, completion = null)
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
            val jpeg = image.toJpeg() ?: return
            onMain(listOf(PickedDocument(jpeg, DocumentNormalization.fileName(null, DocumentNormalization.JPEG, 0).replace("documento", "foto"), DocumentNormalization.JPEG)))
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, completion = null)
        }
}

private class GalleryDelegate(private val deliver: (List<PickedDocument>) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, completion = null)
            val results = didFinishPicking.filterIsInstance<PHPickerResult>()
            if (results.isEmpty()) return
            val picked = arrayOfNulls<PickedDocument>(results.size)
            val group = dispatch_group_create()
            results.forEachIndexed { index, result ->
                val provider = result.itemProvider
                val identifiers = provider.registeredTypeIdentifiers.filterIsInstance<String>()
                val types = identifiers.mapNotNull { UTType.typeWithIdentifier(it) }
                val direct = types.firstOrNull { type -> PASS_THROUGH.any { type.conformsToType(it) } }
                val chosen = direct ?: types.firstOrNull { it.conformsToType(UTTypeImage) } ?: return@forEachIndexed
                dispatch_group_enter(group)
                provider.loadDataRepresentationForTypeIdentifier(chosen.identifier) { data, _ ->
                    if (data != null) {
                        picked[index] = normalise(data.toByteArray(), chosen.preferredMIMEType, provider.suggestedName, index)
                    }
                    dispatch_group_leave(group)
                }
            }
            dispatch_group_notify(group, dispatch_get_main_queue()) { deliver(picked.filterNotNull()) }
        }
}

private class FilesDelegate(private val onMain: (List<PickedDocument>) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val documents = didPickDocumentsAtURLs.filterIsInstance<NSURL>().mapIndexedNotNull { index, url ->
                val data = NSData.dataWithContentsOfURL(url) ?: return@mapIndexedNotNull null
                val type = url.pathExtension?.let { UTType.typeWithFilenameExtension(it) }
                normalise(data.toByteArray(), type?.preferredMIMEType, url.lastPathComponent, index)
            }
            onMain(documents)
        }
    }
