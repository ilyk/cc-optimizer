package im.ilyk.ccoptimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import im.ilyk.ccoptimizer.ui.screens.MainScreen
import im.ilyk.ccoptimizer.ui.screens.AuthScreen
import im.ilyk.ccoptimizer.ui.theme.CreditCardOptimizerTheme
import im.ilyk.ccoptimizer.ui.theme.ThemePreferences
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.AuthViewModel
import im.ilyk.ccoptimizer.ui.navigation.AppNavigation
import im.ilyk.ccoptimizer.ui.preferences.OnboardingPreferences
import kotlinx.coroutines.launch
import com.plaid.link.OpenPlaidLink
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: MainViewModel by viewModels()

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        authViewModel.handleGoogleSignInResult(result.data)
    }

    private val openPlaidLink = registerForActivityResult(OpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> {
                // Successfully linked account
                viewModel.onPlaidSuccess(result.publicToken)
                // Clear the link token to prevent re-launching
                viewModel.clearLinkToken()
            }

            is LinkExit -> {
                // User exited without linking
                viewModel.clearLinkToken()
                result.error?.let { error ->
                    // Handle error
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("MainActivity", "Plaid Link Error: ${error.displayMessage}")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge (status bar will match app theme)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle OAuth redirect on initial launch
        handlePlaidOAuthRedirect(intent)

        setContent {
            // Collect theme preference (default: Auto)
            val themePreferences = ThemePreferences.getInstance(this)
            val currentTheme by themePreferences.themeMode.collectAsState(initial = ThemePreferences.THEME_AUTO)

            CreditCardOptimizerTheme(themeMode = currentTheme) {
                // Update system bars to match theme
                val statusBarColor = MaterialTheme.colorScheme.primaryContainer
                val navigationBarColor = MaterialTheme.colorScheme.background

                SideEffect {
                    window.statusBarColor = statusBarColor.toArgb()
                    window.navigationBarColor = navigationBarColor.toArgb()

                    // Make status bar icons dark/light based on theme
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !currentTheme.contains("dark")
                        isAppearanceLightNavigationBars = !currentTheme.contains("dark")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val authState by authViewModel.authState.collectAsState()
                    val linkToken by viewModel.linkToken.collectAsState()
                    val onboardingPreferences = OnboardingPreferences.getInstance(this@MainActivity)
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                    // Use a more reliable state management approach
                    var hasCompletedOnboarding by remember { mutableStateOf(false) }
                    var isOnboardingStateLoaded by remember { mutableStateOf(false) }

                    // Load onboarding state on first composition
                    LaunchedEffect(Unit) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("MainActivity", "Loading onboarding state from DataStore")
                        }
                        onboardingPreferences.hasCompletedOnboarding.collect { value ->
                            hasCompletedOnboarding = value
                            isOnboardingStateLoaded = true
                            if (BuildConfig.DEBUG) {
                                android.util.Log.d("MainActivity", "Onboarding state loaded: $value")
                            }
                        }
                    }

                    when {
                        // Show loading screen until we've loaded the onboarding state
                        !isOnboardingStateLoaded -> {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator()
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                                    androidx.compose.material3.Text(
                                        text = "Loading app...",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }

                        // Show main content after onboarding state is loaded
                        else -> {
                            when (val currentAuthState = authState) {
                                is im.ilyk.ccoptimizer.ui.viewmodel.AuthState.Loading -> {
                                    // Show loading while checking authentication
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = androidx.compose.ui.Alignment.Center,
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator()
                                    }
                                }

                                is im.ilyk.ccoptimizer.ui.viewmodel.AuthState.NotAuthenticated -> {
                                    if (BuildConfig.DEBUG) {
                                        // Demo mode: bypass authentication
                                        AppNavigation(
                                            viewModel = viewModel,
                                            onConnectPlaid = {
                                                viewModel.initiatePlaidLink()
                                            },
                                            hasCompletedOnboarding = hasCompletedOnboarding,
                                            onOnboardingComplete = {
                                                // Mark onboarding as completed
                                                if (BuildConfig.DEBUG) {
                                                    android.util.Log.d(
                                                        "MainActivity",
                                                        "onOnboardingComplete called - saving to DataStore",
                                                    )
                                                }
                                                hasCompletedOnboarding = true // Update local state immediately
                                                coroutineScope.launch {
                                                    onboardingPreferences.setOnboardingCompleted()
                                                    if (BuildConfig.DEBUG) {
                                                        android.util.Log.d(
                                                            "MainActivity",
                                                            "onOnboardingCompleted saved to DataStore",
                                                        )
                                                    }
                                                }
                                            },
                                            onResetOnboarding = {
                                                if (BuildConfig.DEBUG) {
                                                    android.util.Log.d(
                                                        "MainActivity",
                                                        "onResetOnboarding called - resetting DataStore",
                                                    )
                                                }
                                                hasCompletedOnboarding = false // Update local state immediately
                                                coroutineScope.launch {
                                                    onboardingPreferences.resetOnboarding()
                                                    if (BuildConfig.DEBUG) {
                                                        android.util.Log.d("MainActivity", "onboarding reset in DataStore")
                                                    }
                                                }
                                            },
                                        )
                                    } else {
                                        // Production: show auth screen
                                        AuthScreen(
                                            onLogin = { email, password ->
                                                authViewModel.login(email, password)
                                            },
                                            onRegister = { email, password, name ->
                                                authViewModel.register(email, password, name)
                                            },
                                            onGoogleLogin = {
                                                googleSignInLauncher.launch(authViewModel.getGoogleSignInIntent())
                                            },
                                            onFacebookLogin = {
                                                authViewModel.startFacebookLogin(this@MainActivity)
                                            },
                                            isLoading = false,
                                            errorMessage = null,
                                        )
                                    }
                                }

                                is im.ilyk.ccoptimizer.ui.viewmodel.AuthState.Authenticated -> {
                                    AppNavigation(
                                        viewModel = viewModel,
                                        onConnectPlaid = {
                                            viewModel.initiatePlaidLink()
                                        },
                                        hasCompletedOnboarding = hasCompletedOnboarding,
                                        onOnboardingComplete = {
                                            // Mark onboarding as completed
                                            if (BuildConfig.DEBUG) {
                                                android.util.Log.d(
                                                    "MainActivity",
                                                    "onOnboardingComplete called - saving to DataStore",
                                                )
                                            }
                                            hasCompletedOnboarding = true // Update local state immediately
                                            coroutineScope.launch {
                                                onboardingPreferences.setOnboardingCompleted()
                                                if (BuildConfig.DEBUG) {
                                                    android.util.Log.d(
                                                        "MainActivity",
                                                        "onOnboardingCompleted saved to DataStore",
                                                    )
                                                }
                                            }
                                        },
                                        onResetOnboarding = {
                                            if (BuildConfig.DEBUG) {
                                                android.util.Log.d(
                                                    "MainActivity",
                                                    "onResetOnboarding called - resetting DataStore",
                                                )
                                            }
                                            hasCompletedOnboarding = false // Update local state immediately
                                            coroutineScope.launch {
                                                onboardingPreferences.resetOnboarding()
                                                if (BuildConfig.DEBUG) {
                                                    android.util.Log.d("MainActivity", "onboarding reset in DataStore")
                                                }
                                            }
                                        },
                                    )

                                    // Launch Plaid Link when token is available
                                    LaunchedEffect(linkToken) {
                                        linkToken?.let { token ->
                                            openPlaidLink(token)
                                        }
                                    }
                                }

                                is im.ilyk.ccoptimizer.ui.viewmodel.AuthState.Error -> {
                                    AuthScreen(
                                        onLogin = { email, password ->
                                            authViewModel.login(email, password)
                                        },
                                        onRegister = { email, password, name ->
                                            authViewModel.register(email, password, name)
                                        },
                                        onGoogleLogin = {
                                            googleSignInLauncher.launch(authViewModel.getGoogleSignInIntent())
                                        },
                                        onFacebookLogin = {
                                            authViewModel.startFacebookLogin(this@MainActivity)
                                        },
                                        isLoading = false,
                                        errorMessage = currentAuthState.message,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openPlaidLink(linkToken: String) {
        val config = linkTokenConfiguration {
            this.token = linkToken
            // IMPORTANT: This tells the Plaid SDK to expect OAuth redirects
            // via the custom scheme we defined in AndroidManifest (plaidlink://oauth)
            // The SDK will automatically handle the redirect when it comes back
        }

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MainActivity", "Launching Plaid Link with token: ${linkToken.take(20)}...")
        }
        openPlaidLink.launch(config)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle OAuth redirect when app is already running
        handlePlaidOAuthRedirect(intent)
    }

    private fun handlePlaidOAuthRedirect(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "plaidlink" && uri.host == "oauth") {
                // Log the OAuth redirect for debugging
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MainActivity", "Plaid OAuth redirect received: $uri")
                }

                // The Plaid SDK will automatically handle this via the intent-filter
                // We just need to make sure the activity is in singleTask mode (already configured)

                // Check for error in the redirect
                uri.getQueryParameter("error")?.let { error ->
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("MainActivity", "Plaid OAuth error: $error")
                    }
                    viewModel.clearLinkToken()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle Facebook login result
        authViewModel.handleFacebookLoginResult(requestCode, resultCode, data)
    }
}

