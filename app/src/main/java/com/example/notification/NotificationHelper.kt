package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {

    const val CHANNEL_REMINDERS_ID = "pilulier_reminders_channel"
    const val CHANNEL_REMINDERS_NAME = "Rappels de médicaments"

    const val ACTION_MARK_TAKEN = "com.aistudio.pilulier.ACTION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.aistudio.pilulier.ACTION_SNOOZE"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour la prise ponctuelle de vos médicaments"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showDoseNotification(
        context: Context,
        notificationId: Int,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        quantityPerDose: String,
        instructions: String,
        scheduledDateTime: Long,
        formattedTime: String,
        isSnooze: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent d'ouverture de l'application
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent Action "Pris"
        val markTakenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledDateTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent Action "Rappeler dans 10 min"
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledDateTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSnooze) {
            "Rappel différé : $medicationName"
        } else {
            "Prise prévue : $medicationName"
        } + if (dosage.isNotBlank()) " ($dosage)" else ""

        val subtitleBuilder = StringBuilder()
        subtitleBuilder.append("C'est l'heure de votre prise")
        if (formattedTime.isNotBlank()) {
            subtitleBuilder.append(" de $formattedTime")
        }
        if (quantityPerDose.isNotBlank()) {
            subtitleBuilder.append(" : $quantityPerDose")
        }

        val bigText = buildString {
            append(subtitleBuilder.toString())
            if (instructions.isNotBlank()) {
                append("\nConseil : $instructions")
            }
            append("\n\nAppuyez sur « Pris » dès que vous avez pris votre médicament.")
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitleBuilder.toString())
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Pris",
                takenPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                "Rappeler dans 10 min",
                snoozePendingIntent
            )

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
