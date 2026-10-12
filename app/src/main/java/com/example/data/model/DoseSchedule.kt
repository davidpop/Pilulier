package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class DoseSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val hour: Int,      // 0..23
    val minute: Int     // 0..59
) {
    fun formattedTime(): String {
        return "%02dh%02d".format(hour, minute)
    }

    fun formattedColonTime(): String {
        return "%02d:%02d".format(hour, minute)
    }
}
