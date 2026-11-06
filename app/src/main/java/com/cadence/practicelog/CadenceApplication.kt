package com.cadence.practicelog

import android.app.Application
import com.cadence.practicelog.data.local.CadenceDatabase
import com.cadence.practicelog.data.preferences.UserPreferencesRepository
import com.cadence.practicelog.data.repository.PracticeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CadenceApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: CadenceDatabase by lazy {
        CadenceDatabase.getDatabase(this)
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
                CadenceDatabase.seedSampleData(database.practiceDao(), forceReseed = false)
                userPreferencesRepository.markInitialSeedCompleted()
            }
        }
    }
}
