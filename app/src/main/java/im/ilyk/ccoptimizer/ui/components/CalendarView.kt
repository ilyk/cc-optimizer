package im.ilyk.ccoptimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Calendar view for credit card payment tracking
 * Shows statement close dates (dotted line), due dates (solid line), and optimal payment windows
 */
@Composable
fun CalendarView(
    cards: List<CreditCard>,
    recommendations: List<PaymentRecommendation>,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedCard by remember { mutableStateOf<CreditCard?>(null) }
    var showAllCards by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Calendar header with month navigation
        CalendarHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
            onToday = { currentMonth = YearMonth.now() }
        )

        // View toggle: All cards vs Per card
        ViewToggle(
            showAllCards = showAllCards,
            onToggle = { showAllCards = it },
            cards = cards,
            selectedCard = selectedCard,
            onCardSelected = { selectedCard = it }
        )

        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            cards = if (showAllCards) cards else selectedCard?.let { listOf(it) } ?: emptyList(),
            recommendations = recommendations,
            modifier = Modifier.weight(1f)
        )

        // Legend
        CalendarLegend()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.semantics {
                    contentDescription = "Go to previous month"
                }
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    heading()
                }
            ) {
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onToday,
                    modifier = Modifier.semantics {
                        contentDescription = "Go to today's date"
                    }
                ) {
                    Text("Today", fontSize = 12.sp)
                }
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.semantics {
                    contentDescription = "Go to next month"
                }
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewToggle(
    showAllCards: Boolean,
    onToggle: (Boolean) -> Unit,
    cards: List<CreditCard>,
    selectedCard: CreditCard?,
    onCardSelected: (CreditCard?) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showAllCards,
                onClick = {
                    onToggle(true)
                    onCardSelected(null)
                },
                label = { Text("All cards") },
                modifier = Modifier.semantics {
                    contentDescription = if (showAllCards) "All cards view selected" else "Show all cards"
                }
            )
            FilterChip(
                selected = !showAllCards,
                onClick = {
                    onToggle(false)
                    if (selectedCard == null && cards.isNotEmpty()) {
                        onCardSelected(cards.first())
                    }
                },
                label = { Text("Per card") },
                modifier = Modifier.semantics {
                    contentDescription = if (!showAllCards) "Per card view selected" else "Show per card view"
                }
            )
        }

        // Card selector (shown when per-card view is active)
        if (!showAllCards) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEach { card ->
                    FilterChip(
                        selected = selectedCard?.id == card.id,
                        onClick = { onCardSelected(card) },
                        label = {
                            Text(
                                text = "${card.bankName} ••${card.lastFourDigits}",
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    cards: List<CreditCard>,
    recommendations: List<PaymentRecommendation>,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday
    val daysInMonth = currentMonth.lengthOfMonth()

    Column(modifier = modifier.padding(8.dp)) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        var dayCounter = 1
        val weeksNeeded = kotlin.math.ceil((firstDayOfWeek + daysInMonth) / 7.0).toInt()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeksNeeded) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(7) { dayOfWeek ->
                        val dayIndex = week * 7 + dayOfWeek

                        if (dayIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                            val date = currentMonth.atDay(dayCounter)
                            CalendarDay(
                                date = date,
                                cards = cards,
                                recommendations = recommendations,
                                modifier = Modifier.weight(1f)
                            )
                            dayCounter++
                        } else {
                            // Empty cell
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    cards: List<CreditCard>,
    recommendations: List<PaymentRecommendation>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = date == today

    // Find events for this date
    val closeEvents = cards.filter { it.statementCloseDate == date }
    val dueEvents = cards.filter { it.statementDueDate == date }
    val paymentEvents = recommendations.filter { it.recommendedPaymentDate == date }

    // Check if date is in optimal payment window (between close and due)
    val inOptimalWindow = cards.any { card ->
        card.statementCloseDate?.let { close ->
            card.statementDueDate?.let { due ->
                date.isAfter(close) && date.isBefore(due)
            }
        } ?: false
    }

    val dateDescription = buildString {
        append(date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d")))
        if (isToday) append(", today")
        if (closeEvents.isNotEmpty()) {
            append(", statement closes for ${closeEvents.size} card${if (closeEvents.size > 1) "s" else ""}")
        }
        if (dueEvents.isNotEmpty()) {
            append(", payment due for ${dueEvents.size} card${if (dueEvents.size > 1) "s" else ""}")
        }
        if (paymentEvents.isNotEmpty()) {
            append(", recommended payment date for ${paymentEvents.size} card${if (paymentEvents.size > 1) "s" else ""}")
        }
        if (inOptimalWindow) {
            append(", in optimal payment window")
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    inOptimalWindow -> Color(0xFFE8F5E9) // Light green for optimal window
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
            .semantics {
                contentDescription = dateDescription
                if (closeEvents.isNotEmpty() || dueEvents.isNotEmpty() || paymentEvents.isNotEmpty()) {
                    onClick {
                        // Could open details dialog
                        true
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day number
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Event indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close date indicator (dotted circle)
                if (closeEvents.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFFFF9800),
                                    style = Stroke(
                                        width = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f))
                                    )
                                )
                            }
                    )
                }

                // Due date indicator (solid circle)
                if (dueEvents.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFD32F2F), CircleShape)
                    )
                }

                // Payment recommendation (filled square)
                if (paymentEvents.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = Color(0xFFFF9800),
                label = "Close",
                isDotted = true
            )
            LegendItem(
                color = Color(0xFFD32F2F),
                label = "Due",
                isDotted = false
            )
            LegendItem(
                color = Color(0xFF4CAF50),
                label = "Pay",
                isDotted = false,
                isSquare = true
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
            )
            Text(
                text = "Optimal window",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDotted: Boolean = false,
    isSquare: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .then(
                    if (isDotted) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = color,
                                style = Stroke(
                                    width = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                                )
                            )
                        }
                    } else if (isSquare) {
                        Modifier.background(color, RoundedCornerShape(2.dp))
                    } else {
                        Modifier.background(color, CircleShape)
                    }
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

