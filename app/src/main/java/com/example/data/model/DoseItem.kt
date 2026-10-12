package com.example.data.model

data class DoseItem(
    val logId: Long?,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val form: String,
    val quantityPerDose: String,
    val instructions: String,
    val scheduledDateTime: Long,
    val status: DoseStatus,
    val confirmedAt: Long?,
    val isSnoozed: Boolean,
    val snoozedUntil: Long?
)
