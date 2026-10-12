package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["scheduledDateTime"]),
        Index(value = ["medicationId", "scheduledDateTime"], unique = true)
    ]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String = "",    // Conserve le nom pour l'historique même si le traitement est supprimé
    val dosage: String = "",            // Conserve le dosage
    val scheduledDateTime: Long,        // Millis UTC de l'heure programmée
    val status: String,                 // "CONFIRMED", "PENDING", "OVERDUE"
    val confirmedAt: Long? = null,      // Date et heure réelles de confirmation
    val isSnoozed: Boolean = false,
    val snoozedUntil: Long? = null
)
