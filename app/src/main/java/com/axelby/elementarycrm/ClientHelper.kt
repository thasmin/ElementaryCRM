package com.axelby.elementarycrm

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import java.time.*
import java.time.format.DateTimeFormatter

fun toReminderTime(date: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    return toReminderTime(date.toInstant(ZoneOffset.UTC), now.toInstant(ZoneOffset.UTC))
}

fun toReminderTime(date: Instant, now: Instant = Instant.now()): String {
    val zonedDateTime = date.atZone(ZoneId.systemDefault())
    val daysDiff = Duration.between(now.atZone(ZoneId.systemDefault()), zonedDateTime).toDays()
    val timeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("h:mm a") }
    val thisWeekFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("EEEE 'at' h:mm a") }
    val nextWeekFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("'next' EEEE 'at' h:mm a") }
    val otherwiseFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("MMMM d 'at' h:mm a") }
    return when {
        daysDiff == 0L -> "today at " + timeFormatter.format(zonedDateTime)
        daysDiff == 1L -> "tomorrow at " + timeFormatter.format(zonedDateTime)
        daysDiff < 7 -> thisWeekFormatter.format(zonedDateTime)
        daysDiff < 14 -> nextWeekFormatter.format(zonedDateTime)
        else -> otherwiseFormatter.format(zonedDateTime)
    }
}

fun getContactIdFromUri(context: Context, uri: String): String? {
    val projection = arrayOf(ContactsContract.Contacts._ID)
    val cursor = context.contentResolver.query(Uri.parse(uri), projection, null, null, null)
    val id = if (cursor.moveToFirst()) cursor.getString(0) else null
    cursor.close()
    return id
}

fun getPhoneNumber(context: Context, contactId: String): String? {
    // get phone number
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
    val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
            arrayOf(contactId),
            null)
    val phoneNumber = if (cursor.moveToFirst()) cursor.getString(0) else null
    cursor.close()
    return phoneNumber
}

fun getPhoneNumberFromUri(context: Context, uri: String): String? {
    // get phone number
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
    val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
            arrayOf(getContactIdFromUri(context, uri)),
            null)
    val phoneNumber = if (cursor.moveToFirst()) cursor.getString(0) else null
    cursor.close()
    return phoneNumber
}

fun getEmailAddress(context: Context, contactId: String): String? {
    val projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
    val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
            arrayOf(contactId),
            null)
    val emailAddress = if (cursor.moveToFirst()) cursor.getString(0) else null
    cursor.close()
    return emailAddress
}

fun getEmailAddressFromUri(context: Context, uri: String): String? {
    val projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
    val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
            arrayOf(getContactIdFromUri(context, uri)),
            null)
    val emailAddress = if (cursor.moveToFirst()) cursor.getString(0) else null
    cursor.close()
    return emailAddress
}

