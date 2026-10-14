package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.DoseLogDao
import com.example.data.dao.MedicationDao
import com.example.data.model.DoseLog
import com.example.data.model.DoseSchedule
import com.example.data.model.Medication

@Database(
    entities = [
        Medication::class,
        DoseSchedule::class,
        DoseLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PilulierDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var INSTANCE: PilulierDatabase? = null

        fun getInstance(context: Context): PilulierDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PilulierDatabase::class.java,
                    "pilulier_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun setTestInstance(db: PilulierDatabase?) {
            INSTANCE = db
        }
    }
}
