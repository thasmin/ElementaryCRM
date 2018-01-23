package com.axelby.elementarycrm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_reminder_list.*
import java.time.ZoneOffset

class ReminderListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadReminders()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class ReminderListViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.time)
        val client: TextView = view.findViewById(R.id.client)
        val edit: ImageButton = view.findViewById(R.id.edit)
        val trash: ImageButton = view.findViewById(R.id.trash)
    }

    private fun loadReminders() {
        reminders.layoutManager = LinearLayoutManager(this)
        App.instance.db.clientDao().getReminders()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { reminders.adapter = ReminderListAdapter(it) },
                        { Log.e("ReminderListActivity", "unable to load reminders", it) }
                )
    }

    inner class ReminderListAdapter(private val reminders: List<ReminderItem>) : RecyclerView.Adapter<ReminderListViewHolder>() {
        override fun getItemCount() = reminders.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderListViewHolder {
            return ReminderListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_reminderlist, parent, false))
        }

        override fun onBindViewHolder(holder: ReminderListViewHolder, position: Int) {
            val reminder = reminders[position]
            holder.time.text = reminder.date.toReminderTime()
            holder.client.text = reminder.name
            holder.edit.setOnClickListener {
                val intent = Intent(this@ReminderListActivity, ReminderDelayerActivity::class.java)
                intent.putExtra("uri", reminder.uri)
                intent.putExtra("name", reminder.name)
                intent.putExtra("date", reminder.date.toEpochSecond(ZoneOffset.UTC))
                intent.putExtra("description", reminder.text)
                startActivity(intent)
            }
            holder.trash.setOnClickListener {
                App.instance.db.clientDao().getByUri(reminder.uri)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { client ->
                                    client.reminders.removeIf { it.date == reminder.date && it.text == reminder.text }
                                    Completable.fromCallable { App.instance.db.clientDao().save(client) }
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(
                                                    { loadReminders() },
                                                    { Log.e("ReminderListActivity", "Unable to remove reminder", it) }
                                            )
                                },
                                { Log.e("ReminderListActivity", "unable to load client to remove reminder", it) }
                        )
            }
            holder.view.setOnClickListener {
                it.showPopup(reminder.text)
            }
        }

    }
}
