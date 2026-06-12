package com.lizz.myapptemplate.auth

import com.lizz.myapptemplate.auth.domain.ValidateCredentialsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidateCredentialsUseCaseTest {
    private val validate = ValidateCredentialsUseCase()

    @Test
    fun validCredentialsPass() {
        val result = validate("user@example.com", "password123")

        assertTrue(result.isValid)
        assertNull(result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun blankEmailAndShortPasswordAreBothReported() {
        val result = validate("", "short")

        assertEquals("Email is required", result.emailError)
        assertNotNull(result.passwordError)
        assertTrue(!result.isValid)
    }

    @Test
    fun malformedEmailsAreRejected() {
        for (email in listOf("no-at-sign", "@leading", "trailing@")) {
            assertNotNull(validate(email, "password123").emailError, "expected error for '$email'")
        }
    }
}
