package im.ilyk.ccoptimizer.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.notifications.NotificationService
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for scheduling and sending payment reminder notifications
 *
 * This worker:
 * - Runs at scheduled times before payment due dates
 * - Sends notifications for upcoming payments
 * - Handles notification delivery even when app is closed
 *
 * Usage:
 *   PaymentReminderWorker.schedule(context, recommendation, notificationDate)
 */
class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationService = NotificationService.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            // Get payment details from input data
            val cardName = inputData.getString(KEY_CARD_NAME) ?: return Result.failure()
            val cardLastFour = inputData.getString(KEY_CARD_LAST_FOUR) ?: return Result.failure()
            val amount = inputData.getDouble(KEY_AMOUNT, 0.0)
            val dueDateString = inputData.getString(KEY_DUE_DATE) ?: return Result.failure()
            val daysUntilDue = inputData.getLong(KEY_DAYS_UNTIL_DUE, 0L)

            Timber.d("PaymentReminderWorker: Processing reminder for $cardName ending in $cardLastFour")

            // Reconstruct a simplified PaymentRecommendation
            // Note: In a real app, you might want to fetch the full recommendation from the database
            val dueDate = LocalDate.parse(dueDateString)

            // Create a minimal PaymentRecommendation for notification purposes
            // In production, you should fetch from Room database instead
            val recommendation = createMinimalRecommendation(
                cardName = cardName,
                cardLastFour = cardLastFour,
                amount = amount,
                dueDate = dueDate
            )

            // Send notification
            notificationService.sendPaymentReminder(recommendation)

            Timber.i("PaymentReminderWorker: Notification sent successfully for $cardName")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "PaymentReminderWorker: Failed to send payment reminder")

            // Retry on failure (up to the retry policy limit)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Create a minimal PaymentRecommendation for notification purposes
     * In production, fetch from database instead
     */
    private fun createMinimalRecommendation(
        cardName: String,
        cardLastFour: String,
        amount: Double,
        dueDate: LocalDate
    ): PaymentRecommendation {
        return PaymentRecommendation(
            creditCard = im.ilyk.ccoptimizer.data.models.CreditCard(
                id = cardLastFour, // Use last 4 as ID for notification purposes
                name = cardName,
                lastFourDigits = cardLastFour,
                bankName = cardName,
                statementGenerationDay = null,
                currentBalance = amount,
                availableCredit = 0.0,
                creditLimit = 0.0,
                institution = im.ilyk.ccoptimizer.data.models.BankInstitution.GENERIC,
                statementDueDate = dueDate,
                statementCloseDate = dueDate.minusDays(25)
            ),
            recommendedAmount = amount,
            recommendedPaymentDate = dueDate,
            statementDueDate = dueDate,
            daysUntilPayment = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), dueDate
            ).toInt(),
            reasoning = "Payment due"
        )
    }

    companion object {
        private const val KEY_CARD_NAME = "card_name"
        private const val KEY_CARD_LAST_FOUR = "card_last_four"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DUE_DATE = "due_date"
        private const val KEY_DAYS_UNTIL_DUE = "days_until_due"

        private const val WORK_NAME_PREFIX = "payment_reminder_"

        /**
         * Schedule a payment reminder notification
         *
         * @param context Application context
         * @param recommendation Payment recommendation to remind about
         * @param notificationDate Date when notification should be sent
         */
        fun schedule(
            context: Context,
            recommendation: PaymentRecommendation,
            notificationDate: LocalDate
        ) {
            val now = LocalDate.now()

            // Don't schedule past notifications
            if (notificationDate.isBefore(now)) {
                Timber.w("PaymentReminderWorker: Skipping past notification date $notificationDate")
                return
            }

            // Calculate delay until notification should be sent
            val delayDays = ChronoUnit.DAYS.between(now, notificationDate)
            val delayMillis = TimeUnit.DAYS.toMillis(delayDays)

            // Create unique work name based on card and notification date
            val workName = "$WORK_NAME_PREFIX${recommendation.creditCard.id}_$notificationDate"

            // Prepare input data
            val inputData = workDataOf(
                KEY_CARD_NAME to recommendation.creditCard.name,
                KEY_CARD_LAST_FOUR to recommendation.creditCard.lastFourDigits,
                KEY_AMOUNT to recommendation.recommendedAmount,
                KEY_DUE_DATE to recommendation.recommendedPaymentDate.toString(),
                KEY_DAYS_UNTIL_DUE to ChronoUnit.DAYS.between(
                    notificationDate,
                    recommendation.recommendedPaymentDate
                )
            )

            // Build work request with delay
            val workRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("payment_reminders")
                .addTag("card_${recommendation.creditCard.id}")
                .build()

            // Enqueue with REPLACE policy (if already scheduled, replace with new one)
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Timber.d(
                "PaymentReminderWorker: Scheduled notification for ${recommendation.creditCard.name} " +
                "on $notificationDate (in $delayDays days)"
            )
        }

        /**
         * Cancel all scheduled payment reminders
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("payment_reminders")
            Timber.d("PaymentReminderWorker: Cancelled all payment reminders")
        }

        /**
         * Cancel payment reminders for a specific card
         */
        fun cancelForCard(context: Context, cardId: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag("card_$cardId")
            Timber.d("PaymentReminderWorker: Cancelled reminders for card $cardId")
        }
    }
}
