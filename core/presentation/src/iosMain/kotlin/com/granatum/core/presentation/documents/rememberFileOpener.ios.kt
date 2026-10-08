package com.granatum.core.presentation.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject

@Composable
actual fun rememberFileOpener(): FileOpener {
    // Held here because UIKit keeps the interaction controller's delegate weakly.
    val previewDelegate = remember {
        object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController =
                topViewController() ?: UIViewController()
        }
    }
    val holder = remember { arrayOfNulls<UIDocumentInteractionController>(1) }
    return remember {
        FileOpener(
            onOpen = { file ->
                val url = file.writeToTemporary() ?: return@FileOpener
                val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
                controller.delegate = previewDelegate
                holder[0] = controller
                if (!controller.presentPreviewAnimated(true)) share(file)
            },
            onShare = { file -> share(file) }
        )
    }
}

private fun share(file: SharedFile) {
    val url = file.writeToTemporary() ?: return
    val presenter = topViewController() ?: return
    val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    // iPad presents it as a popover anchored to the presenter's view.
    sheet.popoverPresentationController?.sourceView = presenter.view
    presenter.presentViewController(sheet, animated = true, completion = null)
}
