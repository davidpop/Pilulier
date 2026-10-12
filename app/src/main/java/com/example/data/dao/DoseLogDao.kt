package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query("SELECT * FROM dose_logs WHERE scheduledDateTime >= :startMillis AND scheduledDateTime <= :endMillis ORDER BY scheduledDateTime ASC")
    fun getLogsBetween(startMillis: Long, endMillis: Long): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE scheduledDateTime >= :startMillis AND scheduledDateTime <= :endMillis ORDER BY scheduledDateTime ASC")
    suspend fun getLogsBetweenSync(startMillis: Long, endMillis: Long): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledDateTime = :scheduledDateTime LIMIT 1")
    suspend fun getLogForDose(medicationId: Long, scheduledDateTime: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): DoseLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(doseLog: DoseLog): Long

    @Update
    suspend fun update(doseLog: DoseLog)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND scheduledDateTime >= :afterMillis")
    suspend fun deleteFutureLogsForMedication(medicationId: Long, afterMillis: Long)

    @Query("SELECT * FROM dose_logs WHERE scheduledDateTime >= :fromMillis ORDER BY scheduledDateTime ASC")
    suspend fun getAllFutureLogsSync(fromMillis: Long): List<DoseLog>

    @Query("SELECT * FROM dose_logs ORDER BY scheduledDateTime DESC")
    fun getAllLogs(): Flow<List<DoseLog>>
}
