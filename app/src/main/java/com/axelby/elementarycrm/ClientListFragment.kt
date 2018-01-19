package com.axelby.elementarycrm

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.QuickContactBadge
import android.widget.TextView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_client_list.*

class ClientListFragment : Fragment() {
    private val adapter = ClientListAdapter()
    private val disposables = CompositeDisposable()

    private var clientSelectedListener: OnClientSelectedListener? = null

    interface OnClientSelectedListener {
        fun onClientSelected(clientUri: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnClientSelectedListener)
            clientSelectedListener = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_client_list, container, false)
    }

    private val pickContact: Int = 2015
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab.setOnClickListener {
            val i = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(i, pickContact)
        }

        clientList.setHasFixedSize(true)
        clientList.layoutManager = LinearLayoutManager(context)
        clientList.adapter = adapter
        disposables.add(App.instance.db.clientDao().watchAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            adapter.clients = it
                            adapter.notifyDataSetChanged()
                        },
                        { e -> Log.e("MainActivity", "error retrieving clients", e) }
                ))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickContact || resultCode != RESULT_OK || data == null)
            return

        val contactId = data.data
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val cursor = view?.context?.contentResolver?.query(contactId, projection, null, null, null) ?: return
        if (cursor.moveToFirst()) {
            val name = cursor.getString(0)
            disposables.add(Completable.fromAction { App.instance.db.clientDao().save(Client(data.dataString, name)) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
            )
        }
        cursor.close()
    }

    class ClientListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badge: QuickContactBadge = view.findViewById(R.id.badge)
        val name: TextView = view.findViewById(R.id.name)
        val delete: ImageButton = view.findViewById(R.id.delete)
    }

    inner class ClientListAdapter(var clients: List<Client> = listOf()) : RecyclerView.Adapter<ClientListViewHolder>() {
        override fun onBindViewHolder(holder: ClientListViewHolder?, position: Int) {
            if (holder == null)
                return

            val client = clients[position]
            holder.badge.assignContactUri(Uri.parse(client.uri))
            holder.name.text = client.name
            holder.name.setOnClickListener {
                clientSelectedListener?.onClientSelected(client.uri)
            }
            holder.delete.setOnClickListener {
                disposables.add(Completable.fromAction { App.instance.db.clientDao().deleteByUri(client.uri) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe()
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ClientListViewHolder {
            return ClientListViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.item_clientlist, parent, false))
        }

        override fun getItemCount(): Int = clients.size
    }
}
