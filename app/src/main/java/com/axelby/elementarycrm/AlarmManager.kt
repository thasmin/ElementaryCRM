package com.axelby.elementarycrm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import io.reactivex.schedulers.Schedulers
import java.time.Instant


class Notifier : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.getStringExtra("uri")
        val notificationId = intent.getIntExtra("notificationId", 0)
        val name = intent.getStringExtra("name")
        val date = Instant.ofEpochMilli(intent.getLongExtra("date", Instant.now().toEpochMilli()))
        val desc = intent.getStringExtra("description")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannel = NotificationChannel("client", "Client Notifications", NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.apply {
            description = "Scheduled notifications for clients"
            enableLights(true)
            lightColor = context.getColor(R.color.colorPrimary)
            enableVibration(true)
            vibrationPattern = arrayOf(200L, 200, 200, 600).toLongArray()
        }
        notificationManager.createNotificationChannel(notificationChannel)

        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + getPhoneNumberFromUri(context, uri)))
        val callPendingIntent = PendingIntent.getActivity(context, 0, callIntent, 0)
        val callAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_phone_black_24dp), "Call", callPendingIntent).build()

        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getEmailAddressFromUri(context, uri)))
        val emailPendingIntent = PendingIntent.getActivity(context, 0, emailIntent, 0)
        val emailAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_email_black_24dp), "Email", emailPendingIntent).build()

        val notification = Notification.Builder(context, "client")
                .setContentText(desc)
                .setContentTitle(name)
                .setSmallIcon(R.drawable.ic_access_time_black_24dp)
                .addPerson(uri)
                .setActions(callAction, emailAction)
                .build()
        notificationManager.notify(notificationId, notification)

        // delete reminder from client
        App.instance.db.clientDao().getByUri(uri)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { client ->
                            if (!client.reminders.removeIf { it.date == date && it.text == desc })
                                Log.e("Notifier", "could not remove triggered reminder")
                            App.instance.db.clientDao().save(client)
                        },
                        { e -> Log.e("Notifier", "unable to remove reminder", e) }
                )
    }
}

fun setupAlarms(context: Context) {
    val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // cancel all pending intents to notifier
    val notifierBroadcastIntent = Intent(context, Notifier::class.java)
    val pendingUpdateIntent = PendingIntent.getBroadcast(context, 0, notifierBroadcastIntent, 0)
    alarmManager.cancel(pendingUpdateIntent)

    var notificationId = 0
    App.instance.db.clientDao().getAll()
            .flattenAsObservable { it -> it }
            .filter { it.reminders.isNotEmpty() }
            .flatMapIterable { it.reminders.map { rem -> Client(it.uri, it.name, arrayListOf(), arrayListOf(rem)) } }
            .subscribe(
                    {
                        val alarmIntent = Intent(context, Notifier::class.java)
                        alarmIntent.putExtra("notificationId", notificationId)
                        alarmIntent.putExtra("uri", it.uri)
                        alarmIntent.putExtra("name", it.name)
                        alarmIntent.putExtra("date", it.reminders[0].date.toEpochMilli())
                        alarmIntent.putExtra("description", it.reminders[0].text)
                        val pendingAlarmIntent = PendingIntent.getBroadcast(context, notificationId, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.reminders[0].date.toEpochMilli(), pendingAlarmIntent)
                        notificationId += 1
                    },
                    { Log.e("setupAlarms", "unable to find reminders", it) }
            )
}
