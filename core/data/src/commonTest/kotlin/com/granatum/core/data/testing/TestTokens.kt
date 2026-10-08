package com.granatum.core.data.testing

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Builds JWT-shaped strings for tests. The signature is junk: the app never verifies it. */
@OptIn(ExperimentalEncodingApi::class)
object TestTokens {
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun jwt(payloadJson: String): String {
        val header = base64Url.encode("""{"alg":"HS256","typ":"JWT"}""".encodeToByteArray())
        val payload = base64Url.encode(payloadJson.encodeToByteArray())
        return "$header.$payload.signature"
    }

    fun access(
        sub: String = "emp-1",
        role: String = "EMPLEADO",
        pwdChange: Boolean = false,
        marker: String = ""
    ): String = jwt(
        buildString {
            append("""{"sub":"$sub","role":"$role","type":"access"""")
            if (pwdChange) append(""","pwd_change":true""")
            if (marker.isNotEmpty()) append(""","jti":"$marker"""")
            append("}")
        }
    )
}
