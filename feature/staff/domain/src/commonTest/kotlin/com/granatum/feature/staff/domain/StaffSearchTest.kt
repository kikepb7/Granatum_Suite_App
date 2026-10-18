package com.granatum.feature.staff.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaffSearchTest {
    private fun member(
        name: String,
        document: String = "12345678Z",
        position: String = "Florista",
        active: Boolean = true,
        lastName: String? = null,
    ) = StaffMember(name, name, lastName, lastName == null, document, position, ContractType.FULL_TIME, LocalDate(2026, 1, 1), active)

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

    @Test
    fun searches_and_sorts_by_the_full_name() {
        val people = listOf(member("Ana", lastName = "Zamora Gil"), member("Ana", lastName = "Álvarez Ruiz"), member("Bea"))
        assertEquals(listOf("Ana Álvarez Ruiz", "Ana Zamora Gil", "Bea"), people.search("", active = null).map { it.fullName })
        assertEquals(listOf("Ana Álvarez Ruiz"), people.search("alvarez", active = null).map { it.fullName })
    }

    @Test
    fun the_full_name_joins_name_and_last_names_or_keeps_the_name_alone() {
        assertEquals("Ana García López", member("Ana", lastName = " García López ").fullName)
        assertEquals("Luis", member("Luis").fullName)
        assertEquals("Luis", member("Luis", lastName = " ").fullName)
        assertTrue(member("Luis").lastNamePending)
    }
}
