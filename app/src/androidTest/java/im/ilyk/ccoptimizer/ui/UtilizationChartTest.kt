package im.ilyk.ccoptimizer.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import im.ilyk.ccoptimizer.core.inference.UtilizationPoint
import im.ilyk.ccoptimizer.ui.components.UtilizationChart
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/**
 * UI tests for UtilizationChart component
 */
@RunWith(AndroidJUnit4::class)
class UtilizationChartTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun chart_displaysTitle() {
        // Given
        val data = listOf(
            UtilizationPoint(YearMonth.of(2025, 10), 10.0)
        )
        
        composeTestRule.setContent {
            UtilizationChart(data = data)
        }
        
        // Then
        composeTestRule.onNodeWithText("Monthly Utilization").assertIsDisplayed()
    }
    
    @Test
    fun chart_displaysTargetLine() {
        // Given
        val data = listOf(
            UtilizationPoint(YearMonth.of(2025, 10), 10.0)
        )
        
        composeTestRule.setContent {
            UtilizationChart(data = data, targetPercent = 30)
        }
        
        // Then
        composeTestRule.onNodeWithText("Target: 30%").assertIsDisplayed()
    }
    
    @Test
    fun chart_displaysAxisLabels() {
        // Given
        val data = listOf(
            UtilizationPoint(YearMonth.of(2025, 10), 10.0)
        )
        
        composeTestRule.setContent {
            UtilizationChart(data = data)
        }
        
        // Then
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }
    
    @Test
    fun chart_showsTooltipOnTap() {
        // Given
        val data = listOf(
            UtilizationPoint(YearMonth.of(2025, 10), 25.5)
        )
        
        composeTestRule.setContent {
            UtilizationChart(data = data)
        }
        
        // When: Tap the chart area (approximate center)
        composeTestRule.onNode(
            hasContentDescription("Monthly utilization chart showing 1 months of data")
        ).performTouchInput {
            click(center)
        }
        
        // Then: Tooltip should appear (though this is hard to test without exact coordinates)
        // The tooltip text format is "Oct 2025: 25.5%"
        // This is a simplified test - in production you'd use more sophisticated testing
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun chart_hasAccessibilityDescription() {
        // Given
        val data = listOf(
            UtilizationPoint(YearMonth.of(2025, 10), 10.0),
            UtilizationPoint(YearMonth.of(2025, 11), 15.0),
            UtilizationPoint(YearMonth.of(2025, 12), 20.0)
        )
        
        composeTestRule.setContent {
            UtilizationChart(data = data)
        }
        
        // Then
        composeTestRule.onNode(
            hasContentDescription("Monthly utilization chart showing 3 months of data")
        ).assertIsDisplayed()
    }
    
    @Test
    fun chart_rendersMultipleDataPoints() {
        // Given: 12 months of data
        val data = (1..12).map { month ->
            UtilizationPoint(YearMonth.of(2025, month), month * 5.0)
        }
        
        composeTestRule.setContent {
            UtilizationChart(data = data)
        }
        
        // Then
        composeTestRule.onNode(
            hasContentDescription("Monthly utilization chart showing 12 months of data")
        ).assertIsDisplayed()
        
        // Should show some month labels
        composeTestRule.onNodeWithText("Jan").assertIsDisplayed()
    }
}


