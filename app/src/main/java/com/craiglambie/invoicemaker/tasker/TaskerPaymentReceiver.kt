package com.craiglambie.invoicemaker.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.craiglambie.invoicemaker.data.db.AppDatabase
import com.craiglambie.invoicemaker.repository.InvoiceRepository
import com.craiglambie.invoicemaker.repository.PaymentLogRepository
import kotlinx.coroutines.runBlocking

class TaskerPaymentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RECORD_PAYMENT) return

        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "AUD"
        val reference = intent.getStringExtra(EXTRA_REFERENCE) ?: ""
        val clientName = intent.getStringExtra(EXTRA_CLIENT_NAME) ?: ""
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""

        if (amount <= 0.0 || reference.isEmpty() || clientName.isEmpty()) {
            Log.w(TAG, "Invalid payment parameters: amount=$amount, reference=$reference, clientName=$clientName")
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<ProcessPaymentWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putDouble(EXTRA_AMOUNT, amount)
                    .putString(EXTRA_CURRENCY, currency)
                    .putString(EXTRA_REFERENCE, reference)
                    .putString(EXTRA_CLIENT_NAME, clientName)
                    .putString(EXTRA_NOTES, notes)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Payment processing queued: $amount $currency from $clientName (ref: $reference)")
    }

    companion object {
        private const val TAG = "TaskerPaymentReceiver"
        const val ACTION_RECORD_PAYMENT = "com.craiglambie.invoicemaker.RECORD_PAYMENT"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_CURRENCY = "currency"
        const val EXTRA_REFERENCE = "reference"
        const val EXTRA_CLIENT_NAME = "clientName"
        const val EXTRA_NOTES = "notes"
    }
}

class ProcessPaymentWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = runBlocking {
        return@runBlocking try {
            val amount = inputData.getDouble(TaskerPaymentReceiver.EXTRA_AMOUNT, 0.0)
            val currency = inputData.getString(TaskerPaymentReceiver.EXTRA_CURRENCY) ?: "AUD"
            val reference = inputData.getString(TaskerPaymentReceiver.EXTRA_REFERENCE) ?: ""
            val clientName = inputData.getString(TaskerPaymentReceiver.EXTRA_CLIENT_NAME) ?: ""
            val notes = inputData.getString(TaskerPaymentReceiver.EXTRA_NOTES) ?: ""

            val db = AppDatabase.get(applicationContext)
            val invoiceRepository = InvoiceRepository(db, db.invoiceDao(), db.invoiceLineItemDao())
            val paymentLogRepository = PaymentLogRepository(db, db.paymentLogDao(), db.invoiceDao(), invoiceRepository)

            val result = paymentLogRepository.processTaskerPayment(
                amount = amount,
                currency = currency,
                reference = reference,
                clientName = clientName,
                notes = notes
            )

            Log.d("ProcessPaymentWorker", "Payment processed: ${result.message}")
            Result.success()
        } catch (e: Exception) {
            Log.e("ProcessPaymentWorker", "Failed to process payment", e)
            Result.retry()
        }
    }
}
