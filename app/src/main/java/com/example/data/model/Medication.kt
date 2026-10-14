package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "",            // ex: "500 mg", "1000 UI"
    val form: String = "",              // ex: "Comprimé", "Gélule", "Sirop", "Gouttes"
    val quantityPerDose: String = "",   // ex: "1 comprimé", "5 ml"
    val instructions: String = "",      // ex: "À prendre pendant le repas"
    val startDate: Long,                // Millis début de journée
    val endDate: Long? = null,          // Millis fin de journée (null si traitement continu/illimité)
    val isPaused: Boolean = false,      // Suspension temporaire des rappels
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
