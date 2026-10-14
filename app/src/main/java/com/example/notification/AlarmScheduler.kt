package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.PilulierDatabase
import com.example.data.model.MedicationWithSchedules
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val ACTION_ALARM_DOSE = "com.aistudio.pilulier.ACTION_ALARM_DOSE"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"

    fun getRequestCode(medicationId: Long, scheduledDateTime: Long, isSnooze: Boolean = false): Int {
        val base = ((medicationId.hashCode() * 31) + (scheduledDateTime / 60_000L).hashCode()) and 0x3FFFFFFF
        return if (isSnooze) base + 0x40000000 else base
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleDoseAlarm(
        context: Context,
        medicationId: Long,
        scheduledDateTime: Long,
        isSnooze: Boolean = false,
        triggerAtMillis: Long = scheduledDateTime
    ) {
        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_DOSE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledDateTime)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }

        val requestCode = getRequestCode(medicationId, scheduledDateTime, isSnooze)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback failed: ${fallbackEx.message}")
            }
        }
    }

    fun cancelDoseAlarm(
        context: Context,
        medicationId: Long,
        scheduledDateTime: Long,
        isSnooze: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_DOSE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledDateTime)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val requestCode = getRequestCode(medicationId, scheduledDateTime, isSnooze)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun scheduleAlarmsForMedication(
        context: Context,
        item: MedicationWithSchedules
    ) = withContext(Dispatchers.Default) {
        if (item.medication.isPaused) {
            return@withContext
        }

        val nowMillis = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        val startLocalDate = Instant.ofEpochMilli(item.medication.startDate)
            .atZone(zone).toLocalDate()
        val endLocalDate = item.medication.endDate?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        } ?: run {
            val today = LocalDate.now(zone)
            val baseDate = if (today.isAfter(startLocalDate)) today else startLocalDate
            baseDate.plusDays(30)
        }

        var currentDay = startLocalDate
        while (!currentDay.isAfter(endLocalDate)) {
            for (schedule in item.schedules) {
                val doseDateTime = currentDay.atTime(schedule.hour, schedule.minute)
                    .atZone(zone).toInstant().toEpochMilli()

                if (doseDateTime > nowMillis) {
                    scheduleDoseAlarm(
                        context = context,
                        medicationId = item.medication.id,
                        scheduledDateTime = doseDateTime,
                        isSnooze = false,
                        triggerAtMillis = doseDateTime
                    )
                }
            }
            currentDay = currentDay.plusDays(1)
        }
    }

    suspend fun cancelAlarmsForMedication(
        context: Context,
        item: MedicationWithSchedules
    ) = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val startLocalDate = Instant.ofEpochMilli(item.medication.startDate)
            .atZone(zone).toLocalDate()
        val endLocalDate = item.medication.endDate?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        } ?: run {
            val today = LocalDate.now(zone)
            val baseDate = if (today.isAfter(startLocalDate)) today else startLocalDate
            baseDate.plusDays(30)
        }

        var currentDay = startLocalDate
        while (!currentDay.isAfter(endLocalDate)) {
            for (schedule in item.schedules) {
                val doseDateTime = currentDay.atTime(schedule.hour, schedule.minute)
                    .atZone(zone).toInstant().toEpochMilli()

                cancelDoseAlarm(context, item.medication.id, doseDateTime, isSnooze = false)
                cancelDoseAlarm(context, item.medication.id, doseDateTime, isSnooze = true)
            }
            currentDay = currentDay.plusDays(1)
        }
    }

    suspend fun rescheduleAllActiveAlarms(context: Context) = withContext(Dispatchers.IO) {
        val db = PilulierDatabase.getInstance(context)
        val nowMillis = System.currentTimeMillis()
        val activeMeds = db.medicationDao().getActiveFutureMedicationsSync(nowMillis)
        for (med in activeMeds) {
            scheduleAlarmsForMedication(context, med)
        }
    }
}
