package com.granatum.core.presentation.mapper

import com.granatum.core.domain.util.DataError
import com.granatum.core.presentation.util.UiText
import granatumsuite.core.presentation.generated.resources.error_bad_request
import granatumsuite.core.presentation.generated.resources.error_conflict
import granatumsuite.core.presentation.generated.resources.Res.string as RString
import granatumsuite.core.presentation.generated.resources.error_disk_full
import granatumsuite.core.presentation.generated.resources.error_forbidden
import granatumsuite.core.presentation.generated.resources.error_no_internet
import granatumsuite.core.presentation.generated.resources.error_not_found
import granatumsuite.core.presentation.generated.resources.error_payload_too_large
import granatumsuite.core.presentation.generated.resources.error_request_timeout
import granatumsuite.core.presentation.generated.resources.error_serialization
import granatumsuite.core.presentation.generated.resources.error_server
import granatumsuite.core.presentation.generated.resources.error_service_unavailable
import granatumsuite.core.presentation.generated.resources.error_too_many_requests
import granatumsuite.core.presentation.generated.resources.error_unable_to_send_message
import granatumsuite.core.presentation.generated.resources.error_unauthorized
import granatumsuite.core.presentation.generated.resources.error_unknown

fun DataError.toUiText(): UiText {
    val resource = when (this) {
        DataError.Local.DISK_FULL -> RString.error_disk_full
        DataError.Local.NOT_FOUND -> RString.error_not_found
        DataError.Local.UNKNOWN -> RString.error_unknown
        DataError.Remote.BAD_REQUEST -> RString.error_bad_request
        DataError.Remote.REQUEST_TIMEOUT -> RString.error_request_timeout
        DataError.Remote.UNAUTHORIZED -> RString.error_unauthorized
        DataError.Remote.FORBIDDEN -> RString.error_forbidden
        DataError.Remote.NOT_FOUND -> RString.error_not_found
        DataError.Remote.CONFLICT -> RString.error_conflict
        DataError.Remote.TOO_MANY_REQUESTS -> RString.error_too_many_requests
        DataError.Remote.NO_INTERNET -> RString.error_no_internet
        DataError.Remote.PAYLOAD_TOO_LARGE -> RString.error_payload_too_large
        DataError.Remote.SERVER_ERROR -> RString.error_server
        DataError.Remote.SERVICE_UNAVAILABLE -> RString.error_service_unavailable
        DataError.Remote.SERIALIZATION -> RString.error_serialization
        DataError.Remote.UNKNOWN -> RString.error_unknown
        DataError.ConnectionModel.MESSAGE_SEND_FAILED -> RString.error_no_internet
        DataError.ConnectionModel.NOT_CONNECTED -> RString.error_unable_to_send_message
    }

    return UiText.Resource(id = resource)
}