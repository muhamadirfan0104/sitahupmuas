package muhamad.irfan.sitahupm.ui.order

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.model.Order
import muhamad.irfan.sitahupm.data.model.OrderItem
import muhamad.irfan.sitahupm.data.model.OrderProduct
import muhamad.irfan.sitahupm.util.FormatUtil
import muhamad.irfan.sitahupm.util.QrUtil

class OrderDetailActivity : AppCompatActivity() {
    private var orderId = 0
    private var currentOrder: Order? = null
    private val detailProducts = mutableListOf<OrderProduct>()
    private var firstResume = true
    private var qrPopupShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail_simple)

        orderId = intent.getIntExtra("id", 0)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnInvoice).setOnClickListener {
            currentOrder?.let { openInvoice(it) }
        }

        findViewById<TextView>(R.id.btnQr).setOnClickListener {
            currentOrder?.let { showQrPopup(it) }
        }

        findViewById<TextView>(R.id.btnReview).setOnClickListener {
            currentOrder?.let { chooseProductForReview(it) }
        }

        findViewById<TextView>(R.id.btnReceived).setOnClickListener {
            currentOrder?.let { confirmReceived(it) }
        }

        findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            currentOrder?.let { confirmCancel(it) }
        }

        loadDetail()
    }

    override fun onResume() {
        super.onResume()

        if (firstResume) {
            firstResume = false
        } else {
            loadDetail()
        }
    }

    private fun loadDetail() {
        ApiClient.request(this, Request.Method.GET, "/orders/$orderId", null, { response ->
            val data = response.getJSONObject("data")
            val order = Order.fromJson(data)
            currentOrder = order

            findViewById<TextView>(R.id.txtInvoice).text = order.invoice.ifBlank { "Detail Pesanan" }
            findViewById<TextView>(R.id.txtStatus).text = buildStatusText(order)
            findViewById<TextView>(R.id.txtItems).text = buildItemsText(data)

            findViewById<TextView>(R.id.btnReview).visibility =
                if (order.isSelesai) View.VISIBLE else View.GONE

            findViewById<TextView>(R.id.btnCancel).visibility =
                if (order.canCancel) View.VISIBLE else View.GONE

            findViewById<TextView>(R.id.btnReceived).visibility =
                if (order.canConfirmReceived) View.VISIBLE else View.GONE

            if (intent.getBooleanExtra("open_qr", false) && !qrPopupShown) {
                qrPopupShown = true
                showQrPopup(order)
            }
        }, {
            toast(it)
        })
    }

    private fun buildStatusText(order: Order): String {
        return "Status pesanan: ${order.status}\n" +
            "Status pembayaran: ${order.statusBayar}\n" +
            "Metode pengambilan: ${order.metodeAmbil}\n" +
            "Metode pembayaran: ${order.metodeBayar}\n" +
            "Total: ${FormatUtil.rupiah(order.total)}"
    }

    private fun buildItemsText(data: org.json.JSONObject): String {
        val arr = data.optJSONArray("items") ?: return "Produk tidak tersedia."

        detailProducts.clear()
        val text = StringBuilder("Produk:\n")

        for (i in 0 until arr.length()) {
            val itemJson = arr.getJSONObject(i)
            val item = OrderItem.fromJson(itemJson)

            detailProducts.add(
                OrderProduct(
                    produkId = item.produkId,
                    nama = item.nama
                )
            )

            text.append("- ")
                .append(item.jumlah)
                .append("x ")
                .append(item.nama)
                .append(" = ")
                .append(FormatUtil.rupiah(item.subtotal))
                .append("\n")
        }

        return text.toString().trim()
    }

    private fun openInvoice(order: Order) {
        startActivity(
            Intent(this, InvoiceViewerActivity::class.java)
                .putExtra("order_id", order.id)
                .putExtra("invoice", order.invoice.ifBlank { "Invoice" })
                .putExtra("invoice_url", order.invoiceUrl)
        )
    }

    private fun confirmCancel(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Pesanan")
            .setMessage("Yakin ingin membatalkan pesanan ${order.invoice}?")
            .setNegativeButton("Tidak", null)
            .setPositiveButton("Batalkan") { _, _ ->
                ApiClient.request(this, Request.Method.PATCH, "/orders/${order.id}/cancel", null, {
                    toast("Pesanan berhasil dibatalkan")
                    loadDetail()
                }, {
                    toast(it)
                })
            }
            .show()
    }

    private fun confirmReceived(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pesanan")
            .setMessage("Konfirmasi bahwa pesanan ${order.invoice} sudah diterima?")
            .setNegativeButton("Belum", null)
            .setPositiveButton("Sudah Diterima") { _, _ ->
                ApiClient.request(this, Request.Method.PATCH, "/orders/${order.id}/received", null, {
                    toast("Pesanan dikonfirmasi diterima")
                    loadDetail()
                }, {
                    toast(it)
                })
            }
            .show()
    }

    private fun chooseProductForReview(order: Order) {
        if (!order.isSelesai) {
            toast("Ulasan hanya bisa setelah pesanan selesai.")
            return
        }

        if (detailProducts.isEmpty()) {
            toast("Data produk belum tersedia.")
            return
        }

        val names = detailProducts.map {
            if (order.reviewedProductIds.contains(it.produkId)) {
                it.nama + " (sudah diulas)"
            } else {
                it.nama
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pilih Produk")
            .setItems(names) { _, index ->
                val product = detailProducts[index]

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
            Intent(this, ReviewActivity::class.java)
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

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
