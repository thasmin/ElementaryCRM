package com.axelby.elementarycrm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ClientListFragment.OnClientSelectedListener {
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, ClientListFragment())
                .commit()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClientSelected(clientUri: String) {
        val fragment = ClientDetailFragment()
        val args = Bundle()
        args.putString("client_uri", clientUri)
        fragment.arguments = args
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .addToBackStack(null)
                .commit()
    }
}
