package com.example.data.repository

import android.content.Context
import com.example.data.db.PilulierDatabase
import com.example.data.model.DoseItem
import com.example.data.model.DoseLog
import com.example.data.model.DoseSchedule
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.model.MedicationWithSchedules
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PilulierRepository(
    private val context: Context,
    private val database: PilulierDatabase
) {
    private val medicationDao = database.medicationDao()
    private val doseLogDao = database.doseLogDao()

    val allMedications: Flow<List<MedicationWithSchedules>> =
        medicationDao.getAllMedicationsWithSchedules()

    fun getMedicationById(id: Long): Flow<MedicationWithSchedules?> =
        medicationDao.getMedicationWithSchedulesById(id)

    suspend fun saveMedication(
        medication: Medication,
        schedules: List<DoseSchedule>
    ): Long = withContext(Dispatchers.IO) {
        val isUpdate = medication.id > 0

        if (isUpdate) {
            val oldMed = medicationDao.getMedicationWithSchedulesByIdSync(medication.id)
            if (oldMed != null) {
                AlarmScheduler.cancelAlarmsForMedication(context, oldMed)
            }
        }

        val medId = if (isUpdate) {
            medicationDao.updateMedication(medication)
            medicationDao.deleteSchedulesByMedicationId(medication.id)
            medication.id
        } else {
            medicationDao.insertMedication(medication)
        }

        val schedulesToInsert = schedules.map { it.copy(medicationId = medId) }
        medicationDao.insertSchedules(schedulesToInsert)

        val updatedMed = medicationDao.getMedicationWithSchedulesByIdSync(medId)
        if (updatedMed != null && !updatedMed.medication.isPaused) {
            AlarmScheduler.scheduleAlarmsForMedication(context, updatedMed)
        }

        medId
    }

    suspend fun setMedicationPaused(medicationId: Long, isPaused: Boolean) = withContext(Dispatchers.IO) {
        medicationDao.setPaused(medicationId, isPaused)
        val med = medicationDao.getMedicationWithSchedulesByIdSync(medicationId)
        if (med != null) {
            if (isPaused) {
                AlarmScheduler.cancelAlarmsForMedication(context, med)
            } else {
                AlarmScheduler.scheduleAlarmsForMedication(context, med)
            }
        }
    }

    suspend fun deleteMedication(
        item: MedicationWithSchedules,
        deleteHistory: Boolean = false
    ) = withContext(Dispatchers.IO) {
        // Annuler tous les rappels futurs
        AlarmScheduler.cancelAlarmsForMedication(context, item)

        if (deleteHistory) {
            // Supprimer également l'historique associé si l'utilisateur l'a explicitement demandé
            val allLogs = doseLogDao.getLogsBetweenSync(0L, Long.MAX_VALUE)
            for (log in allLogs) {
                if (log.medicationId == item.medication.id) {
                    doseLogDao.deleteById(log.id)
                }
            }
        } else {
            // Nettoyer uniquement les futurs logs non confirmés
            doseLogDao.deleteFutureLogsForMedication(item.medication.id, System.currentTimeMillis())
        }

        medicationDao.deleteMedication(item.medication)
    }

    fun getDosesForDate(date: LocalDate): Flow<List<DoseItem>> {
        val zone = ZoneId.systemDefault()
        val startOfDayMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDayMillis = date.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

        val logsFlow = doseLogDao.getLogsBetween(startOfDayMillis, endOfDayMillis)

        return combine(allMedications, logsFlow) { medications, logs ->
            val nowMillis = System.currentTimeMillis()
            val doseItems = mutableListOf<DoseItem>()
            val handledLogIds = mutableSetOf<Long>()

            for (medWithSched in medications) {
                val med = medWithSched.medication
                val medStart = Instant.ofEpochMilli(med.startDate).atZone(zone).toLocalDate()
                val medEnd = Instant.ofEpochMilli(med.endDate).atZone(zone).toLocalDate()

                // Vérifier si la date demandée est dans la période du traitement
                if (!date.isBefore(medStart) && !date.isAfter(medEnd)) {
                    for (sched in medWithSched.schedules) {
                        val scheduledDateTime = date.atTime(sched.hour, sched.minute)
                            .atZone(zone).toInstant().toEpochMilli()

                        val log = logs.firstOrNull {
                            it.medicationId == med.id && it.scheduledDateTime == scheduledDateTime
                        }

                        if (log != null) {
                            handledLogIds.add(log.id)
                        }

                        val status = when {
                            log?.status == "CONFIRMED" -> DoseStatus.CONFIRMED
                            scheduledDateTime < nowMillis -> DoseStatus.OVERDUE
                            else -> DoseStatus.PENDING
                        }

                        doseItems.add(
                            DoseItem(
                                logId = log?.id,
                                medicationId = med.id,
                                medicationName = med.name,
                                dosage = med.dosage,
                                form = med.form,
                                quantityPerDose = med.quantityPerDose,
                                instructions = med.instructions,
                                scheduledDateTime = scheduledDateTime,
                                status = status,
                                confirmedAt = log?.confirmedAt,
                                isSnoozed = log?.isSnoozed == true,
                                snoozedUntil = log?.snoozedUntil
                            )
                        )
                    }
                }
            }

            // Ajouter également les logs orphelins (médicaments supprimés mais dont l'historique est conservé)
            for (log in logs) {
                if (!handledLogIds.contains(log.id)) {
                    val status = when {
                        log.status == "CONFIRMED" -> DoseStatus.CONFIRMED
                        log.scheduledDateTime < nowMillis -> DoseStatus.OVERDUE
                        else -> DoseStatus.PENDING
                    }
                    doseItems.add(
                        DoseItem(
                            logId = log.id,
                            medicationId = log.medicationId,
                            medicationName = if (log.medicationName.isNotBlank()) log.medicationName else "Médicament archivé",
                            dosage = log.dosage,
                            form = "",
                            quantityPerDose = "",
                            instructions = "",
                            scheduledDateTime = log.scheduledDateTime,
                            status = status,
                            confirmedAt = log.confirmedAt,
                            isSnoozed = log.isSnoozed,
                            snoozedUntil = log.snoozedUntil
                        )
                    )
                }
            }

            // Trier chronologiquement par heure de prise prévue
            doseItems.sortedBy { it.scheduledDateTime }
        }
    }

    suspend fun markDoseTaken(
        medicationId: Long,
        scheduledDateTime: Long,
        medicationName: String,
        dosage: String
    ) = withContext(Dispatchers.IO) {
        val existingLog = doseLogDao.getLogForDose(medicationId, scheduledDateTime)
        val now = System.currentTimeMillis()
        val updatedLog = if (existingLog != null) {
            existingLog.copy(
                status = "CONFIRMED",
                confirmedAt = now,
                isSnoozed = false,
                snoozedUntil = null
            )
        } else {
            DoseLog(
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                scheduledDateTime = scheduledDateTime,
                status = "CONFIRMED",
                confirmedAt = now
            )
        }
        doseLogDao.insertOrReplace(updatedLog)

        // Annuler tout éventuel rappel différé actif pour cette dose
        AlarmScheduler.cancelDoseAlarm(context, medicationId, scheduledDateTime, isSnooze = true)
    }

    suspend fun unmarkDoseTaken(
        medicationId: Long,
        scheduledDateTime: Long
    ) = withContext(Dispatchers.IO) {
        val existingLog = doseLogDao.getLogForDose(medicationId, scheduledDateTime)
        if (existingLog != null) {
            val resetLog = existingLog.copy(
                status = "PENDING",
                confirmedAt = null
            )
            doseLogDao.insertOrReplace(resetLog)
        }
    }

    suspend fun snoozeDose(
        medicationId: Long,
        scheduledDateTime: Long,
        medicationName: String,
        dosage: String,
        snoozeMinutes: Int
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val triggerAtMillis = now + (snoozeMinutes * 60_000L)

        val existingLog = doseLogDao.getLogForDose(medicationId, scheduledDateTime)
        val updatedLog = if (existingLog != null) {
            existingLog.copy(
                isSnoozed = true,
                snoozedUntil = triggerAtMillis
            )
        } else {
            DoseLog(
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                scheduledDateTime = scheduledDateTime,
                status = "PENDING",
                isSnoozed = true,
                snoozedUntil = triggerAtMillis
            )
        }
        doseLogDao.insertOrReplace(updatedLog)

        AlarmScheduler.scheduleDoseAlarm(
            context = context,
            medicationId = medicationId,
            scheduledDateTime = scheduledDateTime,
            isSnooze = true,
            triggerAtMillis = triggerAtMillis
        )
    }
}
