package com.granatum.core.data.demo

/** Small helpers the demo backend needs to read uploads and produce files. */
internal object DemoDocuments {
    class Part(
        val contentType: String?,
        val bytes: ByteArray,
    )

    /** The parts of a `multipart/form-data` body, in order. */
    fun multipart(
        body: ByteArray,
        contentType: String?,
    ): List<Part> {
        val boundary = contentType?.substringAfter("boundary=", "")?.trim('"')?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val delimiter = "--$boundary".encodeToByteArray()
        val separator = "\r\n\r\n".encodeToByteArray()
        val parts = mutableListOf<Part>()
        var start = indexOf(body, delimiter, 0)
        while (start >= 0) {
            val headersStart = start + delimiter.size + 2
            val next = indexOf(body, delimiter, headersStart)
            if (next < 0 || headersStart >= body.size) break
            val headersEnd = indexOf(body, separator, headersStart)
            if (headersEnd in 0 until next) {
                val headers = body.copyOfRange(headersStart, headersEnd).decodeToString()
                val type =
                    headers
                        .lineSequence()
                        .firstOrNull {
                            it.startsWith(
                                "Content-Type",
                                ignoreCase = true,
                            )
                        }?.substringAfter(':')
                        ?.trim()
                parts += Part(type, body.copyOfRange(headersEnd + separator.size, (next - 2).coerceAtLeast(headersEnd + separator.size)))
            }
            start = next
        }
        return parts
    }

    private fun indexOf(
        data: ByteArray,
        pattern: ByteArray,
        from: Int,
    ): Int {
        outer@ for (i in from..data.size - pattern.size) {
            for (j in pattern.indices) if (data[i + j] != pattern[j]) continue@outer
            return i
        }
        return -1
    }

    /** By magic bytes only, like the server: never by name or declared type. */
    fun sniff(bytes: ByteArray): String? =
        when {
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
            bytes.size >= 4 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'N'.code.toByte() &&
                bytes[3] == 'G'.code.toByte() -> "image/png"
            bytes.size >= 12 &&
                bytes
                    .copyOfRange(
                        0,
                        4,
                    ).decodeToString() == "RIFF" &&
                bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> "image/webp"
            bytes.size >= 4 && bytes.copyOfRange(0, 4).decodeToString() == "%PDF" -> "application/pdf"
            else -> null
        }

    fun extension(mime: String) =
        when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "pdf"
        }

    /** A one-page A4 PDF with [lines] of Helvetica text: enough for originals and reports. */
    fun pdf(lines: List<String>): ByteArray {
        fun escape(text: String) =
            text
                .map { c ->
                    if (c.code >
                        126
                    ) {
                        '?'
                    } else {
                        c
                    }
                }.joinToString("")
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
        val content =
            buildString {
                append("BT /F1 12 Tf 56 780 Td 16 TL\n")
                lines.forEach { append("(").append(escape(it)).append(") Tj T*\n") }
                append("ET")
            }
        val objects =
            listOf(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length ${content.length} >>\nstream\n$content\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            )
        val out = StringBuilder("%PDF-1.4\n")
        val offsets =
            objects.mapIndexed { index, body ->
                val offset = out.length
                out.append("${index + 1} 0 obj\n$body\nendobj\n")
                offset
            }
        val xref = out.length
        out.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { out.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF")
        return out.toString().encodeToByteArray()
    }
}
