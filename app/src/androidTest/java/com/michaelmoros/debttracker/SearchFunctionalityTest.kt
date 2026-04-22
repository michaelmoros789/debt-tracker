package com.michaelmoros.debttracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.text.SimpleDateFormat

class SearchFunctionalityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSearchFunctionalityByAllProperties() {
        val uniqueId = UUID.randomUUID().toString().take(4)
        val personName = "SearchTester$uniqueId"
        
        // Data for testing
        val amount1 = "987.65"
        val desc1 = "TodayItem$uniqueId"
        
        val amount2 = "111.11"
        val desc2 = "PastMonthItem"
        val method2 = "Bank Transfer"
        val ref2 = "REF-SEARCH-$uniqueId"
        
        // Date Setup
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val sdf = SimpleDateFormat("MMM", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        
        val currentMonthStr = sdf.format(calendar.time)
        
        calendar.add(Calendar.MONTH, -1)
        val pastMonthStr = sdf.format(calendar.time)

        // 1. Setup: Ensure app is ready and create a person
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }

        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(personName)
        composeTestRule.onNodeWithTag("add_person_name_field").performImeAction()
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()

        // 2. Navigate to History
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$personName").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("person_item_$personName").performClick()

        // 3. Add Transaction 1 (Current Month, unique amount/desc)
        composeTestRule.onNodeWithTag("empty_history_add_transaction_button").performClick()
        composeTestRule.onNodeWithTag("transaction_amount_field").performTextInput(amount1)
        composeTestRule.onNodeWithTag("transaction_description_field").performTextInput(desc1)
        composeTestRule.onNodeWithTag("transaction_description_field").performImeAction()
        composeTestRule.onNodeWithTag("save_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // 4. Add Transaction 2 (Set to PREVIOUS MONTH, unique method/ref)
        composeTestRule.onNodeWithTag("add_transaction_button").performClick()
        composeTestRule.onNodeWithTag("transaction_amount_field").performTextInput(amount2)
        composeTestRule.onNodeWithTag("transaction_description_field").performTextInput(desc2)
        composeTestRule.onNodeWithTag("transaction_description_field").performImeAction()

        // Open Date Picker and navigate back one month
        composeTestRule.onNodeWithContentDescription("Select Date").performClick()
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        composeTestRule.onNodeWithText("15").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        // Set Method and Ref
        composeTestRule.onNodeWithTag("transaction_method_field").performClick()
        composeTestRule.onNodeWithTag("method_option_$method2").performClick()
        composeTestRule.onNodeWithTag("transaction_reference_field").performTextInput(ref2)
        composeTestRule.onNodeWithTag("transaction_reference_field").performImeAction()

        composeTestRule.onNodeWithTag("save_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // --- START SEARCH TESTS ---
        composeTestRule.onNodeWithContentDescription("Search").performClick()

        // CASE 1: Test Amount Search
        composeTestRule.onNode(hasSetTextAction()).performTextInput(amount1)
        composeTestRule.onNode(hasText(desc1) and isEditable().not()).assertExists()
        composeTestRule.onNode(hasText(desc2) and isEditable().not()).assertDoesNotExist()
        
        // CASE 2: Test Description Search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(desc2)
        composeTestRule.onNode(hasText(desc2) and isEditable().not()).assertExists()
        composeTestRule.onNode(hasText(desc1) and isEditable().not()).assertDoesNotExist()

        // CASE 3: Test Reference Number Search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(ref2)
        composeTestRule.onNode(hasText(desc2) and isEditable().not()).assertExists()
        composeTestRule.onNode(hasText(desc1) and isEditable().not()).assertDoesNotExist()

        // CASE 4: Test Payment Method Search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(method2)
        composeTestRule.onNode(hasText(desc2) and isEditable().not()).assertExists()
        composeTestRule.onNode(hasText(desc1) and isEditable().not()).assertDoesNotExist()

        // CASE 5: Test Transaction Date Search (Past Month abbreviation)
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(pastMonthStr)
        composeTestRule.onNode(hasText(desc2) and isEditable().not()).assertExists()
        composeTestRule.onNode(hasText(desc1) and isEditable().not()).assertDoesNotExist()

        // Final verification: No Match Case
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("NoMatchingDataXYZ")
        composeTestRule.onNodeWithText("No transactions match your search").assertIsDisplayed()

        // Cleanup: Close search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
    }
}
