package com.mart.villagemart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForLogin() {
        // The branded splash screen is shown first; wait for the login form to appear.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Password").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun loginScreenShowsBusinessIdentityAndOwnerInputs() {
        waitForLogin()
        composeRule.onNodeWithText("Welcome to VillageMart").assertIsDisplayed()
        composeRule.onNodeWithText("Mobile number or email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Forgot password?").assertIsDisplayed()
        composeRule.onNodeWithText("Create Account").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun createAccountOpensRegistrationAndLoginReturns() {
        waitForLogin()
        composeRule.onNodeWithText("Create Account").performScrollTo().performClick()
        composeRule.onNodeWithText("Create your owner account").assertIsDisplayed()
        composeRule.onNodeWithText("Full name").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm password").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Business name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Login").performScrollTo().performClick()
        composeRule.onNodeWithText("Welcome to VillageMart").assertIsDisplayed()
    }
}
