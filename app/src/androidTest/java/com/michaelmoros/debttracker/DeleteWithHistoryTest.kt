package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class DeleteWithHistoryTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testDeletePersonWithHistoryTriggersFriction() {
        val uniqueName = "History${UUID.randomUUID().toString().take(4)}"
        
        // 1. Create a new person
        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }
        
        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(uniqueName)
        composeTestRule.onNodeWithTag("add_person_name_field").performImeAction()
        
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()

        // 2. Enter history
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$uniqueName").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("person_item_$uniqueName").performClick()

        // 3. Add a simple transaction to create history
        composeTestRule.onNodeWithTag("empty_history_add_transaction_button").performClick()
        
        composeTestRule.onNodeWithTag("transaction_amount_field").performTextInput("100")
        
        composeTestRule.onNodeWithTag("transaction_description_field").performTextInput("Test Item")
        composeTestRule.onNodeWithTag("transaction_description_field").performImeAction()
        
        // Wait for keyboard to clear and layout to settle
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("save_transaction_button")
            .assertIsEnabled()
            .performClick()
        
        // Wait for list to reflect the new transaction (UI state transition from empty to list)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("transaction_list").fetchSemanticsNodes().isNotEmpty()
        }
        
        // 4. Try to delete the person
        composeTestRule.onNodeWithTag("more_options_button").performClick()
        composeTestRule.onNodeWithTag("delete_person_menu_item").performClick()

        // 5. Assert that the Friction Dialog (Delete Entry) IS displayed
        composeTestRule.onNodeWithText("Delete Entry").assertIsDisplayed()
        
        // Cleanup: Dismiss the dialog
        composeTestRule.onNodeWithText("CANCEL").performClick()
    }
}
