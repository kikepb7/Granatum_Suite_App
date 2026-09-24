@file:OptIn(ExperimentalForeignApi::class)

package com.granatum.core.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.posix.memcpy

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (PickedImage) -> Unit
): ImagePickerLauncher {
    val scope = rememberCoroutineScope()

    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(flag = true, completion = null)

                val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                val dispatchGroup = dispatch_group_create()
                val imageDataList = mutableListOf<PickedImage>()

                for (result in results) {
                    dispatch_group_enter(dispatchGroup)

                    val itemProvider = result.itemProvider
                    val typeIdentifiers = itemProvider.registeredTypeIdentifiers
                    val primaryType = typeIdentifiers.firstOrNull() as? String

                    if (primaryType == null) {
                        dispatch_group_leave(dispatchGroup)
                        continue
                    }

                    val mimeType = UTType.typeWithIdentifier(identifier = primaryType)
                        ?.preferredMIMEType

                    if (mimeType == null) {
                        dispatch_group_leave(dispatchGroup)
                        continue
                    }

                    itemProvider.loadDataRepresentationForTypeIdentifier(
                        typeIdentifier = primaryType
                    ) { nsData, _ ->
                        scope.launch {
                            nsData?.let {
                                val bytes = ByteArray(size = it.length.toInt())
                                withContext(context = Dispatchers.Default) {
                                    memcpy(bytes.refTo(index = 0), it.bytes, it.length)
                                }
                                imageDataList.add(PickedImage(bytes = bytes, mimeType = mimeType))
                            }
                            dispatch_group_leave(dispatchGroup)
                        }
                    }

                    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                        scope.launch {
                            imageDataList.firstOrNull()?.let { image ->
                                onResult(image)
                            }
                        }
                    }
                }
            }
        }
    }

    return remember {
        val pickerViewController = PHPickerViewController(
            configuration = PHPickerConfiguration().apply {
                setSelectionLimit(1)
                setFilter(filter = PHPickerFilter.imagesFilter)
                setSelection(PHPickerConfigurationSelectionOrdered)
            }
        )
        pickerViewController.delegate = delegate

        ImagePickerLauncher(
            onLaunch = {
                UIApplication.sharedApplication.keyWindow
                    ?.rootViewController
                    ?.presentViewController(
                        viewControllerToPresent = pickerViewController,
                        animated = true,
                        completion = null
                    )
            }
        )
    }
}
