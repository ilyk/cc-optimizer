package im.ilyk.ccoptimizer.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.ui.viewmodel.ExportState
import im.ilyk.ccoptimizer.ui.viewmodel.ExportViewModel

/**
 * Export menu component
 * 
 * Provides UI for exporting credit card data to CSV
 */
@Composable
fun ExportMenuItem(
    viewModel: ExportViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    
    // Handle export completion
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            val state = exportState as ExportState.Success
            val shareIntent = viewModel.createShareIntent(context, state.uri)
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${state.filename}")
            )
            // Reset state after sharing
            kotlinx.coroutines.delay(500)
            viewModel.resetState()
        }
    }
    
    when (val state = exportState) {
        is ExportState.Idle -> {
            TextButton(
                onClick = { viewModel.exportToCsv(context) },
                modifier = modifier.semantics {
                    contentDescription = "Export your data as CSV"
                }
            ) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export data (CSV)")
            }
        }
        
        is ExportState.Exporting -> {
            Row(
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Exporting...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        is ExportState.Success -> {
            // Will trigger share dialog via LaunchedEffect
            Row(
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sharing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        is ExportState.Error -> {
            Column(
                modifier = modifier.padding(16.dp)
            ) {
                Text(
                    "Export failed: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.exportToCsv(context) }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Export button that can be placed in a top bar or menu
 */
@Composable
fun ExportButton(
    viewModel: ExportViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    
    IconButton(
        onClick = { viewModel.exportToCsv(context) },
        enabled = exportState is ExportState.Idle,
        modifier = modifier.semantics {
            contentDescription = "Export your data as CSV"
        }
    ) {
        when (exportState) {
            is ExportState.Exporting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = "Export"
                )
            }
        }
    }
    
    // Handle share dialog
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            val state = exportState as ExportState.Success
            val shareIntent = viewModel.createShareIntent(context, state.uri)
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${state.filename}")
            )
            kotlinx.coroutines.delay(500)
            viewModel.resetState()
        }
    }
}


