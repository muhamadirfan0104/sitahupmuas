package muhamad.irfan.sitahupm.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.google.android.material.bottomnavigation.BottomNavigationView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.ui.auth.AuthActivity
import muhamad.irfan.sitahupm.ui.cart.CartFragment
import muhamad.irfan.sitahupm.ui.home.HomeFragment
import muhamad.irfan.sitahupm.ui.order.OrdersFragment
import muhamad.irfan.sitahupm.ui.product.ProductsFragment
import muhamad.irfan.sitahupm.ui.product.SearchProductsFragment
import muhamad.irfan.sitahupm.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var headerNormal: LinearLayout
    private lateinit var headerSearch: LinearLayout
    private lateinit var edtSearch: AutoCompleteTextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)

        setContentView(R.layout.activity_main)

        title = findViewById(R.id.txtTitle)
        headerNormal = findViewById(R.id.headerNormal)
        headerSearch = findViewById(R.id.headerSearch)
        edtSearch = findViewById(R.id.edtSearch)
        bottomNav = findViewById(R.id.bottomNav)

        setupAutoCompleteSearch()
        findViewById<View>(R.id.btnSearch).setOnClickListener { openProductSearch() }
        findViewById<View>(R.id.btnSearchBack).setOnClickListener {
            closeSearchMode(clearText = true)
            bottomNav.selectedItemId = R.id.nav_products
        }
        findViewById<View>(R.id.btnDoSearch).setOnClickListener { submitSearch() }
        edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }
        findViewById<View>(R.id.btnMenu).setOnClickListener { showPopupMenu(it) }
        findViewById<View>(R.id.btnMenu).setOnLongClickListener {
            showPopupMenu(it)
            true
        }

        bottomNav.setOnItemSelectedListener { item ->
            closeSearchMode(clearText = false)
            when (item.itemId) {
                R.id.nav_products -> show("Produk", ProductsFragment())
                R.id.nav_cart -> show("Keranjang", CartFragment())
                R.id.nav_orders -> show("Pesanan", OrdersFragment())
                R.id.nav_profile -> show("Profil", ProfileFragment())
                else -> show("SiTahu", HomeFragment())
            }
            true
        }

        if (savedInstanceState == null) handleOpenTab(intent)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenTab(intent)
    }

    private fun handleOpenTab(intent: Intent?) {
        when (intent?.getStringExtra("open_tab")) {
            "cart" -> bottomNav.selectedItemId = R.id.nav_cart
            "orders" -> bottomNav.selectedItemId = R.id.nav_orders
            else -> bottomNav.selectedItemId = R.id.nav_home
        }
    }

    fun isLogin(): Boolean = session.isLogin()

    fun openLogin() {
        startActivity(Intent(this, AuthActivity::class.java))
    }

    fun openProductsFromHome() {
        bottomNav.selectedItemId = R.id.nav_products
    }

    fun setSearchText(keyword: String) {
        edtSearch.setText(keyword)
        edtSearch.setSelection(edtSearch.text.length)
    }

    private fun openProductSearch() {
        headerNormal.visibility = View.GONE
        headerSearch.visibility = View.VISIBLE
        edtSearch.requestFocus()
        show("Pencarian Produk", SearchProductsFragment())
    }

    private fun closeSearchMode(clearText: Boolean = false) {
        headerSearch.visibility = View.GONE
        headerNormal.visibility = View.VISIBLE
        if (clearText) edtSearch.setText("")
    }

    private fun submitSearch() {
        val fragment = supportFragmentManager.findFragmentById(R.id.frameContent)
        if (fragment !is SearchProductsFragment) {
            show("Pencarian Produk", SearchProductsFragment())
            supportFragmentManager.executePendingTransactions()
        }
        val active = supportFragmentManager.findFragmentById(R.id.frameContent)
        (active as? SearchReceiver)?.onSearchSubmit(edtSearch.text.toString())
    }


    private fun setupAutoCompleteSearch() {
        val suggestions = arrayOf(
            "Tahu Putih",
            "Tahu Kuning",
            "Tahu Pong",
            "Tahu Susu",
            "Tahu Goreng",
            "Paket Campur Tahu Premium",
            "Tahu Crispy"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggestions)
        edtSearch.setAdapter(adapter)
        edtSearch.threshold = 1
        edtSearch.setOnItemClickListener { _, _, _, _ ->
            submitSearch()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_login_logout)?.title = if (session.isLogin()) "Logout" else "Login"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> {
                bottomNav.selectedItemId = R.id.nav_home
                true
            }
            R.id.menu_products -> {
                bottomNav.selectedItemId = R.id.nav_products
                true
            }
            R.id.menu_cart -> {
                bottomNav.selectedItemId = R.id.nav_cart
                true
            }
            R.id.menu_orders -> {
                bottomNav.selectedItemId = R.id.nav_orders
                true
            }
            R.id.menu_login_logout -> {
                if (session.isLogin()) logout() else openLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun show(pageTitle: String, fragment: Fragment) {
        title.text = pageTitle
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContent, fragment)
            .commit()
    }

    private fun showPopupMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Profil")
            menu.add(if (session.isLogin()) "Logout" else "Login")

            setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "Profil" -> bottomNav.selectedItemId = R.id.nav_profile
                    "Logout" -> logout()
                    "Login" -> openLogin()
                }
                true
            }
            show()
        }
    }

    private fun logout() {
        ApiClient.request(this, Request.Method.POST, "/auth/logout", null, {
            clearLocalSession("Logout berhasil")
        }, {
            clearLocalSession("Logout berhasil")
        })
    }

    private fun clearLocalSession(message: String) {
        session.clear()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        invalidateOptionsMenu()
        bottomNav.selectedItemId = R.id.nav_home
    }
}
