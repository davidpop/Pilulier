package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DoseSchedule
import com.example.data.model.Medication
import com.example.data.model.MedicationWithSchedules
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Transaction
    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getAllMedicationsWithSchedules(): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationWithSchedulesById(id: Long): Flow<MedicationWithSchedules?>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationWithSchedulesByIdSync(id: Long): MedicationWithSchedules?

    @Transaction
    @Query("SELECT * FROM medications WHERE isPaused = 0 AND (endDate IS NULL OR endDate >= :nowMillis)")
    suspend fun getActiveFutureMedicationsSync(nowMillis: Long): List<MedicationWithSchedules>

    @Transaction
    @Query("SELECT * FROM medications")
    suspend fun getAllMedicationsSync(): List<MedicationWithSchedules>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<DoseSchedule>)

    @Query("DELETE FROM dose_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesByMedicationId(medicationId: Long)

    @Query("UPDATE medications SET isPaused = :isPaused, updatedAt = :updatedAt WHERE id = :medicationId")
    suspend fun setPaused(medicationId: Long, isPaused: Boolean, updatedAt: Long = System.currentTimeMillis())
}
