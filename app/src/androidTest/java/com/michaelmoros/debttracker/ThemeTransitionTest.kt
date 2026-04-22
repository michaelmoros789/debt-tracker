package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ThemeTransitionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testThemeTransitions() {
        // 1. Navigate to Settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // 2. Navigate to Theme Settings
        composeTestRule.onNodeWithText("Theme Settings").performClick()

        // 3. Test transition from Light to Dark
        
        // Ensure we are in Light mode first
        composeTestRule.onNodeWithTag("theme_option_LIGHT").performClick()
        composeTestRule.waitForIdle()
        
        // Click Dark mode
        composeTestRule.onNodeWithTag("theme_option_DARK").performClick()
        composeTestRule.waitForIdle()
        
        // Assert visual elements exist and are correctly identified
        composeTestRule.onNodeWithTag("theme_top_bar").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_body").assertExists().assertIsDisplayed()
        
        // Verify Dark mode is selected
        composeTestRule.onNodeWithTag("theme_option_DARK").assertIsSelected()
        
        // 4. Change it again (back to Light)
        composeTestRule.onNodeWithTag("theme_option_LIGHT").performClick()
        composeTestRule.waitForIdle()
        
        // Verify Light mode is selected
        composeTestRule.onNodeWithTag("theme_option_LIGHT").assertIsSelected()
        
        // 5. Final check - back to Settings
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
