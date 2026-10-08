package com.granatum.core.domain.validation

import com.granatum.core.domain.validation.PasswordRequirement.FALTA_DIGITO
import com.granatum.core.domain.validation.PasswordRequirement.FALTA_MAYUSCULA
import com.granatum.core.domain.validation.PasswordRequirement.FALTA_MINUSCULA
import com.granatum.core.domain.validation.PasswordRequirement.FALTA_SIMBOLO
import com.granatum.core.domain.validation.PasswordRequirement.LONGITUD_MAXIMA
import com.granatum.core.domain.validation.PasswordRequirement.LONGITUD_MINIMA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordValidatorTest {

    @Test
    fun a_password_meeting_every_rule_is_valid() {
        assertTrue(PasswordValidator.validate("Granatum1!").isEmpty())
    }

    @Test
    fun eight_characters_is_enough_and_seven_is_not() {
        assertTrue(LONGITUD_MINIMA !in PasswordValidator.validate("Abcde1!x"))
        assertTrue(LONGITUD_MINIMA in PasswordValidator.validate("Abcd1!x"))
    }

    @Test
    fun one_hundred_twenty_eight_characters_is_the_maximum() {
        val base = "Aa1!"
        assertTrue(LONGITUD_MAXIMA !in PasswordValidator.validate(base + "x".repeat(124)))
        assertTrue(LONGITUD_MAXIMA in PasswordValidator.validate(base + "x".repeat(125)))
    }

    @Test
    fun each_character_class_is_reported_on_its_own() {
        assertEquals(setOf(FALTA_MAYUSCULA), PasswordValidator.validate("granatum1!"))
        assertEquals(setOf(FALTA_MINUSCULA), PasswordValidator.validate("GRANATUM1!"))
        assertEquals(setOf(FALTA_DIGITO), PasswordValidator.validate("Granatum!!"))
        assertEquals(setOf(FALTA_SIMBOLO), PasswordValidator.validate("Granatum12"))
    }

    @Test
    fun whitespace_does_not_count_as_a_symbol() {
        assertTrue(FALTA_SIMBOLO in PasswordValidator.validate("Granatum 1"))
    }

    @Test
    fun an_empty_password_fails_every_rule_except_the_maximum() {
        assertEquals(
            setOf(LONGITUD_MINIMA, FALTA_MAYUSCULA, FALTA_MINUSCULA, FALTA_DIGITO, FALTA_SIMBOLO),
            PasswordValidator.validate("")
        )
    }
}
