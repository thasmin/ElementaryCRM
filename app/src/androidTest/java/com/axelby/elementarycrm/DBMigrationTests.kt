package com.axelby.elementarycrm

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.arch.persistence.room.testing.MigrationTestHelper
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith

@Ignore
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
}