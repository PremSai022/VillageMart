package com.mart.villagemart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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

    @Test
    fun loginScreenShowsBusinessIdentityAndOwnerInputs() {
        // The branded splash screen is shown first; wait for the login form to appear.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Password").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("VillageMart").assertIsDisplayed()
        composeRule.onNodeWithText("Username or mobile number").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
    }
}
