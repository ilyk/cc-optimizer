package im.ilyk.ccoptimizer.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.ui.screens.MarkPaidSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for CardDetailScreen components
 */
@RunWith(AndroidJUnit4::class)
class CardDetailScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun markPaidSection_displaysCorrectly() {
        // Given
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 500.0,
                isLoading = false,
                onMarkPaid = { _, _ -> }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Mark as Paid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount").assertIsDisplayed()
    }
    
    @Test
    fun markPaidSection_buttonDisabledWhenFieldsEmpty() {
        // Given
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 0.0,
                isLoading = false,
                onMarkPaid = { _, _ -> }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Mark as Paid")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
    
    @Test
    fun markPaidSection_showsErrorForInvalidDate() {
        // Given
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 500.0,
                isLoading = false,
                onMarkPaid = { _, _ -> }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Payment Date").performTextInput("invalid-date")
        composeTestRule.onNodeWithText("Amount").performTextInput("100")
        composeTestRule.onNodeWithText("Mark as Paid").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Invalid date format. Use YYYY-MM-DD")
            .assertIsDisplayed()
    }
    
    @Test
    fun markPaidSection_showsErrorForInvalidAmount() {
        // Given
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 500.0,
                isLoading = false,
                onMarkPaid = { _, _ -> }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Payment Date").performTextInput("2025-10-15")
        composeTestRule.onNodeWithText("Amount").performTextInput("invalid")
        composeTestRule.onNodeWithText("Mark as Paid").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Invalid amount")
            .assertIsDisplayed()
    }
    
    @Test
    fun markPaidSection_callsOnMarkPaidWithValidInput() {
        // Given
        var calledWithDate: java.time.LocalDate? = null
        var calledWithAmount: Long? = null
        
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 500.0,
                isLoading = false,
                onMarkPaid = { date, amount ->
                    calledWithDate = date
                    calledWithAmount = amount
                }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Payment Date").performTextInput("2025-10-15")
        composeTestRule.onNodeWithText("Amount").performTextInput("100.50")
        composeTestRule.onNodeWithText("Mark as Paid").performClick()
        
        // Then
        assert(calledWithDate == java.time.LocalDate.of(2025, 10, 15))
        assert(calledWithAmount == 10050L) // $100.50 = 10050 cents
    }
    
    @Test
    fun markPaidSection_hasAccessibilityLabels() {
        // Given
        composeTestRule.setContent {
            MarkPaidSection(
                currentBalance = 500.0,
                isLoading = false,
                onMarkPaid = { _, _ -> }
            )
        }
        
        // Then
        composeTestRule.onNode(
            hasContentDescription("Mark payment as paid")
        ).assertIsDisplayed()
        
        composeTestRule.onNode(
            hasContentDescription("Enter payment date in YYYY-MM-DD format")
        ).assertIsDisplayed()
        
        composeTestRule.onNode(
            hasContentDescription("Enter payment amount in dollars")
        ).assertIsDisplayed()
        
        composeTestRule.onNode(
            hasContentDescription("Mark this payment as paid")
        ).assertIsDisplayed()
    }
}


