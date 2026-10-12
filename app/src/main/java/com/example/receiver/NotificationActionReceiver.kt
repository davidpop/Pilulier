package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.PilulierDatabase
import com.example.data.model.DoseLog
import com.example.data.repository.UserPreferencesRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val scheduledDateTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (medicationId == -1L || scheduledDateTime == -1L) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PilulierDatabase.getInstance(context)

                when (action) {
                    NotificationHelper.ACTION_MARK_TAKEN -> {
                        // Annuler la notification
                        if (notificationId != -1) {
                            NotificationHelper.cancelNotification(context, notificationId)
                        }

                        // Enregistrer la prise confirmée avec heure réelle
                        val existingLog = db.doseLogDao().getLogForDose(medicationId, scheduledDateTime)
                        val medInfo = if (existingLog == null) {
                            db.medicationDao().getMedicationWithSchedulesByIdSync(medicationId)?.medication
                        } else null

                        val confirmedLog = if (existingLog != null) {
                            existingLog.copy(
                                status = "CONFIRMED",
                                confirmedAt = System.currentTimeMillis(),
                                isSnoozed = false,
                                snoozedUntil = null
                            )
                        } else {
                            DoseLog(
                                medicationId = medicationId,
                                medicationName = medInfo?.name ?: "",
                                dosage = medInfo?.dosage ?: "",
                                scheduledDateTime = scheduledDateTime,
                                status = "CONFIRMED",
                                confirmedAt = System.currentTimeMillis()
                            )
                        }
                        db.doseLogDao().insertOrReplace(confirmedLog)

                        // Annuler également les éventuels rappels snoozés pour cette dose
                        AlarmScheduler.cancelDoseAlarm(context, medicationId, scheduledDateTime, isSnooze = true)
                    }

                    NotificationHelper.ACTION_SNOOZE -> {
                        // Fermer la notification actuelle
                        if (notificationId != -1) {
                            NotificationHelper.cancelNotification(context, notificationId)
                        }

                        val prefsRepo = UserPreferencesRepository(context)
                        val snoozeMinutes = try {
                            prefsRepo.snoozeMinutes.first()
                        } catch (e: Exception) {
                            10
                        }

                        val now = System.currentTimeMillis()
                        val snoozeTriggerTime = now + (snoozeMinutes * 60_000L)

                        // Mettre à jour le log en base
                        val existingLog = db.doseLogDao().getLogForDose(medicationId, scheduledDateTime)
                        val updatedLog = if (existingLog != null) {
                            existingLog.copy(
                                isSnoozed = true,
                                snoozedUntil = snoozeTriggerTime
                            )
                        } else {
                            DoseLog(
                                medicationId = medicationId,
                                scheduledDateTime = scheduledDateTime,
                                status = "PENDING",
                                isSnoozed = true,
                                snoozedUntil = snoozeTriggerTime
                            )
                        }
                        db.doseLogDao().insertOrReplace(updatedLog)

                        // Reprogrammer l'alarme exacte pour le délai choisi
                        AlarmScheduler.scheduleDoseAlarm(
                            context = context,
                            medicationId = medicationId,
                            scheduledDateTime = scheduledDateTime,
                            isSnooze = true,
                            triggerAtMillis = snoozeTriggerTime
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationAction", "Error handling notification action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
