package muhamad.irfan.sitahupm.ui.address

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.model.Address

class AddressActivity : AppCompatActivity() {
    private lateinit var listContainer: LinearLayout
    private var firstResume = true
    private var selectedAddress: Address? = null

    companion object {
        private const val MENU_EDIT = 201
        private const val MENU_VIEW = 202
        private const val MENU_MAIN = 203
        private const val MENU_DELETE = 204
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_simple)

        listContainer = findViewById(R.id.addressListContainer)

        findViewById<TextView>(R.id.btnAddAddress).setOnClickListener {
            startActivity(Intent(this, AddressFormActivity::class.java))
        }

        loadAddresses()
    }

    override fun onResume() {
        super.onResume()

        if (firstResume) {
            firstResume = false
        } else {
            loadAddresses()
        }
    }

    private fun loadAddresses() {
        ApiClient.request(this, Request.Method.GET, "/addresses", null, { response ->
            val arr = response.getJSONArray("data")
            val rows = mutableListOf<Address>()

            for (i in 0 until arr.length()) {
                rows.add(Address.fromJson(arr.getJSONObject(i)))
            }

            renderAddressList(rows)
        }, {
            toast(it)
        })
    }

    private fun renderAddressList(rows: List<Address>) {
        listContainer.removeAllViews()

        if (rows.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Belum ada alamat. Tambahkan alamat baru."
            empty.setTextColor(resources.getColor(R.color.text_muted, null))
            empty.setPadding(dp(12), dp(12), dp(12), dp(12))
            listContainer.addView(empty)
            return
        }

        rows.forEach { address ->
            listContainer.addView(createAddressCard(address))
        }
    }

    private fun createAddressCard(address: Address): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(12), dp(12), dp(12), dp(12))
        card.setBackgroundResource(R.drawable.bg_order_card_simple)

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, dp(10))
        card.layoutParams = lp

        val title = TextView(this)
        title.text = address.nama + if (address.utama) " (Utama)" else ""
        title.setTextColor(resources.getColor(R.color.text_main, null))
        title.setTextSize(15f)
        title.setTypeface(null, Typeface.BOLD)

        val detail = TextView(this)
        detail.text = "${address.telepon}\n${address.alamat}\nTitik lokasi: ${pointText(address)}"
        detail.setTextColor(resources.getColor(R.color.text_muted, null))
        detail.setTextSize(12.5f)
        detail.setPadding(0, dp(5), 0, dp(8))

        val hint = TextView(this)
        hint.text = "Tahan kartu untuk membuka pilihan alamat."
        hint.setTextColor(resources.getColor(R.color.primary, null))
        hint.setTextSize(12f)
        hint.setTypeface(null, Typeface.BOLD)

        card.addView(title)
        card.addView(detail)
        card.addView(hint)

        registerForContextMenu(card)
        card.setOnLongClickListener {
            selectedAddress = address
            openContextMenu(card)
            true
        }

        return card
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val address = selectedAddress ?: return
        menu.setHeaderTitle(address.nama)
        menu.add(0, MENU_EDIT, 0, "Edit")
        menu.add(0, MENU_VIEW, 1, "Lihat Lokasi")
        menu.add(0, MENU_MAIN, 2, if (address.utama) "Sudah Alamat Utama" else "Jadikan Utama")
        menu.add(0, MENU_DELETE, 3, "Hapus")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val address = selectedAddress ?: return super.onContextItemSelected(item)

        return when (item.itemId) {
            MENU_EDIT -> {
                openEdit(address)
                true
            }
            MENU_VIEW -> {
                if (address.lat == null || address.lng == null) {
                    toast("Alamat ini belum memiliki titik lokasi")
                } else {
                    openMap(address.lat, address.lng, address.nama)
                }
                true
            }
            MENU_MAIN -> {
                if (address.utama) {
                    toast("Alamat ini sudah menjadi alamat utama")
                } else {
                    setMainAddress(address.id)
                }
                true
            }
            MENU_DELETE -> {
                confirmDelete(address)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun openEdit(address: Address) {
        startActivity(
            Intent(this, AddressFormActivity::class.java)
                .putExtra("id", address.id)
                .putExtra("nama", address.nama)
                .putExtra("telepon", address.telepon)
                .putExtra("alamat", address.alamat)
                .putExtra("utama", address.utama)
                .putExtra("lat", address.lat ?: 0.0)
                .putExtra("lng", address.lng ?: 0.0)
                .putExtra("has_point", address.lat != null && address.lng != null)
        )
    }

    private fun openMap(lat: Double, lng: Double, title: String) {
        startActivity(
            Intent(this, AddressMapActivity::class.java)
                .putExtra("lat", lat)
                .putExtra("lng", lng)
                .putExtra("title", title)
        )
    }

    private fun setMainAddress(id: Int) {
        ApiClient.request(this, Request.Method.PATCH, "/addresses/$id/main", null, {
            toast("Alamat utama diperbarui")
            loadAddresses()
        }, {
            toast(it)
        })
    }

    private fun confirmDelete(address: Address) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Alamat")
            .setMessage("Hapus alamat ${address.nama}?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                deleteAddress(address.id)
            }
            .show()
    }

    private fun deleteAddress(id: Int) {
        ApiClient.request(this, Request.Method.DELETE, "/addresses/$id", null, {
            toast("Alamat berhasil dihapus")
            loadAddresses()
        }, {
            toast(it)
        })
    }

    private fun pointText(address: Address): String {
        return if (address.lat == null || address.lng == null) {
            "Belum dipilih"
        } else {
            "Sudah dipilih"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
