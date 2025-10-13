package im.ilyk.ccoptimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import im.ilyk.ccoptimizer.ui.screens.MainScreen
import im.ilyk.ccoptimizer.ui.theme.CreditCardOptimizerTheme
import im.ilyk.ccoptimizer.ui.theme.ThemePreferences
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import com.plaid.link.OpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private var plaidHandler: PlaidHandler? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Collect theme preference (default: Auto)
            val themePreferences = ThemePreferences.getInstance(this)
            val currentTheme by themePreferences.themeMode.collectAsState(initial = ThemePreferences.THEME_AUTO)
            
            CreditCardOptimizerTheme(themeMode = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val linkToken by viewModel.linkToken.collectAsState()
                    
                    MainScreen(
                        viewModel = viewModel,
                        onConnectPlaid = {
                            viewModel.initiatePlaidLink()
                        }
                    )
                    
                    // Launch Plaid Link when token is available
                    LaunchedEffect(linkToken) {
                        linkToken?.let { token ->
                            openPlaidLink(token)
                        }
                    }
                }
            }
        }
    }
    
    private fun openPlaidLink(linkToken: String) {
        val config = linkTokenConfiguration {
            this.token = linkToken
        }
        
        plaidHandler = Plaid.create(
            application = application,
            linkTokenConfiguration = config
        )
        
        val openPlaidLink = registerForActivityResult(OpenPlaidLink()) { result ->
            when (result) {
                is LinkSuccess -> {
                    // Successfully linked account
                    viewModel.onPlaidSuccess(result.publicToken)
                }
                is LinkExit -> {
                    // User exited without linking
                    if (result.error != null) {
                        // Handle error
                        println("Plaid Link Error: ${result.error.displayMessage}")
                    }
                }
            }
        }
        
        openPlaidLink.launch(plaidHandler!!)
    }
}

