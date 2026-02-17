package im.ilyk.ccoptimizer.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import im.ilyk.ccoptimizer.data.models.User

/**
 * Manages social authentication (Google Sign-In and Facebook Login)
 */
class SocialAuthManager(private val context: Context) {
    
    private val googleSignInClient: GoogleSignInClient
    private val facebookCallbackManager: CallbackManager = CallbackManager.Factory.create()
    
    init {
        // Initialize Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Initialize Facebook Login
        setupFacebookLogin()
    }
    
    /**
     * Set up Facebook login callback
     */
    private fun setupFacebookLogin() {
        LoginManager.getInstance().registerCallback(
            facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val accessToken = result.accessToken
                    val user = User(
                        id = accessToken.userId ?: "",
                        email = "", // Facebook doesn't provide email in basic login
                        name = null
                    )
                    onFacebookLoginSuccess?.invoke(user, accessToken.token)
                }
                
                override fun onCancel() {
                    onFacebookLoginError?.invoke("Facebook login was cancelled")
                }
                
                override fun onError(error: FacebookException) {
                    onFacebookLoginError?.invoke(error.message ?: "Facebook login failed")
                }
            }
        )
    }
    
    // Callbacks for Google Sign-In
    var onGoogleLoginSuccess: ((User, String) -> Unit)? = null
    var onGoogleLoginError: ((String) -> Unit)? = null
    
    // Callbacks for Facebook Login
    var onFacebookLoginSuccess: ((User, String) -> Unit)? = null
    var onFacebookLoginError: ((String) -> Unit)? = null
    
    /**
     * Create Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    /**
     * Handle Google Sign-In result
     */
    fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            if (account != null) {
                val user = User(
                    id = account.id ?: "",
                    email = account.email ?: "",
                    name = account.displayName
                )
                val idToken = account.idToken ?: ""
                onGoogleLoginSuccess?.invoke(user, idToken)
            }
        } catch (e: ApiException) {
            onGoogleLoginError?.invoke("Google Sign-In failed: ${e.message}")
        }
    }
    
    /**
     * Start Facebook login
     */
    fun startFacebookLogin(activity: Activity) {
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            listOf("public_profile", "email")
        )
    }
    
    /**
     * Handle Facebook login result
     */
    fun handleFacebookLoginResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }
    
    /**
     * Sign out from Google
     */
    fun signOutGoogle() {
        googleSignInClient.signOut()
    }
    
    /**
     * Sign out from Facebook
     */
    fun signOutFacebook() {
        LoginManager.getInstance().logOut()
    }
    
    /**
     * Sign out from all social providers
     */
    fun signOutAll() {
        signOutGoogle()
        signOutFacebook()
    }
    
    /**
     * Check if user is signed in with Google
     */
    fun isGoogleSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }
    
    /**
     * Check if user is signed in with Facebook
     */
    fun isFacebookSignedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }
    
    /**
     * Get current Google account
     */
    fun getCurrentGoogleAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Get current Facebook access token
     */
    fun getCurrentFacebookToken(): AccessToken? {
        return AccessToken.getCurrentAccessToken()
    }
}
