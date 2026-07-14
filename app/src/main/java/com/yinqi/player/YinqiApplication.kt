package com.yinqi.player

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.yinqi.player.data.MusicDatabase

val Context.settingsDataStore by preferencesDataStore(name = "settings")

class YinqiApplication : Application() {
    val database: MusicDatabase by lazy {
        Room.databaseBuilder(this, MusicDatabase::class.java, "music-library.db")
            .addMigrations(MusicDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
