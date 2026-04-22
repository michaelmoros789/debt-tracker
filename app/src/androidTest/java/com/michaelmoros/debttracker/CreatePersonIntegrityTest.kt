package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CreatePersonIntegrityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCreatePersonFlowAndDataIntegrity() {
        // App validation allows max 1 space.
        val uniqueName = "Test Case${UUID.randomUUID().toString().take(4)}"

        // 1. Setup: Anchor wait to ensure app is ready
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        
        // 2. Click Add (Empty state button or FAB)
        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }

        // 3. Fill Name
        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(uniqueName)
        
        // 4. Confirm
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()

        // 5. Wait for Person List to update
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$uniqueName").fetchSemanticsNodes().isNotEmpty()
        }

        // 6. Verify the person item exists in the main list
        composeTestRule.onNodeWithTag("person_item_$uniqueName").assertExists()
        
        // 7. Click the item to go to History screen
        composeTestRule.onNodeWithTag("person_item_$uniqueName").performClick()
        
        // 8. Verify the Header on the History Screen
        composeTestRule.waitForIdle()

        // We use useUnmergedTree = true because the parent Row is clickable, 
        // which causes it to merge all its children's semantics (hiding individual tags).
        composeTestRule.onNodeWithTag("history_person_name", useUnmergedTree = true)
            .assertTextEquals(uniqueName)
        
        // Final verification that we've reached the correct destination
        composeTestRule.onNodeWithTag("empty_history_text").assertExists()
    }
}
