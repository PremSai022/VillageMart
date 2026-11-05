package com.mart.villagemart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationValidationTest {
    private val valid = RegisterForm(
        fullName = "Business Owner",
        mobile = "9876543210",
        email = "owner@example.com",
        password = "SecurePassword123",
        confirmPassword = "SecurePassword123",
        businessName = "VillageMart",
        businessAddress = "Hyderabad",
        acceptTerms = true
    )

    @Test
    fun validFormHasNoErrors() {
        assertTrue(validateRegistration(valid).isEmpty())
        assertTrue(validateRegistration(valid.copy(email = "", businessAddress = "")).isEmpty())
    }

    @Test
    fun emptyFormReportsEveryMandatoryField() {
        val errors = validateRegistration(RegisterForm())
        assertEquals(
            setOf(
                RegisterField.FULL_NAME, RegisterField.MOBILE, RegisterField.PASSWORD,
                RegisterField.CONFIRM_PASSWORD, RegisterField.BUSINESS_NAME, RegisterField.TERMS
            ),
            errors.keys
        )
    }

    @Test
    fun rejectsInvalidMobileEmailAndWeakPassword() {
        assertTrue(RegisterField.MOBILE in validateRegistration(valid.copy(mobile = "12345")))
        assertTrue(RegisterField.MOBILE in validateRegistration(valid.copy(mobile = "5876543210")))
        assertTrue(RegisterField.EMAIL in validateRegistration(valid.copy(email = "not-an-email")))
        val weak = validateRegistration(valid.copy(password = "password", confirmPassword = "password"))
        assertTrue(RegisterField.PASSWORD in weak)
    }

    @Test
    fun confirmPasswordMustMatch() {
        val errors = validateRegistration(valid.copy(confirmPassword = "SecurePassword124"))
        assertEquals("Passwords do not match", errors[RegisterField.CONFIRM_PASSWORD])
    }
}
