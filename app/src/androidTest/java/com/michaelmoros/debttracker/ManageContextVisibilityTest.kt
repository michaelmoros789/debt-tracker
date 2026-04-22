package com.michaelmoros.debttracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ManageContextVisibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCategoryDeletionRestoresGeneralVisibility() {
        // Use a simple lowercase name to avoid title-casing mismatch issues
        val uniqueId = UUID.randomUUID().toString().take(4)
        val testCategoryInput = "bugfix$uniqueId"
        val testCategoryFormatted = "Bugfix$uniqueId" // The app will capitalize the first letter

        // 1. Navigate to Manage Contexts
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Manage Contexts").performClick()
        composeTestRule.waitForIdle()

        // Step 1: Disable ALL categories except General
        // We find all visibility icons and click their parent cards if they are 'Visible'
        val allVisibilityIcons = composeTestRule.onAllNodes(hasContentDescription("Visibility"), useUnmergedTree = true)
            .fetchSemanticsNodes()
        
        allVisibilityIcons.forEach { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: ""
            if (tag.startsWith("visibility_icon_") && !tag.endsWith("_General")) {
                val contextName = tag.removePrefix("visibility_icon_")
                composeTestRule.onNodeWithTag("context_card_$contextName").performClick()
                composeTestRule.waitForIdle()
            }
        }

        // Step 2: Add the new category
        composeTestRule.onNodeWithTag("new_context_field").performTextInput(testCategoryInput)
        composeTestRule.onNodeWithTag("add_context_button").performClick()
        
        // Wait for the new category to appear and be confirmed as VISIBLE
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("visibility_icon_$testCategoryFormatted", useUnmergedTree = true)
                .fetchSemanticsNodes().any { 
                    it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Visibility") == true &&
                    it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("VisibilityOff") == false
                }
        }
        composeTestRule.waitForIdle()

        // Step 3: Disable General
        // Now 'General' and 'testCategory' are both visible. We hide General.
        composeTestRule.onNodeWithTag("context_card_General").performClick()
        
        // Wait for General to definitely be HIDDEN before proceeding
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val nodes = composeTestRule.onAllNodesWithTag("visibility_icon_General", useUnmergedTree = true).fetchSemanticsNodes()
            nodes.isNotEmpty() && nodes[0].config.getOrNull(SemanticsProperties.ContentDescription)?.contains("VisibilityOff") == true
        }
        composeTestRule.waitForIdle()

        // Step 4: Delete the new category
        // Since it's the only one visible, its deletion must force General back to visible.
        composeTestRule.onNodeWithTag("delete_context_$testCategoryFormatted", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Step 5: Verify if General is visible automatically
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val nodes = composeTestRule.onAllNodesWithTag("visibility_icon_General", useUnmergedTree = true).fetchSemanticsNodes()
            nodes.isNotEmpty() && nodes[0].config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Visibility") == true &&
            nodes[0].config.getOrNull(SemanticsProperties.ContentDescription)?.contains("VisibilityOff") == false
        }
        
        // Final assertion
        composeTestRule.onNodeWithTag("visibility_icon_General", useUnmergedTree = true)
            .assertContentDescriptionEquals("Visibility")
            
        composeTestRule.onNodeWithTag("context_card_General").assertIsDisplayed()
    }
}
