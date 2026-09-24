package com.granatum.core.presentation.permissions

import com.granatum.core.presentation.permissions.Permission.NOTIFICATIONS
import com.granatum.core.presentation.permissions.PermissionState.DENIED
import com.granatum.core.presentation.permissions.PermissionState.GRANTED
import com.granatum.core.presentation.permissions.PermissionState.PERMANENTLY_DENIED
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

actual class PermissionController(
    private val mokoPermissionsController: PermissionsController
) {
    actual suspend fun requestPermission(permission: Permission): PermissionState {
        return try {
            mokoPermissionsController.providePermission(permission = permission.toMokoPermission())
            GRANTED
        } catch (_: DeniedAlwaysException) {
            PERMANENTLY_DENIED

        } catch (_: DeniedException) {
            DENIED

        } catch (_: RequestCanceledException) {
            DENIED
        }
    }
}

fun Permission.toMokoPermission(): dev.icerock.moko.permissions.Permission =
    when (this) {
        NOTIFICATIONS -> dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION
    }