package im.ilyk.ccoptimizer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.utils.ValidationResult
import im.ilyk.ccoptimizer.utils.Validators

@Composable
fun StatementDateInputDialog(
    card: CreditCard,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var statementDay by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(R.string.dialog_statement_date_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Card info
                Text(
                    text = stringResource(R.string.card_info_format, card.name, card.lastFourDigits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instructions
                Text(
                    text = stringResource(R.string.dialog_statement_date_question),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.dialog_statement_date_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Input field
                OutlinedTextField(
                    value = statementDay,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^[0-9]{1,2}$"))) {
                            statementDay = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.dialog_statement_day_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = {
                        error?.let { 
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.dialog_statement_day_placeholder)) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            when (val result = Validators.isValidStatementDayString(statementDay)) {
                                is ValidationResult.Success -> {
                                    onConfirm(result.value)
                                }
                                is ValidationResult.Error -> {
                                    error = result.message
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.button_save))
                    }
                }
            }
        }
    }
}

