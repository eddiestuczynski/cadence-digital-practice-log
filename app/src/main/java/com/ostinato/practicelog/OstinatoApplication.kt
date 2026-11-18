package com.ostinato.practicelog

import android.app.Application
import com.ostinato.practicelog.data.local.OstinatoDatabase
import com.ostinato.practicelog.data.preferences.UserPreferencesRepository
import com.ostinato.practicelog.data.repository.PracticeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OstinatoApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: OstinatoDatabase by lazy {
        OstinatoDatabase.getDatabase(this)
    }

    val practiceRepository: PracticeRepository by lazy {
        PracticeRepository(database.practiceDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (userPreferencesRepository.shouldRunInitialSeed()) {
                OstinatoDatabase.seedSampleData(database.practiceDao(), forceReseed = false)
                userPreferencesRepository.markInitialSeedCompleted()
            }
        }
    }
}
