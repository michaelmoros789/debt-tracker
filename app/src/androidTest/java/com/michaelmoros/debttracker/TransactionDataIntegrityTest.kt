package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.text.SimpleDateFormat

class TransactionDataIntegrityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testTransactionCreationAndDataMatch() {
        val uniqueId = UUID.randomUUID().toString().take(4)
        val personName = "Tester$uniqueId"
        val testAmount = "1250.50"
        val testDescription = "Rent payment $uniqueId"
        val testRef = "REF-$uniqueId"
        
        // Match the format and TimeZone (UTC) used in the App components
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val expectedDate = sdf.format(Date())

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
        
        // Fill Amount and Description
        composeTestRule.onNodeWithTag("transaction_amount_field").performTextInput(testAmount)
        composeTestRule.onNodeWithTag("transaction_description_field").performTextInput(testDescription)
        
        // Dismiss keyboard to reveal dropdowns
        composeTestRule.onNodeWithTag("transaction_description_field").performImeAction()
        
        // Select Bank Transfer
        composeTestRule.onNodeWithTag("transaction_method_field").performClick()
        composeTestRule.onNodeWithTag("method_option_Bank Transfer").performClick()
        
        // Fill Reference Number
        composeTestRule.onNodeWithTag("transaction_reference_field").performTextInput(testRef)
        composeTestRule.onNodeWithTag("transaction_reference_field").performImeAction()
        
        // Save
        composeTestRule.onNodeWithTag("save_transaction_button").performClick()

        // 5. Navigate to Details Screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(testDescription).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(testDescription).performClick()
        composeTestRule.waitForIdle()

        // 6. DATA INTEGRITY ASSERTION
        
        // Verify Amount (Substring match allows for currency symbol presence)
        composeTestRule.onNodeWithTag("details_amount_value", useUnmergedTree = true)
            .assertTextContains(testAmount, substring = true)

        // Verify Description
        composeTestRule.onNodeWithTag("details_description_value", useUnmergedTree = true)
            .assertTextEquals(testDescription)

        // Verify Payment Method
        composeTestRule.onNodeWithTag("details_method_value", useUnmergedTree = true)
            .assertTextEquals("Bank Transfer")
            
        // Verify Reference Number
        composeTestRule.onNodeWithTag("details_reference_value", useUnmergedTree = true)
            .assertTextEquals(testRef)

        // Verify Date (MMMM dd, yyyy) - Now forced to UTC in both app and test
        composeTestRule.onNodeWithTag("details_date_value", useUnmergedTree = true)
            .assertTextEquals(expectedDate)
    }
}
