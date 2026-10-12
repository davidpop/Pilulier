package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.PilulierDatabase
import com.example.data.model.DoseLog
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val scheduledDateTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, -1L)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        if (medicationId == -1L || scheduledDateTime == -1L) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PilulierDatabase.getInstance(context)
                val medWithSchedules = db.medicationDao().getMedicationWithSchedulesByIdSync(medicationId)

                if (medWithSchedules != null && !medWithSchedules.medication.isPaused) {
                    val med = medWithSchedules.medication
                    val now = System.currentTimeMillis()

                    // Vérifier si le traitement n'est pas expiré
                    if (now in (med.startDate - 86_400_000L)..(med.endDate + 86_400_000L)) {
                        // Vérifier si la prise n'a pas déjà été marquée comme prise
                        val existingLog = db.doseLogDao().getLogForDose(medicationId, scheduledDateTime)
                        if (existingLog?.status != "CONFIRMED") {
                            // Enregistrer le statut initial si absent
                            if (existingLog == null) {
                                db.doseLogDao().insertOrReplace(
                                    DoseLog(
                                        medicationId = medicationId,
                                        medicationName = med.name,
                                        dosage = med.dosage,
                                        scheduledDateTime = scheduledDateTime,
                                        status = "PENDING"
                                    )
                                )
                            }

                            val notificationId = AlarmScheduler.getRequestCode(medicationId, scheduledDateTime, isSnooze)

                            val zone = ZoneId.systemDefault()
                            val localTime = Instant.ofEpochMilli(scheduledDateTime).atZone(zone).toLocalTime()
                            val formattedTime = localTime.format(DateTimeFormatter.ofPattern("HH'h'mm"))

                            NotificationHelper.showDoseNotification(
                                context = context,
                                notificationId = notificationId,
                                medicationId = medicationId,
                                medicationName = med.name,
                                dosage = med.dosage,
                                quantityPerDose = med.quantityPerDose,
                                instructions = med.instructions,
                                scheduledDateTime = scheduledDateTime,
                                formattedTime = formattedTime,
                                isSnooze = isSnooze
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
