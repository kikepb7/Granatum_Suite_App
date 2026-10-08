@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.granatum.core.presentation.documents

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.posix.memcpy
import kotlin.math.max

internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

internal fun ByteArray.toNSData(): NSData =
    usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.toULong())
    }

/** The view controller on top, to present pickers and sheets from. */
internal fun topViewController(): UIViewController? {
    val window =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

/** Any image UIKit can read (HEIC included), scaled to fit, as JPEG. */
internal fun UIImage.toJpeg(): ByteArray? {
    val maxSide = DocumentNormalization.MAX_IMAGE_SIDE.toDouble()
    val (width, height) = size.useContents { width to height }
    val longest = max(width, height)
    val image =
        if (longest > maxSide) {
            val scale = maxSide / longest
            val target = CGSizeMake(width * scale, height * scale)
            UIGraphicsBeginImageContextWithOptions(target, false, 1.0)
            drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
            val scaled = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            scaled ?: this
        } else {
            this
        }
    return UIImageJPEGRepresentation(image, DocumentNormalization.JPEG_QUALITY / 100.0)?.toByteArray()
}

/** Normalises picked bytes for upload; null when they cannot be read or fixed. */
internal fun normalise(
    data: ByteArray,
    mimeType: String?,
    originalName: String?,
    index: Int,
): PickedDocument? =
    when (DocumentNormalization.decide(mimeType, data.size.toLong())) {
        DocumentNormalization.Action.KEEP -> {
            val sent = mimeType ?: "application/octet-stream"
            PickedDocument(data, DocumentNormalization.fileName(originalName, sent, index), sent)
        }
        DocumentNormalization.Action.TO_JPEG ->
            UIImage.imageWithData(data.toNSData())?.toJpeg()?.let {
                PickedDocument(
                    it,
                    DocumentNormalization.fileName(originalName, DocumentNormalization.JPEG, index),
                    DocumentNormalization.JPEG,
                )
            }
        DocumentNormalization.Action.REJECT -> {
            val sent = mimeType ?: "application/octet-stream"
            PickedDocument(data, DocumentNormalization.fileName(originalName, sent, index), sent)
        }
    }

/** Writes [file] to the app's temporary folder and returns its URL. */
internal fun SharedFile.writeToTemporary(): NSURL? {
    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    return if (bytes.toNSData().writeToURL(url, atomically = true)) url else null
}
