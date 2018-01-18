package com.axelby.elementarycrm

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_client_detail.*
import java.util.*


class ClientDetailFragment : Fragment() {
    private lateinit var clientUri: String
    private var isFABOpen = false
    private val disposables = CompositeDisposable()

    private val requestReadContacts = 0
    private val requestCallPhone = 1

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // no need to shouldShowRequestPermissionRationale because it's a CRM
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), requestReadContacts)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return

        when (requestCode) {
            requestReadContacts -> loadClient()
            requestCallPhone ->
                startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:" + phone.text) })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_client_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        call_btn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(view.context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:" + phone.text) })
            else
                requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), requestCallPhone)
        }
        email_btn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:" + email.text) })
        }

        notes.layoutManager = LinearLayoutManager(view.context)
        reminders.layoutManager = LinearLayoutManager(view.context)

        fab.setOnClickListener { if (isFABOpen) closeFABMenu() else showFABMenu() }
        noteFab.setOnClickListener { createNote() }
        reminderFab.setOnClickListener { createReminder() }

        loadClient()
    }

    override fun onDetach() {
        super.onDetach()
        disposables.clear()
    }

    private fun createReminder() {
        closeFABMenu()
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val reminderTime = Calendar.getInstance()
            reminderTime.set(Calendar.SECOND, 0)
            reminderTime.set(Calendar.YEAR, year)
            reminderTime.set(Calendar.MONTH, month)
            reminderTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            showReminderTime(reminderTime)
        }
        val c = Calendar.getInstance()
        val nowYear = c.get(Calendar.YEAR)
        val nowMonth = c.get(Calendar.MONTH)
        val nowDayOfMonth = c.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(context, dateListener, nowYear, nowMonth, nowDayOfMonth).show()
    }

    private fun showReminderTime(reminderTime: Calendar) {
        val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
            reminderTime.set(Calendar.MINUTE, minute)
            showReminderDescription(reminderTime)
        }
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        TimePickerDialog(context, timeListener, nowHour, 0, false).show()
    }

    private fun showReminderDescription(reminderTime: Calendar) {
        showTextInputDialog { reminder ->
            App.instance.db.clientDao().getByUri(clientUri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            { client ->
                                client.reminders.add(Note(reminderTime.time, reminder))
                                App.instance.db.clientDao().save(client)
                            },
                            { Log.e("ClientDetailFragment", "unable to load client to add note", it) }
                    )
        }
    }

    private fun createNote() {
        closeFABMenu()
        showTextInputDialog { note ->
            App.instance.db.clientDao().getByUri(clientUri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            { client ->
                                client.notes.add(Note(Date(), note))
                                App.instance.db.clientDao().save(client)
                            },
                            { Log.e("ClientDetailFragment", "unable to load client to add note", it) }
                    )
        }
    }

    private fun showTextInputDialog(saveListener: (String) -> Unit) {
        val textEditor = EditText(context)
        val builder = AlertDialog.Builder(context)
        builder.setView(textEditor)
        builder.setPositiveButton(getString(R.string.save), { _, _ -> saveListener(textEditor.text.toString()) })
        builder.setNegativeButton(getString(R.string.cancel), { _, _ -> })
        val dialog = builder.create()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun showFABMenu() {
        val context = view?.context ?: return

        isFABOpen = true

        val r = context.resources
        val shiftDp = 56f + 24f // 56dp size + 24dp margin
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, shiftDp, r.displayMetrics)

        fab.animate().rotation(45.0f)
        noteFabLayout.animate().alpha(1.0f).translationY(-px)
        reminderFabLayout.animate().alpha(1.0f).translationY(-px * 2)
    }

    private fun closeFABMenu() {
        isFABOpen = false
        fab.animate().rotation(0.0f)
        noteFabLayout.animate().alpha(0.0f).translationY(0f)
        reminderFabLayout.animate().alpha(0.0f).translationY(0f)
    }

    private fun loadClient() {
        if (context == null)
            return
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return
        clientUri = arguments?.getString("client_uri") ?: return

        // get name and phone
        val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        )
        val cursor = context!!.contentResolver.query(Uri.parse(clientUri), projection, null, null, null)
        if (cursor.moveToFirst()) {
            val contactId = cursor.getString(0)
            name.text = cursor.getString(1)

            disposables.add(Single.fromCallable({ getPhoneNumber(contactId) })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { phone.text = it },
                            { Log.e("ClientDetailFragment", "unable to get phone number", it) }
                    )
            )

            disposables.add(Single.fromCallable({ getEmailAddress(contactId) })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { email.text = it },
                            { Log.e("ClientDetailFragment", "unable to get phone number", it) }
                    )
            )

            // get notes
            disposables.add(App.instance.db.clientDao().watchUri(clientUri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                notes.adapter = NotesAdapter(it.notes)
                                reminders.adapter = NotesAdapter(it.reminders)
                            },
                            { Log.e("ClientDetailFragment", "unable to retrieve client from db", it) }
                    )
            )
        }
        cursor.close()
    }

    private fun getEmailAddress(contactId: String?): String? {
        val projection2 = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val cursor2 = context!!.contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection2,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + contactId,
                null,
                null)
        var emailAddress: String? = null
        if (cursor2.moveToFirst())
            emailAddress = cursor2.getString(0)
        cursor2.close()
        return emailAddress
    }

    private fun getPhoneNumber(contactId: String): String? {
        // get phone number
        val projection3 = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val cursor3 = context!!.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection3,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                null,
                null)
        var phoneNumber: String? = null
        if (cursor3.moveToFirst())
            phoneNumber = cursor3.getString(0)
        cursor3.close()
        return phoneNumber
    }

    class NotesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val createTime: ImageView = view.findViewById(R.id.createTime)
        val text: TextView = view.findViewById(R.id.description)
        val delete: ImageButton = view.findViewById(R.id.delete)
    }

    inner class NotesAdapter(private val notes: List<Note>) : RecyclerView.Adapter<NotesViewHolder>() {
        override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
            holder.createTime.setOnClickListener {
                val textView = TextView(holder.itemView.context)
                textView.setPadding(24, 24, 24, 24)
                textView.setTextColor(holder.createTime.context.resources.getColor(R.color.primary_text_default_material_dark, null))
                textView.setBackgroundColor(holder.createTime.context.resources.getColor(R.color.colorPrimary, null))
                textView.text = notes[position].date.toString()

                val popup = PopupWindow(textView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popup.elevation = 5.0f
                // dismiss by touching outside
                popup.isFocusable = true
                popup.isOutsideTouchable = true
                popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val rect = Rect()
                holder.createTime.getGlobalVisibleRect(rect)
                popup.showAtLocation(
                        holder.createTime,
                        Gravity.NO_GRAVITY,
                        rect.left,
                        rect.top + holder.createTime.height)
            }
            holder.text.text = notes[position].text
            holder.delete.setOnClickListener {
                if (isFABOpen)
                    return@setOnClickListener
                App.instance.db.clientDao().getByUri(this@ClientDetailFragment.clientUri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                { client ->
                                    client.notes.removeAt(position)
                                    App.instance.db.clientDao().save(client)
                                },
                                { Log.e("ClientDetailFragment", "unable to load client to add note", it) }
                        )
            }
        }

        override fun getItemCount() = notes.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
            return NotesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false))
        }
    }
}
