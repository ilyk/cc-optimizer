# Payment Reminder Notifications

Push notifications and calendar integration for payment reminders.

## Overview

The app provides payment reminders through:
- **Push Notifications**: Local notifications for upcoming payments
- **Calendar Integration**: Add payments to device calendar
- **Alarm Integration**: Set alarms for payment days

## Implementation

### Notification Service

```kotlin
class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    
    fun schedulePaymentReminder(recommendation: PaymentRecommendation) {
        val notification = createNotification(recommendation)
        val triggerTime = recommendation.recommendedPaymentDate.atStartOfDay()
        
        // Schedule notification
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTime.toEpochMilli(),
            createPendingIntent(recommendation)
        )
    }
    
    private fun createNotification(recommendation: PaymentRecommendation): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Payment Due: ${recommendation.creditCard.name}")
            .setContentText("Pay $${recommendation.recommendedAmount} today")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }
}
```

### Calendar Integration

```kotlin
fun addToCalendar(recommendation: PaymentRecommendation) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, 
            "Pay ${recommendation.creditCard.name}")
        putExtra(CalendarContract.Events.DESCRIPTION,
            "Payment amount: $${recommendation.recommendedAmount}\n" +
            recommendation.reasoning)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
            recommendation.recommendedPaymentDate.atStartOfDay().toEpochMilli())
        putExtra(CalendarContract.Events.ALL_DAY, true)
    }
    context.startActivity(intent)
}
```

### Permissions

Required permissions in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
```

### Permission Handling

```kotlin
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - schedule notifications
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

## Notification Channels

```kotlin
fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payment Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming credit card payments"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
```

## Graceful Fallback

If permissions denied:
```kotlin
fun schedulePaymentReminder(recommendation: PaymentRecommendation) {
    when {
        hasNotificationPermission() -> {
            // Send notification
            sendNotification(recommendation)
        }
        hasCalendarPermission() -> {
            // Add to calendar only
            addToCalendar(recommendation)
        }
        else -> {
            // Show in-app reminder
            showInAppReminder(recommendation)
        }
    }
}
```

---

**See Also**:
- [Android Notifications](https://developer.android.com/develop/ui/views/notifications)
- [Calendar Provider](https://developer.android.com/guide/topics/providers/calendar-provider)

