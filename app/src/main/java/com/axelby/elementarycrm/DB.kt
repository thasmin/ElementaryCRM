package com.axelby.elementarycrm

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.util.JsonReader
import android.util.JsonWriter
import io.reactivex.Flowable
import io.reactivex.Single
import java.io.StringReader
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

data class Note(val date: Date, val text: String)

@Entity(tableName = "clients")
data class Client(
        @PrimaryKey(autoGenerate = false) var uri: String = "",
        @ColumnInfo var name: String = "",
        @ColumnInfo var notes: MutableList<Note> = arrayListOf(),
        @ColumnInfo var reminders: MutableList<Note> = arrayListOf()
)

@Dao()
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name")
    fun getAll(): Flowable<List<Client>>

    @Query("SELECT * FROM clients WHERE uri = :arg0 ORDER BY name")
    fun getByUri(arg0: String): Single<Client>

    @Query("SELECT * FROM clients WHERE uri = :arg0 ORDER BY name")
    fun watchUri(arg0: String): Flowable<Client>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(vararg clients: Client)

    @Delete
    fun delete(client: Client)

    @Query("DELETE FROM clients WHERE uri = :arg0")
    fun deleteByUri(arg0: String)
}

class Converters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    @TypeConverter
    fun fromNoteList(strings: MutableList<Note>?): String? {
        val result = StringWriter()
        val json = JsonWriter(result)
        json.beginArray()
        strings?.forEach {
            json.beginObject()
            json.name("date")
            json.value(dateFormat.format(it.date))
            json.name("text")
            json.value(it.text)
            json.endObject()
        }
        json.endArray()
        json.close()
        return result.toString()
    }

    @TypeConverter
    fun toNoteList(strings: String?): MutableList<Note>? {
        if (strings == null)
            return arrayListOf()
        val reader = StringReader(strings)
        val json = JsonReader(reader)
        val result = arrayListOf<Note>()
        json.beginArray()
        while (json.hasNext()) {
            json.beginObject()
            json.nextName()
            val date = dateFormat.parse(json.nextString())
            json.nextName()
            val text = json.nextString()
            result.add(Note(date, text))
            json.endObject()
        }
        json.endArray()
        return result
    }
}

@TypeConverters(Converters::class)
@Database(entities = [(Client::class)], version = 3)
abstract class DB : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    companion object {

        @Suppress("PropertyName")
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE clients ADD COLUMN notes TEXT NOT NULL DEFAULT '[]'")
            }
        }

        @Suppress("PropertyName")
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE clients ADD COLUMN reminders TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val allMigrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}

