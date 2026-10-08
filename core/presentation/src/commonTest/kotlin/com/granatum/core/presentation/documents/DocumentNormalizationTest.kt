package com.granatum.core.presentation.documents

import com.granatum.core.presentation.documents.DocumentNormalization.Action
import com.granatum.core.presentation.documents.DocumentNormalization.MAX_BYTES
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentNormalizationTest {
    @Test
    fun supported_formats_under_the_cap_are_kept() {
        assertEquals(Action.KEEP, DocumentNormalization.decide("image/jpeg", 1_000))
        assertEquals(Action.KEEP, DocumentNormalization.decide("IMAGE/PNG", MAX_BYTES))
        assertEquals(Action.KEEP, DocumentNormalization.decide("image/webp", 1_000))
        assertEquals(Action.KEEP, DocumentNormalization.decide("application/pdf", MAX_BYTES))
    }

    @Test
    fun other_images_and_oversized_ones_become_jpeg() {
        assertEquals(Action.TO_JPEG, DocumentNormalization.decide("image/heic", 1_000))
        assertEquals(Action.TO_JPEG, DocumentNormalization.decide("image/heif", 1_000))
        assertEquals(Action.TO_JPEG, DocumentNormalization.decide("image/jpeg", MAX_BYTES + 1))
        assertEquals(Action.TO_JPEG, DocumentNormalization.decide("image/webp", 8L * 1024 * 1024))
    }

    @Test
    fun a_pdf_over_the_cap_cannot_be_fixed() {
        assertEquals(Action.REJECT, DocumentNormalization.decide("application/pdf", MAX_BYTES + 1))
    }

    @Test
    fun unknown_types_go_as_they_are_for_the_server_to_judge() {
        assertEquals(Action.KEEP, DocumentNormalization.decide(null, 10))
        assertEquals(Action.KEEP, DocumentNormalization.decide("text/plain", 10))
    }

    @Test
    fun the_name_matches_what_is_sent() {
        assertEquals("IMG_0001.jpg", DocumentNormalization.fileName("IMG_0001.HEIC", "image/jpeg", 0))
        assertEquals("factura.pdf", DocumentNormalization.fileName("factura.pdf", "application/pdf", 0))
        assertEquals("documento-3.jpg", DocumentNormalization.fileName(null, "image/jpeg", 2))
    }
}
