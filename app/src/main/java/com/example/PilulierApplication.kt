package com.example

import android.app.Application
import com.example.data.db.PilulierDatabase
import com.example.data.repository.PilulierRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.notification.NotificationHelper

class PilulierApplication : Application() {

    lateinit var database: PilulierDatabase
        private set

    lateinit var repository: PilulierRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = PilulierDatabase.getInstance(this)
        repository = PilulierRepository(this, database)
        preferencesRepository = UserPreferencesRepository(this)

        // Initialisation des canaux de notifications locales
        NotificationHelper.createNotificationChannels(this)
    }
}
