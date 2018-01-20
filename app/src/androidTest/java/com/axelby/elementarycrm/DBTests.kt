package com.axelby.elementarycrm

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class DBTests {
    private lateinit var db: DB

    @Rule
    @JvmField
    val instantTaskRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), DB::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testClientDao() {
        val noteDate = LocalDateTime.now().withNano(0)
        val noteText = "note"
        val reminderDate = noteDate.plusSeconds(10)
        val reminderText = "note"
        db.clientDao().save(Client(
                "1",
                "Dan",
                notes = arrayListOf(Note(noteDate, noteText)),
                reminders = arrayListOf(Note(reminderDate, reminderText)))
        )
        val tests = db.clientDao().watchAll().test()
        tests.assertValueCount(1)
        tests.assertValueAt(0) { it[0].uri == "1" }
        tests.assertValueAt(0) { it[0].name == "Dan" }
        tests.assertValueAt(0) { it[0].notes.size == 1 }
        tests.assertValueAt(0) { it[0].notes[0].date == noteDate }
        tests.assertValueAt(0) { it[0].notes[0].text == noteText }
        tests.assertValueAt(0) { it[0].reminders[0].date == reminderDate }
        tests.assertValueAt(0) { it[0].reminders[0].text == reminderText }

        db.clientDao().save(Client("2", "Sebastian"))
        tests.assertValueCount(2)
    }

    @Test
    fun testNotWatch() {
        val client = Client("1", "Dan")
        db.clientDao().save(client)
        val test = db.clientDao().getByUri("1").test()
        test.assertValue { it.notes.size == 0 }
        client.notes.add(Note(LocalDateTime.now(), "have a note"))
        db.clientDao().save(client)
        test.assertValue { it.notes.size == 0 }
    }

    @Test
    fun testWatchNotes() {
        val client = Client("1", "dan")
        db.clientDao().save(client)
        val test = db.clientDao().watchUri("1").test()
        test.assertValue { it.notes.size == 0 }
        val date = LocalDateTime.now().withNano(0)
        client.notes.add(Note(date, "have a note"))
        db.clientDao().save(client)
        test.assertValueCount(2)
        test.assertValueAt(1) { it.notes.size == 1 }
        test.assertValueAt(1) { it.notes[0].date == date }
        test.assertValueAt(1) { it.notes[0].text == "have a note" }
    }
}
