package im.ilyk.ccoptimizer.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Manages permissions for notifications, calendar, and alarms
 * Gracefully degrades if permissions are denied
 */
class PermissionManager(private val activity: ComponentActivity) {
    
    private var onPermissionsResult: ((PermissionResult) -> Unit)? = null
    
    // Permission launcher
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val result = PermissionResult(
            notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission(),
            calendarGranted = (permissions[Manifest.permission.READ_CALENDAR] == true && 
                               permissions[Manifest.permission.WRITE_CALENDAR] == true),
            alarmsGranted = canScheduleExactAlarms()
        )
        onPermissionsResult?.invoke(result)
    }
    
    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are granted by default on Android < 13
            true
        }
    }
    
    /**
     * Check if calendar permissions are granted
     */
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if can schedule exact alarms
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // Can schedule exact alarms by default on Android < 12
            true
        }
    }
    
    /**
     * Request all permissions
     */
    fun requestAllPermissions(onResult: (PermissionResult) -> Unit) {
        this.onPermissionsResult = onResult
        
        val permissions = mutableListOf<String>()
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Calendar permissions
        permissions.add(Manifest.permission.READ_CALENDAR)
        permissions.add(Manifest.permission.WRITE_CALENDAR)
        
        // Note: SCHEDULE_EXACT_ALARM doesn't require runtime permission
        // but needs to be declared in manifest
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            // All permissions already granted
            onResult(getCurrentPermissionStatus())
        }
    }
    
    /**
     * Request only notification permission
     */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onPermissionsResult = { result ->
                onResult(result.notificationsGranted)
            }
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else {
            onResult(true)
        }
    }
    
    /**
     * Request only calendar permissions
     */
    fun requestCalendarPermission(onResult: (Boolean) -> Unit) {
        onPermissionsResult = { result ->
            onResult(result.calendarGranted)
        }
        permissionLauncher.launch(arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        ))
    }
    
    /**
     * Get current permission status
     */
    fun getCurrentPermissionStatus(): PermissionResult {
        return PermissionResult(
            notificationsGranted = hasNotificationPermission(),
            calendarGranted = hasCalendarPermission(),
            alarmsGranted = canScheduleExactAlarms()
        )
    }
    
    /**
     * Check if should show rationale
     */
    fun shouldShowRationale(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                return true
            }
        }
        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            return true
        }
        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
            return true
        }
        return false
    }
}

/**
 * Result of permission requests
 */
data class PermissionResult(
    val notificationsGranted: Boolean,
    val calendarGranted: Boolean,
    val alarmsGranted: Boolean
) {
    val hasAnyPermission: Boolean
        get() = notificationsGranted || calendarGranted || alarmsGranted
        
    val hasAllPermissions: Boolean
        get() = notificationsGranted && calendarGranted && alarmsGranted
}

