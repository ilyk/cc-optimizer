package im.ilyk.ccoptimizer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import im.ilyk.ccoptimizer.BuildConfig
import im.ilyk.ccoptimizer.core.privacy.DataWiper
import im.ilyk.ccoptimizer.core.telemetry.TelemetryClient
import im.ilyk.ccoptimizer.core.telemetry.TelemetryConfig
import im.ilyk.ccoptimizer.core.telemetry.TelemetryEvent
import im.ilyk.ccoptimizer.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsState(initial = false)
    val telemetryUrl by viewModel.telemetryUrl.collectAsState(initial = null)
    val exportStatus by viewModel.exportStatus.collectAsState()
    val deleteStatus by viewModel.deleteStatus.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTelemetryDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewJson by remember { mutableStateOf("") }
    
    // SAF launcher for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Data usage summary
            DataUsageSection()
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Export data
            ExportDataSection(
                status = exportStatus,
                onExport = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    exportLauncher.launch("cc_optimizer_data_$timestamp.json")
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Delete all data
            DeleteDataSection(
                status = deleteStatus,
                onDelete = { showDeleteDialog = true }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Telemetry (opt-in, self-hosted)
            TelemetrySection(
                enabled = telemetryEnabled,
                endpointUrl = telemetryUrl,
                onToggle = { enabled ->
                    if (enabled) {
                        showTelemetryDialog = true
                    } else {
                        viewModel.disableTelemetry()
                    }
                },
                onPreview = {
                    previewJson = viewModel.previewTelemetry()
                    showPreviewDialog = true
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Link to docs
            DocsSection()
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteAllData(context as FragmentActivity)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    // Telemetry setup dialog
    if (showTelemetryDialog) {
        TelemetrySetupDialog(
            onConfirm = { url, sampleRate ->
                viewModel.enableTelemetry(url, sampleRate)
                showTelemetryDialog = false
            },
            onDismiss = { showTelemetryDialog = false }
        )
    }
    
    // Preview dialog
    if (showPreviewDialog) {
        PreviewDialog(
            json = previewJson,
            onDismiss = { showPreviewDialog = false }
        )
    }
    
    // Show snackbar for status messages
    LaunchedEffect(exportStatus, deleteStatus) {
        // Handle status updates
    }
}

@Composable
private fun DataUsageSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Data We Use",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app stores locally on your device:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val dataItems = listOf(
            "• Credit card balances and limits",
            "• Statement and due dates",
            "• Issuer names (e.g., Chase, Amex)",
            "• Your calculated payment cycles"
        )
        dataItems.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We do NOT collect:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val notCollected = listOf(
            "• Purchase categories or transaction details",
            "• Your identity or personal info",
            "• Usage analytics (unless you opt-in below)"
        )
        notCollected.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun ExportDataSection(
    status: String?,
    onExport: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Export Data",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Save your cards and cycles as JSON",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onExport) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        }
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DeleteDataSection(
    status: String?,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delete All Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permanently wipes all local data, caches, and settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TelemetrySection(
    enabled: Boolean,
    endpointUrl: String?,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Self-Hosted Analytics",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (enabled && !endpointUrl.isNullOrBlank()) {
                        "Sending to: $endpointUrl"
                    } else {
                        "Send anonymous usage to your own server (advanced)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
        
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Preview Payload")
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "⚠️ Default OFF. No third-party analytics. Only minimal, non-PII events.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun DocsSection() {
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Documentation",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Open PRIVACY.md (in app viewer or browser)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/yourusername/cc-optimizer/blob/main/docs/PRIVACY.md")
                    }
                    context.startActivity(intent)
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Open SECURITY.md
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/yourusername/cc-optimizer/blob/main/docs/SECURITY.md")
                    }
                    context.startActivity(intent)
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Security Documentation", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TelemetrySetupDialog(
    onConfirm: (String, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("https://") }
    var sampleRate by remember { mutableStateOf("1.0") }
    var urlError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Self-Hosted Analytics") },
        text = {
            Column {
                Text(
                    text = "Enter your HTTPS endpoint URL:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = if (!it.startsWith("https://")) {
                            "Must use HTTPS"
                        } else null
                    },
                    label = { Text("Endpoint URL") },
                    placeholder = { Text("https://analytics.example.com/events") },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = sampleRate,
                    onValueChange = { sampleRate = it },
                    label = { Text("Sample Rate (0.0 - 1.0)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Only minimal, anonymous events will be sent (screen views, actions). No financial data or PII.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = sampleRate.toFloatOrNull() ?: 1f
                    if (url.startsWith("https://") && rate in 0f..1f) {
                        onConfirm(url, rate)
                    }
                },
                enabled = url.startsWith("https://")
            ) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PreviewDialog(
    json: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telemetry Payload Preview") },
        text = {
            Column {
                Text(
                    text = "This is what we send to your endpoint:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = json,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val dataWiper: DataWiper,
    private val telemetryConfig: TelemetryConfig,
    private val telemetryClient: TelemetryClient,
    private val database: AppDatabase
) : ViewModel() {

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow<String?>(null)
    val deleteStatus: StateFlow<String?> = _deleteStatus.asStateFlow()

    val telemetryEnabled = telemetryConfig.isEnabled
    val telemetryUrl = telemetryConfig.endpointUrl

    fun exportData(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _exportStatus.value = "Exporting..."
                
                val cards = database.cardDao().getAllActive()
                val cycles = mutableListOf<im.ilyk.ccoptimizer.data.db.entities.CycleEntity>()
                
                // Collect all cycles for all cards
                cards.forEach { card ->
                    val cardCycles = database.cycleDao().getAllByCard(card.cardId)
                    cycles.addAll(cardCycles)
                }
                
                val export = JSONObject().apply {
                    put("schema_version", "1.0")  // Export schema version
                    put("app_version", BuildConfig.VERSION_NAME)
                    put("exported_at", System.currentTimeMillis())
                    put("export_note", "This export contains non-sensitive card metadata. Card IDs are hashed for privacy.")
                    put("cards", JSONArray().apply {
                        cards.forEach { card: im.ilyk.ccoptimizer.data.db.entities.CardEntity ->
                            put(JSONObject().apply {
                                // Use hashed ID instead of raw ID for privacy
                                put("id_hash", hashCardId(card.cardId))
                                put("name", card.displayName ?: "Unknown")
                                put("issuer", card.issuer ?: "Unknown")
                                put("last4", card.last4 ?: "****")
                                put("is_active", card.isActive)
                                put("opened_at", card.openedAt?.toString())
                            })
                        }
                    })
                    put("cycles", JSONArray().apply {
                        cycles.forEach { cycle: im.ilyk.ccoptimizer.data.db.entities.CycleEntity ->
                            put(JSONObject().apply {
                                // Use hashed card ID for privacy
                                put("card_id_hash", hashCardId(cycle.cardId))
                                put("close_day", cycle.closeDay)
                                put("cadence", cycle.cadence)
                                put("confidence", cycle.confidence)
                                put("next_close_date", cycle.nextCloseDate?.toString())
                                put("computed_at", cycle.computedAt.toString())
                            })
                        }
                    })
                }
                
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(export.toString(2).toByteArray())
                }
                
                _exportStatus.value = "Export completed"
                telemetryClient.trackAction("data_exported")
            } catch (e: Exception) {
                Timber.e(e, "Failed to export data")
                _exportStatus.value = "Export failed: ${e.message}"
            }
        }
    }

    fun deleteAllData(activity: FragmentActivity) {
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModelScope.launch {
                        _deleteStatus.value = "Deleting..."
                        val result = dataWiper.wipeAll()
                        if (result.isSuccess) {
                            _deleteStatus.value = "All data deleted"
                        } else {
                            _deleteStatus.value = "Delete failed: ${result.exceptionOrNull()?.message}"
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    _deleteStatus.value = "Authentication failed"
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Data Deletion")
            .setSubtitle("This will permanently delete all local data")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun enableTelemetry(url: String, sampleRate: Float) {
        viewModelScope.launch {
            try {
                telemetryConfig.enable(url, sampleRate)
            } catch (e: Exception) {
                Timber.e(e, "Failed to enable telemetry")
            }
        }
    }

    fun disableTelemetry() {
        viewModelScope.launch {
            telemetryConfig.disable()
        }
    }

    fun previewTelemetry(): String {
        return telemetryClient.preview(
            TelemetryEvent.ScreenView("preview_screen")
        )
    }
    
    /**
     * Hash card ID with SHA-256 for privacy in exports.
     * Salted hash ensures IDs cannot be trivially linked back.
     */
    private fun hashCardId(cardId: String): String {
        return try {
            val salt = "cc_optimizer_export_salt_v1"  // Static salt for consistent hashing
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest("$salt$cardId".toByteArray())
            hash.joinToString("") { "%02x".format(it) }.take(16)  // Take first 16 chars
        } catch (e: Exception) {
            Timber.e(e, "Failed to hash card ID")
            "***"
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Data?")
            }
        },
        text = {
            Column {
                Text(
                    text = "This will permanently delete:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val items = listOf(
                    "• All credit cards",
                    "• All payment cycles",
                    "• All balances and history",
                    "• Encrypted preferences",
                    "• All cached data"
                )
                items.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Type DELETE to confirm:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it.uppercase() },
                    placeholder = { Text("DELETE") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmText == "DELETE",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

