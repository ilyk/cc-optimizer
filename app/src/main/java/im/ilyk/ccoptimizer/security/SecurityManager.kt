package im.ilyk.ccoptimizer.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Central security manager for app security checks
 */
object SecurityManager {
    
    /**
     * Performs comprehensive security checks
     * Returns list of security issues found
     */
    fun performSecurityChecks(context: Context): List<String> {
        val issues = mutableListOf<String>()
        
        if (isRooted()) {
            issues.add("Device is rooted - security compromised")
        }
        
        if (isAppTampered(context)) {
            issues.add("App integrity check failed - app may be modified")
        }
        
        if (isDebuggerAttached()) {
            issues.add("Debugger detected - potential security threat")
        }
        
        if (isEmulator()) {
            issues.add("Running on emulator - increased security risk")
        }
        
        return issues
    }
    
    /**
     * Check if device is rooted
     * Multiple detection methods for better coverage
     */
    fun isRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }
    
    // Method 1: Check for common root binaries
    private fun checkRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }
    
    // Method 2: Check for root management apps
    private fun checkRootMethod2(): Boolean {
        val packages = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk"
        )
        
        for (packageName in packages) {
            try {
                Class.forName(packageName)
                return true
            } catch (e: ClassNotFoundException) {
                // Package not found, continue
            }
        }
        return false
    }
    
    // Method 3: Check for dangerous system properties
    private fun checkRootMethod3(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    /**
     * Check if app has been tampered with
     * Compares app signature against known good signature
     */
    fun isAppTampered(context: Context): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures == null || signatures.isEmpty()) {
                return true // No signature = tampered
            }
            
            // Calculate signature hash
            val signature = signatures[0]
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature.toByteArray())
            val signatureHash = digest.joinToString("") { "%02x".format(it) }
            
            // TODO: In production, compare against your actual release signature hash
            // For now, we just check if signature exists
            // val KNOWN_GOOD_SIGNATURE = "your_release_signature_sha256_hash_here"
            // return signatureHash != KNOWN_GOOD_SIGNATURE
            
            return false
        } catch (e: Exception) {
            // Error checking signature = assume tampered
            return true
        }
    }
    
    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
    }
    
    /**
     * Check if running on emulator
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
}

