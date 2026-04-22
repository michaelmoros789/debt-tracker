package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class DeleteTransactionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCreateAndDeleteTransaction() {
        val uniqueId = UUID.randomUUID().toString().take(4)
        val personName = "Deleter$uniqueId"
        val testAmount = "500"
        val testDescription = "Temp Transaction $uniqueId"

        // 1. Setup: Anchor wait to ensure app is ready
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Create a person
        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }
        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(personName)
        composeTestRule.onNodeWithTag("add_person_name_field").performImeAction() 
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()

        // 3. Navigate to History
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$personName").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("person_item_$personName").performClick()

        // 4. Add Transaction
        composeTestRule.onNodeWithTag("empty_history_add_transaction_button").performClick()
        composeTestRule.onNodeWithTag("transaction_amount_field").performTextInput(testAmount)
        composeTestRule.onNodeWithTag("transaction_description_field").performTextInput(testDescription)
        composeTestRule.onNodeWithTag("transaction_description_field").performImeAction()
        composeTestRule.onNodeWithTag("save_transaction_button").performClick()

        // 5. Verify transaction exists in list and click it
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(testDescription).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(testDescription).performClick()
        composeTestRule.waitForIdle()

        // 6. Delete the transaction from Details Screen
        composeTestRule.onNodeWithTag("delete_transaction_details_button").performClick()
        
        // Confirm deletion in dialog
        composeTestRule.onNodeWithTag("confirm_delete_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // 7. Verify we are back in History Screen and transaction is GONE
        composeTestRule.onNodeWithText(testDescription).assertDoesNotExist()
        
        // Ensure "History is empty" state returns if it was the only transaction
        composeTestRule.onNodeWithTag("empty_history_text").assertExists()
    }
}
