package com.eomer91.minigamesworld

import android.app.Application
import com.eomer91.minigamesworld.data.LegacyDataMigrator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MiniGamesApp : Application() {

    @Inject
    lateinit var legacyDataMigrator: LegacyDataMigrator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // React Native surumunden kalan verileri (rekorlar) bir kez yeni depoya tasir.
        appScope.launch { legacyDataMigrator.migrateIfNeeded() }
    }
}
