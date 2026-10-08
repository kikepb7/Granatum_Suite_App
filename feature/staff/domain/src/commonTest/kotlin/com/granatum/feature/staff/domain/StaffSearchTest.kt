package com.granatum.feature.staff.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StaffSearchTest {
    private fun member(
        name: String,
        document: String = "12345678Z",
        position: String = "Florista",
        active: Boolean = true,
    ) = StaffMember(name, name, document, position, ContractType.FULL_TIME, LocalDate(2026, 1, 1), active)

    private val staff =
        listOf(member("Óscar Ruiz"), member("ana Pérez", position = "Repartidora"), member("Luis", document = "X1234567L", active = false))

    @Test
    fun searches_name_document_and_position_without_accents() {
        assertEquals(listOf("Óscar Ruiz"), staff.search("oscar", active = null).map { it.name })
        assertEquals(listOf("ana Pérez"), staff.search("REPARTI", active = null).map { it.name })
        assertEquals(listOf("Luis"), staff.search("x1234", active = null).map { it.name })
    }

    @Test
    fun filters_by_active_and_sorts_by_name() {
        assertEquals(listOf("ana Pérez", "Óscar Ruiz"), staff.search("", active = true).map { it.name })
        assertEquals(listOf("Luis"), staff.search("", active = false).map { it.name })
    }
}
