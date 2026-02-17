package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/**
 * Expandable card component with accessibility support
 * - TalkBack labels
 * - 48dp minimum touch targets
 * - 200ms expand/collapse animation
 * - WCAG AA compliant
 */
@Composable
fun ExpandableCard(
    title: String,
    subtitle: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expand_arrow_rotation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) {
                // Accessibility: Describe the expandable state
                stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                // Custom action for expand/collapse
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = if (isExpanded) "Collapse" else "Expand"
                    ) {
                        onToggle()
                        true
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header - clickable with minimum 48dp height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp) // WCAG minimum touch target
                    .clickable(
                        onClickLabel = if (isExpanded) "Collapse card" else "Expand card"
                    ) { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (isExpanded) "Collapse details" else "Expand details"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null, // Described by button
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
            
            // Expandable content with 200ms animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 200),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 200),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    content()
                }
            }
        }
    }
}

/**
 * Accessible button with proper touch target size and labels
 */
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp) // WCAG minimum
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            },
        enabled = enabled
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            Text(text)
        }
    }
}

/**
 * Accessible icon button with proper touch target and label
 */
@Composable
fun AccessibleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // WCAG minimum touch target
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        enabled = enabled
    ) {
        icon()
    }
}

/**
 * Accessible text with proper heading semantics
 */
@Composable
fun AccessibleHeading(
    text: String,
    level: Int = 1,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = when (level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.titleLarge
    }
) {
    Text(
        text = text,
        style = style,
        modifier = modifier.semantics {
            heading()
        }
    )
}

/**
 * Loading indicator with accessibility announcement
 */
@Composable
fun AccessibleLoadingIndicator(
    message: String = "Loading",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

