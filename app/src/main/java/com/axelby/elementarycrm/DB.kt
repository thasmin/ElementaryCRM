package com.axelby.elementarycrm

import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.util.JsonReader
import android.util.JsonWriter
import io.reactivex.Flowable
import io.reactivex.Single
import java.io.StringReader
import java.io.StringWriter
import java.time.Instant

data class Note(val date: java.time.Instant, val text: String)

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
    fun getAll(): Single<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name")
    fun watchAll(): Flowable<List<Client>>

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
    @TypeConverter
    fun fromNoteList(strings: MutableList<Note>?): String? {
        val result = StringWriter()
        val json = JsonWriter(result)
        json.beginArray()
        strings?.forEach {
            json.beginObject()
            json.name("date")
            json.value(it.date.toEpochMilli())
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
            val date = Instant.ofEpochMilli(json.nextLong())
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
@Database(entities = [(Client::class)], version = 1)
abstract class DB : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    companion object {
        val allMigrations = arrayOf<Migration>()
    }
}

