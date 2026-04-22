package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class DeleteEmptyPersonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testDeleteEmptyPersonFrictionless() {
        // Simplified name to comply with the 1-space validation rule
        val uniqueName = "Delete${UUID.randomUUID().toString().take(4)}"
        
        // 1. Create a new person (starting from empty or existing list)
        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }

        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(uniqueName)
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()

        // 2. Wait for the person to appear and click them
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$uniqueName").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("person_item_$uniqueName").performClick()

        // 3. Open the "More" menu and click "Delete Person"
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Delete Person").performClick()

        // 4. Assert that we are back on the Main Screen (Debt Tracker title visible)
        // and NOT seeing the Friction Dialog (which has the text "Are you sure you want to delete")
        composeTestRule.waitForIdle()
        
        // Check if the title of the Main screen is displayed
        composeTestRule.onNodeWithText("Debt Tracker").assertIsDisplayed()
        
        // Ensure the Friction Dialog is NOT present
        composeTestRule.onNodeWithText("Delete Entry").assertDoesNotExist()
    }
}
