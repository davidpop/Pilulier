package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicationWithSchedules(
    @Embedded
    val medication: Medication,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId"
    )
    val schedules: List<DoseSchedule>
) {
    fun sortedSchedules(): List<DoseSchedule> {
        return schedules.sortedWith(compareBy({ it.hour }, { it.minute }))
    }
}
