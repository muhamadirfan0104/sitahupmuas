package muhamad.irfan.sitahupm.ui.order

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.data.model.Order
import muhamad.irfan.sitahupm.data.model.OrderProduct
import muhamad.irfan.sitahupm.ui.auth.AuthActivity
import muhamad.irfan.sitahupm.util.QrUtil

class OrdersFragment : Fragment() {
    private lateinit var adapter: OrderSimpleAdapter
    private lateinit var empty: TextView
    private lateinit var actionButton: android.widget.Button
    private lateinit var filterWrap: LinearLayout

    private val allOrders = mutableListOf<Order>()
    private var currentFilter = "Semua"

    companion object {
        private const val MENU_DETAIL = 301
        private const val MENU_QR = 302
        private const val MENU_INVOICE = 303
        private const val MENU_REVIEW = 304
        private const val MENU_RECEIVED = 305
        private const val MENU_CANCEL = 306
    }

    private val filters = listOf(
        "Semua",
        "Belum Bayar",
        "Diproses",
        "Dikirim",
        "Selesai",
        "Dibatalkan"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_orders_simple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        empty = view.findViewById(R.id.txtEmpty)
        actionButton = view.findViewById(R.id.btnAction)
        filterWrap = view.findViewById(R.id.filterWrap)

        adapter = OrderSimpleAdapter(mutableListOf()) { anchor, order ->
            showOrderPopup(anchor, order)
        }

        view.findViewById<RecyclerView>(R.id.rvList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@OrdersFragment.adapter
        }

        renderFilterButtons()

        if (!SessionManager(requireContext()).isLogin()) {
            showLoginState()
        } else {
            actionButton.text = "Refresh"
            actionButton.setOnClickListener { loadOrders() }
            loadOrders()
        }
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized && SessionManager(requireContext()).isLogin()) {
            loadOrders()
        }
    }

    private fun showLoginState() {
        actionButton.text = "Login untuk Melihat Pesanan"
        actionButton.setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
        }

        empty.text = "Belum login. Silakan login untuk melihat pesanan."
        empty.visibility = View.VISIBLE
        adapter.setItems(emptyList())
    }

    private fun loadOrders() {
        ApiClient.request(requireContext(), Request.Method.GET, "/orders", null, { response ->
            val data = response.getJSONObject("data")
            val arr = data.optJSONArray("data") ?: data.optJSONArray("orders")

            allOrders.clear()

            if (arr != null) {
                for (i in 0 until arr.length()) {
                    allOrders.add(Order.fromJson(arr.getJSONObject(i)))
                }
            }

            showFilteredOrders()
        }, {
            toast(it)
        })
    }

    private fun showFilteredOrders() {
        val rows = allOrders.filter { matchFilter(it) }

        adapter.setItems(rows)

        empty.text = if (allOrders.isEmpty()) {
            "Belum ada pesanan."
        } else {
            "Tidak ada pesanan pada filter $currentFilter."
        }

        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun matchFilter(order: Order): Boolean {
        return when (currentFilter) {
            "Belum Bayar" ->
                order.metodeBayarKey == "transfer_bank" &&
                    order.statusBayarKey in listOf("menunggu_pembayaran", "menunggu_verifikasi", "ditolak")

            "Diproses" ->
                order.statusKey in listOf("diproses", "disiapkan")

            "Dikirim" ->
                order.statusKey in listOf("siap_diambil", "dalam_pengantaran")

            "Selesai" ->
                order.statusKey == "selesai"

            "Dibatalkan" ->
                order.statusKey == "dibatalkan"

            else -> true
        }
    }

    private fun renderFilterButtons() {
        filterWrap.removeAllViews()

        for (filter in filters) {
            val btn = TextView(requireContext())
            btn.text = filter
            btn.textSize = 12f
            btn.setTypeface(null, Typeface.BOLD)
            btn.gravity = android.view.Gravity.CENTER
            btn.setPadding(dp(12), 0, dp(12), 0)

            if (filter == currentFilter) {
                btn.setTextColor(resources.getColor(R.color.primary, null))
                btn.setBackgroundResource(R.drawable.bg_button_outline_orange)
            } else {
                btn.setTextColor(resources.getColor(R.color.text_muted, null))
                btn.setBackgroundResource(R.drawable.bg_product_card)
            }

            btn.setOnClickListener {
                currentFilter = filter
                renderFilterButtons()
                showFilteredOrders()
            }

            filterWrap.addView(
                btn,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(34)
                ).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            )
        }
    }

    private fun openDetail(order: Order) {
        startActivity(
            Intent(requireContext(), OrderDetailActivity::class.java)
                .putExtra("id", order.id)
        )
    }

    private fun showOrderPopup(anchor: View, order: Order) {
        val popup = PopupMenu(requireContext(), anchor)

        popup.menu.add(0, MENU_DETAIL, 0, "Lihat Detail")
        popup.menu.add(0, MENU_QR, 1, "Lihat QR")
        popup.menu.add(0, MENU_INVOICE, 2, "Lihat Invoice")

        if (order.isSelesai) {
            popup.menu.add(0, MENU_REVIEW, 3, "Beri Ulasan")
        }

        if (order.canConfirmReceived) {
            popup.menu.add(0, MENU_RECEIVED, 4, "Konfirmasi Diterima")
        }

        if (order.canCancel) {
            popup.menu.add(0, MENU_CANCEL, 5, "Batalkan Pesanan")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_DETAIL -> openDetail(order)
                MENU_QR -> showQrPopup(order)
                MENU_INVOICE -> openInvoice(order)
                MENU_REVIEW -> chooseProductForReview(order)
                MENU_RECEIVED -> confirmReceived(order)
                MENU_CANCEL -> confirmCancel(order)
            }
            true
        }

        popup.show()
    }

    private fun openInvoice(order: Order) {
        startActivity(
            Intent(requireContext(), InvoiceViewerActivity::class.java)
                .putExtra("order_id", order.id)
                .putExtra("invoice", order.invoice.ifBlank { "Invoice" })
                .putExtra("invoice_url", order.invoiceUrl)
        )
    }

    private fun confirmCancel(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("Batalkan Pesanan")
            .setMessage("Yakin ingin membatalkan pesanan ${order.invoice}?")
            .setNegativeButton("Tidak", null)
            .setPositiveButton("Batalkan") { _, _ ->
                ApiClient.request(requireContext(), Request.Method.PATCH, "/orders/${order.id}/cancel", null, {
                    toast("Pesanan berhasil dibatalkan")
                    loadOrders()
                }, {
                    toast(it)
                })
            }
            .show()
    }

    private fun confirmReceived(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Pesanan")
            .setMessage("Konfirmasi bahwa pesanan ${order.invoice} sudah diterima?")
            .setNegativeButton("Belum", null)
            .setPositiveButton("Sudah Diterima") { _, _ ->
                ApiClient.request(requireContext(), Request.Method.PATCH, "/orders/${order.id}/received", null, {
                    toast("Pesanan dikonfirmasi diterima")
                    loadOrders()
                }, {
                    toast(it)
                })
            }
            .show()
    }

    private fun chooseProductForReview(order: Order) {
        if (order.products.isEmpty()) {
            toast("Buka detail pesanan untuk memberi ulasan.")
            openDetail(order)
            return
        }

        val products = order.products
        val names = products.map { product ->
            if (order.reviewedProductIds.contains(product.produkId)) {
                product.nama + " (sudah diulas)"
            } else {
                product.nama
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Produk")
            .setItems(names) { _, index ->
                val product = products[index]

                if (order.reviewedProductIds.contains(product.produkId)) {
                    toast("Produk ini sudah diberi ulasan.")
                } else {
                    openReview(order, product)
                }
            }
            .show()
    }

    private fun openReview(order: Order, product: OrderProduct) {
        startActivity(
            Intent(requireContext(), ReviewActivity::class.java)
                .putExtra("order_id", order.id)
                .putExtra("product_id", product.produkId)
                .putExtra("product_name", product.nama)
        )
    }

    private fun showQrPopup(order: Order) {
        val view = layoutInflater.inflate(R.layout.dialog_qr_simple, null)

        view.findViewById<TextView>(R.id.txtQrTitle).text = order.invoice
        view.findViewById<TextView>(R.id.txtQrInfo).text =
            "Tunjukkan QR ini saat mengambil pesanan di toko."

        val imgQr = view.findViewById<ImageView>(R.id.imgQr)
        val txtToken = view.findViewById<TextView>(R.id.txtQrToken)

        val payload = order.qrUrl?.takeIf { it.isNotBlank() } ?: order.qrToken?.takeIf { it.isNotBlank() }
        val bitmap = payload?.let { QrUtil.generate(it, 320) }

        if (bitmap != null) {
            imgQr.setImageBitmap(bitmap)
            txtToken.text = order.qrToken ?: order.qrUrl ?: "QR pengambilan"
        } else {
            imgQr.setImageResource(R.drawable.ic_box)
            txtToken.text = order.qrToken ?: "QR belum tersedia."
        }

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
