package com.granatum.core.domain.validation

import com.granatum.core.domain.validation.PasswordRequirement.MISSING_DIGIT
import com.granatum.core.domain.validation.PasswordRequirement.MISSING_UPPERCASE
import com.granatum.core.domain.validation.PasswordRequirement.MISSING_LOWERCASE
import com.granatum.core.domain.validation.PasswordRequirement.MISSING_SYMBOL
import com.granatum.core.domain.validation.PasswordRequirement.MAX_LENGTH
import com.granatum.core.domain.validation.PasswordRequirement.MIN_LENGTH
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
        assertTrue(MIN_LENGTH !in PasswordValidator.validate("Abcde1!x"))
        assertTrue(MIN_LENGTH in PasswordValidator.validate("Abcd1!x"))
    }

    @Test
    fun one_hundred_twenty_eight_characters_is_the_maximum() {
        val base = "Aa1!"
        assertTrue(MAX_LENGTH !in PasswordValidator.validate(base + "x".repeat(124)))
        assertTrue(MAX_LENGTH in PasswordValidator.validate(base + "x".repeat(125)))
    }

    @Test
    fun each_character_class_is_reported_on_its_own() {
        assertEquals(setOf(MISSING_UPPERCASE), PasswordValidator.validate("granatum1!"))
        assertEquals(setOf(MISSING_LOWERCASE), PasswordValidator.validate("GRANATUM1!"))
        assertEquals(setOf(MISSING_DIGIT), PasswordValidator.validate("Granatum!!"))
        assertEquals(setOf(MISSING_SYMBOL), PasswordValidator.validate("Granatum12"))
    }

    @Test
    fun whitespace_does_not_count_as_a_symbol() {
        assertTrue(MISSING_SYMBOL in PasswordValidator.validate("Granatum 1"))
    }

    @Test
    fun an_empty_password_fails_every_rule_except_the_maximum() {
        assertEquals(
            setOf(MIN_LENGTH, MISSING_UPPERCASE, MISSING_LOWERCASE, MISSING_DIGIT, MISSING_SYMBOL),
            PasswordValidator.validate("")
        )
    }
}
