package com.example.data.model

enum class DoseStatus(val label: String) {
    PENDING("À venir"),
    CONFIRMED("Prise confirmée"),
    OVERDUE("Non confirmée");

    companion object {
        fun fromString(value: String): DoseStatus {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                PENDING
            }
        }
    }
}
