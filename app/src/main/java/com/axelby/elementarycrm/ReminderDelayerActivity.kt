package com.axelby.elementarycrm

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.TextView
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_reminder_delayer.*
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReminderDelayerActivity : AppCompatActivity() {
    private val requestCallPhone = 1

    private lateinit var clientUri: String
    private lateinit var notificationDate: LocalDateTime
    private lateinit var notificationText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clientUri = intent.getStringExtra("uri")
        val notificationId = intent.getIntExtra("notificationId", 0)
        val name = intent.getStringExtra("name")
        notificationDate = LocalDateTime.ofEpochSecond(intent.getLongExtra("date", 0), 0, ZoneOffset.UTC)
        notificationText = intent.getStringExtra("description")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        setContentView(R.layout.activity_reminder_delayer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        client.text = name
        date.text = toReminderTime(notificationDate)
        text.setText(notificationText, TextView.BufferType.EDITABLE)
        Single.fromCallable { getPhoneNumberFromUri(this, clientUri) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { phone.text = it },
                        { Log.e("ReminderDelayerActivity", "unable to get phone number", it) }
                )
        Single.fromCallable { getEmailAddressFromUri(this, clientUri) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { email.text = it },
                        { Log.e("ReminderDelayerActivity", "unable to get phone number", it) }
                )

        text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                notificationText = s?.toString() ?: ""
            }

        })
        call_btn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone.text)))
            else
                requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), requestCallPhone)
        }
        email_btn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email.text)))
        }
        date_btn.setOnClickListener { showReminderDate() }
        save_btn.setOnClickListener { saveReminder() }
    }

    override fun onSupportNavigateUp(): Boolean {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return

        when (requestCode) {
            requestCallPhone ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone.text)))
        }
    }

    private fun showReminderDate() {
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val reminderTime = LocalDateTime.now()
                    .withYear(year)
                    .withMonth(month + 1)
                    .withDayOfMonth(dayOfMonth)
            showReminderTime(reminderTime)
        }
        val now = LocalDateTime.now()
        DatePickerDialog(this, dateListener, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    private fun showReminderTime(reminderTime: LocalDateTime) {
        val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val finalTime = reminderTime
                    .withHour(hourOfDay)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0)
            notificationDate = finalTime
            date.text = toReminderTime(finalTime)
        }
        TimePickerDialog(this, timeListener, LocalDateTime.now().hour, 0, false).show()
    }

    private fun saveReminder() {
        val noteDate = if (notificationDate.isAfter(LocalDateTime.now())) notificationDate else LocalDateTime.now().plusSeconds(1)
        val note = Note(noteDate, notificationText)
        App.instance.db.clientDao().getByUri(clientUri)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { client ->
                            client.reminders.add(note)
                            Completable.fromCallable { App.instance.db.clientDao().save(client) }
                                    .observeOn(Schedulers.io())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            {
                                                finish()
                                                startActivity(Intent(this@ReminderDelayerActivity, MainActivity::class.java))
                                                setupAlarms(this@ReminderDelayerActivity)
                                            },
                                            { Log.e("ReminderDelayerActivity", "unable to save reminder", it) }
                                    )
                        },
                        { Log.e("ReminderDelayerActivity", "unable to load client to save reminder", it) }
                )
    }

}
