package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.ui.viewmodel.CardDetailViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.PaymentOperationState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Card detail screen shown as a bottom sheet or full screen dialog
 * 
 * Displays payment history and allows marking payments as paid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CreditCard,
    viewModel: CardDetailViewModel,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val payments by viewModel.payments.collectAsState()
    val operationState by viewModel.paymentOperation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(card.id) {
        viewModel.setCard(card.id)
    }
    
    // Show snackbar for operation feedback
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is PaymentOperationState.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
            }
            is PaymentOperationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.bankName ?: "Credit Card",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.semantics {
                                    contentDescription = "Close card details"
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        Text(
                            text = "•••• ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mark as paid section
                    item {
                        val balanceToShow = if (card.manualPaymentsTotal > 0 && card.adjustedBalance != null) {
                            maxOf(0.0, card.adjustedBalance)  // Never negative
                        } else {
                            card.currentBalance
                        }
                        
                        android.util.Log.d(
                            "CardDetailScreen",
                            "Card ${card.id}: bankBalance=${card.currentBalance}, " +
                            "manualPaymentsTotal=${card.manualPaymentsTotal}, " +
                            "adjustedBalance=${card.adjustedBalance}, " +
                            "showing in form: $balanceToShow"
                        )
                        
                        MarkPaidSection(
                            currentBalance = balanceToShow,
                            isLoading = operationState is PaymentOperationState.Loading,
                            onMarkPaid = { date, amountCents ->
                                android.util.Log.d("CardDetailScreen", "Mark paid clicked: cardId=${card.id}, date=$date, amount=$amountCents")
                                viewModel.markPaid(card.id, date, amountCents) {
                                    // Refresh card data after successful payment
                                    onPaymentSuccess()
                                }
                            }
                        )
                    }
                    
                    // Info note about balance updates
                    if (payments.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Balance shown is from your bank. Manual payments update within 24 hours.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Payment history
                    item {
                        Text(
                            text = "Payment History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (payments.isEmpty()) {
                        item {
                            Text(
                                text = "No manual payments recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(payments) { payment ->
                            PaymentItem(
                                payment = payment,
                                onUnmark = {
                                    viewModel.unmarkPaid(
                                        card.id,
                                        payment.postedDate,
                                        payment.amountCents
                                    ) {
                                        // Refresh card data after removing payment
                                        onPaymentSuccess()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * Section for marking a payment as paid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkPaidSection(
    currentBalance: Double,
    isLoading: Boolean = false,
    onMarkPaid: (LocalDate, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dateString by remember { mutableStateOf(LocalDate.now().toString()) }
    var amountString by remember { 
        mutableStateOf(
            if (currentBalance > 0) String.format("%.2f", currentBalance) else ""
        )
    }
    var showDateError by remember { mutableStateOf(false) }
    var showAmountError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Update amount when currentBalance changes (different card opened)
    LaunchedEffect(currentBalance) {
        if (amountString.isEmpty() || amountString.toDoubleOrNull() == 0.0) {
            amountString = if (currentBalance > 0) String.format("%.2f", currentBalance) else ""
        }
    }
    
    // Reset form after successful operation
    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (wasLoading && !isLoading) {
            // Operation completed - reset form
            kotlinx.coroutines.delay(500) // Brief delay to show success message
            dateString = LocalDate.now().toString()
            amountString = "" // Clear amount so user must enter new payment
            showDateError = false
            showAmountError = false
        }
        wasLoading = isLoading
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Mark payment as paid"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mark as Paid",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Date input with picker
            OutlinedTextField(
                value = dateString,
                onValueChange = { },
                readOnly = true,
                label = { Text("Payment Date") },
                placeholder = { Text("YYYY-MM-DD") },
                isError = showDateError,
                supportingText = if (showDateError) {
                    { Text("Invalid date format") }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select date"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Payment date: $dateString. Tap to open calendar picker"
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showDatePicker = true
                        }
                    }
            )
            
            // Amount input
            OutlinedTextField(
                value = amountString,
                onValueChange = {
                    amountString = it
                    showAmountError = false
                },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                prefix = { Text("$") },
                isError = showAmountError,
                supportingText = if (showAmountError) {
                    { Text("Invalid amount") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Enter payment amount in dollars"
                    }
            )
            
            // Submit button
            Button(
                onClick = {
                    val date = try {
                        LocalDate.parse(dateString)
                    } catch (e: Exception) {
                        showDateError = true
                        null
                    }
                    
                    val amountCents = try {
                        (amountString.toDouble() * 100).toLong()
                    } catch (e: Exception) {
                        showAmountError = true
                        null
                    }
                    
                    if (date != null && amountCents != null && amountCents > 0) {
                        onMarkPaid(date, amountCents)
                    } else {
                        if (date == null) showDateError = true
                        if (amountCents == null || amountCents <= 0) showAmountError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Mark this payment as paid"
                    },
                enabled = dateString.isNotBlank() && amountString.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Mark as Paid")
                }
            }
        }
        
        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    LocalDate.parse(dateString)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            )
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                dateString = selectedDate.toString()
                                showDateError = false
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

/**
 * Single payment item in the history list
 */
@Composable
fun PaymentItem(
    payment: im.ilyk.ccoptimizer.data.db.entities.PaymentEntity,
    onUnmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val amountDollars = payment.amountCents / 100.0
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Payment of $${String.format("%.2f", amountDollars)} on ${payment.postedDate.format(dateFormatter)}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = payment.postedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = payment.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", amountDollars)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onUnmark,
                    modifier = Modifier.semantics {
                        contentDescription = "Remove payment record"
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


