package im.ilyk.ccoptimizer.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import im.ilyk.ccoptimizer.data.repository.AuthRepository
import im.ilyk.ccoptimizer.data.models.User
import im.ilyk.ccoptimizer.auth.SocialAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository(application.applicationContext)
    private val socialAuthManager = SocialAuthManager(application.applicationContext)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthenticationStatus()
        setupSocialAuthCallbacks()
    }
    
    private fun setupSocialAuthCallbacks() {
        socialAuthManager.onGoogleLoginSuccess = { user, idToken ->
            // Handle Google login success
            // TODO: Send idToken to backend for verification
            _authState.value = AuthState.Authenticated(user)
        }
        
        socialAuthManager.onGoogleLoginError = { error ->
            _authState.value = AuthState.Error(error)
        }
        
        socialAuthManager.onFacebookLoginSuccess = { user, accessToken ->
            // Handle Facebook login success
            // TODO: Send accessToken to backend for verification
            _authState.value = AuthState.Authenticated(user)
        }
        
        socialAuthManager.onFacebookLoginError = { error ->
            _authState.value = AuthState.Error(error)
        }
    }
    
    private fun checkAuthenticationStatus() {
        if (authRepository.isAuthenticated()) {
            val userInfo = authRepository.getCurrentUser()
            if (userInfo != null) {
                val user = User(
                    id = userInfo.first,
                    email = userInfo.second, // This is now nullable but stored value should always exist
                    name = null // We don't store name in current implementation
                )
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.NotAuthenticated
            }
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.login(email, password).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }
    
    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.register(email, password, name).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }
    
    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.NotAuthenticated
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.NotAuthenticated
        }
    }
    
    /**
     * Get Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent {
        return socialAuthManager.getGoogleSignInIntent()
    }
    
    /**
     * Handle Google Sign-In result
     */
    fun handleGoogleSignInResult(data: Intent?) {
        socialAuthManager.handleGoogleSignInResult(data)
    }
    
    /**
     * Start Facebook login
     */
    fun startFacebookLogin(activity: androidx.activity.ComponentActivity) {
        socialAuthManager.startFacebookLogin(activity)
    }
    
    /**
     * Handle Facebook login result
     */
    fun handleFacebookLoginResult(requestCode: Int, resultCode: Int, data: Intent?) {
        socialAuthManager.handleFacebookLoginResult(requestCode, resultCode, data)
    }
    
    /**
     * Sign out from all providers
     */
    fun signOutAll() {
        authRepository.logout()
        socialAuthManager.signOutAll()
        _authState.value = AuthState.NotAuthenticated
    }
}
