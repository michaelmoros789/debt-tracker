package com.michaelmoros.debttracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CategoryVisibilityEnforcementTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHiddenCategoriesAreNotAvailableInHistoryDropdown() {
        val uniqueName = "VisTest${UUID.randomUUID().toString().take(4)}"

        // Anchor: Ensure the app is launched and the compose hierarchy is ready
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Setup: Hide all categories except General in Manage Contexts
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Manage Contexts").performClick()
        composeTestRule.waitForIdle()

        // Hide Work, Family, Friends if they are visible
        val toHide = listOf("Work", "Family", "Friends")
        toHide.forEach { name ->
            val iconNodes = composeTestRule.onAllNodesWithTag("visibility_icon_$name", useUnmergedTree = true).fetchSemanticsNodes()
            if (iconNodes.isNotEmpty()) {
                val descriptions = iconNodes[0].config.getOrNull(SemanticsProperties.ContentDescription)
                val isVisible = descriptions?.any { it == "Visibility" } == true
                if (isVisible) {
                    composeTestRule.onNodeWithTag("context_card_$name").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
        
        // Go back to Main Screen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 2. Create a new person
        if (composeTestRule.onAllNodesWithTag("add_person_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("add_person_fab").performClick()
        } else {
            composeTestRule.onNodeWithTag("empty_add_person_button").performClick()
        }
        composeTestRule.onNodeWithTag("add_person_name_field").performTextInput(uniqueName)
        composeTestRule.onNodeWithTag("add_person_confirm_button").performClick()
        
        // 3. Navigate to the person's History Screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("person_item_$uniqueName").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("person_item_$uniqueName").performClick()
        composeTestRule.waitForIdle()

        // 4. Open the Change Category Dialog (the popup)
        composeTestRule.onNodeWithTag("history_header").performClick()
        composeTestRule.waitForIdle()

        // 5. INTERACT with the selectbox (OutlinedCard) inside the dialog to open the DropdownMenu
        composeTestRule.onNodeWithTag("category_selector_card").performClick()
        composeTestRule.waitForIdle()

        // 6. ASSERTION: Verify only 'General' is in the unmerged tree of the dropdown
        composeTestRule.onNodeWithTag("category_option_General").assertExists()
        
        // Ensure hidden ones are gone
        composeTestRule.onNodeWithTag("category_option_Work").assertDoesNotExist()
        composeTestRule.onNodeWithTag("category_option_Family").assertDoesNotExist()
        composeTestRule.onNodeWithTag("category_option_Friends").assertDoesNotExist()
        
        // Close the dialog
        composeTestRule.onNodeWithText("CANCEL").performClick()
        composeTestRule.waitForIdle()
    }
}
