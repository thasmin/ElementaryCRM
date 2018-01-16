package com.axelby.elementarycrm

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.arch.persistence.room.Room
import android.arch.persistence.room.testing.MigrationTestHelper
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DBMigrationTests {
    private val dbName = "dbtest.db"

    @Rule
    @JvmField
    val instantTaskRule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val testHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            DB::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigrationFrom1() {
        val db = testHelper.createDatabase(dbName, 1)
        val values = ContentValues()
        values.put("uri", "1")
        values.put("name", "Dan")
        db.insert("clients", SQLiteDatabase.CONFLICT_REPLACE, values)
        Assert.assertEquals("insert should have succeeded", 1, db.query("SELECT COUNT(*) FROM clients").count)

        testHelper.runMigrationsAndValidate(dbName, 3, true, DB.MIGRATION_1_2, DB.MIGRATION_2_3)
        val db2 = migratedDB()
        val b = db2.clientDao().getAll().test()
        b.assertValue {
            it.size == 1 && it[0].name == "Dan"
        }
    }

    @Test
    fun testMigrationFrom2() {
        val db = testHelper.createDatabase(dbName, 2)
        val values = ContentValues()
        val noteDate = Date()
        val noteText = "asdf"
        values.put("uri", "1")
        values.put("name", "Dan")
        values.put("notes", Converters().fromNoteList(arrayListOf(Note(noteDate, noteText))))
        db.insert("clients", SQLiteDatabase.CONFLICT_REPLACE, values)
        Assert.assertEquals("insert should have succeeded", 1, db.query("SELECT COUNT(*) FROM clients").count)

        testHelper.runMigrationsAndValidate(dbName, 3, true, DB.MIGRATION_1_2, DB.MIGRATION_2_3)
        val db2 = migratedDB()
        val b = db2.clientDao().getAll().test()
        b.assertValue { it.size == 1 }
        b.assertValue { it[0].name == "Dan" }
        b.assertValue { it[0].notes.size == 1 }
        b.assertValue { it[0].notes[0].date == noteDate }
        b.assertValue { it[0].notes[0].text == noteText }
    }

    private fun migratedDB(): DB {
        val db = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(), DB::class.java, dbName)
                .allowMainThreadQueries()
                .addMigrations(DB.MIGRATION_1_2)
                .build()
        testHelper.closeWhenFinished(db)
        return db
    }

}