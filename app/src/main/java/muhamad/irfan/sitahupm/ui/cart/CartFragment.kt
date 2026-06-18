package muhamad.irfan.sitahupm.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.GuestCartDbHelper
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.data.model.CartItem
import muhamad.irfan.sitahupm.ui.auth.AuthActivity
import muhamad.irfan.sitahupm.ui.checkout.CheckoutActivity
import muhamad.irfan.sitahupm.ui.common.SimpleCard
import muhamad.irfan.sitahupm.ui.common.SimpleCardAdapter
import muhamad.irfan.sitahupm.util.FormatUtil
import org.json.JSONObject

class CartFragment : Fragment() {
    private lateinit var adapter: SimpleCardAdapter
    private lateinit var info: TextView
    private lateinit var empty: TextView
    private lateinit var checkoutButton: Button

    private val qtyMap = mutableMapOf<Int, Int>()
    private var total = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.txtPageTitle).text = "Keranjang"
        view.findViewById<TextView>(R.id.txtPageDesc).text =
            "Pilih produk yang ingin dipesan."

        info = view.findViewById(R.id.txtExtra)
        empty = view.findViewById(R.id.txtEmpty)
        checkoutButton = view.findViewById(R.id.btnAction)

        checkoutButton.setOnClickListener { checkout() }

        adapter = SimpleCardAdapter(
            mutableListOf(),
            { addOne(it.dataId) },
            { removeItem(it.dataId) }
        )

        view.findViewById<RecyclerView>(R.id.rvList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CartFragment.adapter
        }

        loadCart()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadCart()
    }

    private fun isLogin(): Boolean {
        val ctx = context ?: return false
        return SessionManager(ctx).isLogin()
    }

    private fun loadCart() {
        if (!isAdded) return
        if (isLogin()) loadCartFromApi() else loadGuestCart()
    }

    private fun loadGuestCart() {
        val ctx = context ?: return
        val db = GuestCartDbHelper(ctx)
        val rows = db.items()

        qtyMap.clear()
        total = rows.sumOf { it.subtotal }

        val cards = rows.map {
            qtyMap[it.produkId] = it.jumlah
            SimpleCard(
                top = it.nama,
                mid = "Jumlah ${it.jumlah} • Stok ${it.stok} ${it.satuan}",
                bottom = FormatUtil.rupiah(it.subtotal),
                primary = "+1",
                second = "Hapus",
                dataId = it.produkId,
                imageUrl = it.gambar
            )
        }

        showCart(cards)
    }

    private fun loadCartFromApi() {
        val ctx = context ?: return
        ApiClient.request(ctx, Request.Method.GET, "/cart", null, success@{ response ->
            if (!isAdded || context == null) return@success
            val data = response.getJSONObject("data")
            val arr = data.getJSONArray("items")

            qtyMap.clear()
            total = 0.0

            val cards = mutableListOf<SimpleCard>()

            for (i in 0 until arr.length()) {
                val item = CartItem.fromJson(arr.getJSONObject(i))
                qtyMap[item.id] = item.jumlah
                total += item.subtotal

                cards.add(
                    SimpleCard(
                        top = item.nama,
                        mid = "Jumlah ${item.jumlah} • Stok ${item.stok} ${item.satuan}",
                        bottom = FormatUtil.rupiah(item.subtotal),
                        primary = "+1",
                        second = "Hapus",
                        dataId = item.id,
                        imageUrl = item.gambar
                    )
                )
            }

            showCart(cards)
        }, error@{
            if (!isAdded || context == null) return@error
            toast(it)
        })
    }

    private fun showCart(cards: List<SimpleCard>) {
        if (!isAdded || context == null) return
        adapter.setItems(cards)

        val isEmpty = cards.isEmpty()
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        info.text = if (isEmpty) "Keranjang kosong." else "Total: ${FormatUtil.rupiah(total)}"
        checkoutButton.text = if (isLogin()) "Checkout" else "Login untuk Checkout"
    }

    private fun addOne(id: Int) {
        if (isLogin()) {
            val newQty = (qtyMap[id] ?: 0) + 1
            updateApiQty(id, newQty)
        } else {
            val ctx = context ?: return
            GuestCartDbHelper(ctx).increment(id)
            loadGuestCart()
        }
    }

    private fun removeItem(id: Int) {
        if (isLogin()) {
            val ctx = context ?: return
            ApiClient.request(ctx, Request.Method.DELETE, "/cart/items/$id", null, success@{
                if (!isAdded || context == null) return@success
                loadCartFromApi()
            }, error@{
                if (!isAdded || context == null) return@error
                toast(it)
            })
        } else {
            val ctx = context ?: return
            GuestCartDbHelper(ctx).remove(id)
            loadGuestCart()
        }
    }

    private fun updateApiQty(id: Int, qty: Int) {
        val body = JSONObject().put("jumlah", qty)

        val ctx = context ?: return
        ApiClient.request(ctx, Request.Method.PATCH, "/cart/items/$id", body, success@{
            if (!isAdded || context == null) return@success
            loadCartFromApi()
        }, error@{
            if (!isAdded || context == null) return@error
            toast(it)
        })
    }

    private fun checkout() {
        if (total <= 0.0) {
            toast("Keranjang masih kosong")
            return
        }

        if (!isLogin()) {
            val ctx = context ?: return
            startActivity(Intent(ctx, AuthActivity::class.java))
            return
        }

        val ctx = context ?: return
        startActivity(Intent(ctx, CheckoutActivity::class.java))
    }

    private fun toast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}
