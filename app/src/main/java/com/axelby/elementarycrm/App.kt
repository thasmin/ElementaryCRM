package com.axelby.elementarycrm

import android.app.Application
import android.arch.persistence.room.Room

class App : Application() {
    lateinit var db: DB

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, DB::class.java, "lmcrm.db").addMigrations(*DB.allMigrations).build()
    }

    init {
        instance = this
    }

    companion object {
        lateinit var instance: App
    }
}
