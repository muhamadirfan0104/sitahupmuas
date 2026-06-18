package muhamad.irfan.sitahupm.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.GuestCartDbHelper
import muhamad.irfan.sitahupm.data.local.HistoryDbHelper
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.data.model.Product
import muhamad.irfan.sitahupm.ui.main.MainActivity
import muhamad.irfan.sitahupm.util.FormatUtil
import muhamad.irfan.sitahupm.util.ImageUtil
import org.json.JSONObject

class ProductDetailActivity : AppCompatActivity() {
    private var productId = 0
    private var product: Product? = null
    private var openCartAfterAdd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail_simple)

        productId = intent.getIntExtra("id", 0)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnCart).setOnClickListener {
            openCartAfterAdd = false
            addToCart()
        }

        findViewById<TextView>(R.id.btnBuyNow).setOnClickListener {
            openCartAfterAdd = true
            addToCart()
        }

        findViewById<TextView>(R.id.btnPublicReviews).setOnClickListener {
            openPublicReviews()
        }

        loadProduct()
    }

    private fun loadProduct() {
        ApiClient.request(this, Request.Method.GET, "/products/$productId", null, { response ->
            val item = Product.fromJson(response.getJSONObject("data"))
            product = item
            HistoryDbHelper(this).saveViewed(item)

            showProduct(item)
            loadReviewsPreview()
        }, {
            toast(it)
        })
    }

    private fun showProduct(item: Product) {
        findViewById<TextView>(R.id.txtName).text = item.nama
        findViewById<TextView>(R.id.txtPrice).text = FormatUtil.rupiah(item.harga)
        findViewById<TextView>(R.id.txtStock).text = "Stok: ${item.stok} ${item.satuan}"
        findViewById<TextView>(R.id.txtDescription).text =
            item.deskripsi.ifBlank { "Deskripsi produk belum tersedia." }
        findViewById<TextView>(R.id.txtExtra).text = buildExtraInfo(item)

        val mainImage = findViewById<ImageView>(R.id.imgProduct)
        ImageUtil.load(mainImage, item.gambarList.firstOrNull() ?: item.gambar)
        showGallery(item)
    }

    private fun showGallery(item: Product) {
        val block = findViewById<View>(R.id.galleryBlock)
        val row = findViewById<LinearLayout>(R.id.imgGallery)
        row.removeAllViews()

        val images = item.gambarList
        block.visibility = if (images.size > 1) View.VISIBLE else View.GONE
        if (images.size <= 1) return

        for (url in images) {
            val image = ImageView(this)
            val size = dp(62)
            val margin = dp(4)
            image.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, 0, margin, 0)
            }
            image.background = getDrawable(R.drawable.bg_product_thumb)
            image.contentDescription = "Foto produk"
            image.setOnClickListener { ImageUtil.load(findViewById(R.id.imgProduct), url) }
            row.addView(image)
            ImageUtil.load(image, url)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadReviewsPreview() {
        val preview = findViewById<TextView>(R.id.txtReviewsPreview)
        preview.text = "Memuat ulasan..."

        ApiClient.request(this, Request.Method.GET, "/products/$productId/reviews", null, { response ->
            val data = response.getJSONObject("data")
            val arr = data.optJSONArray("data") ?: data.optJSONArray("reviews")

            if (arr == null || arr.length() == 0) {
                preview.text = "Belum ada ulasan pembeli untuk produk ini."
                return@request
            }

            val total = data.optInt("total", arr.length())
            val first = arr.getJSONObject(0)
            val user = first.optString("user").ifBlank { "Pembeli" }
            val rating = first.optInt("rating")
            val comment = first.optString("komentar").ifBlank { "Tidak ada komentar." }

            preview.text = "Total ulasan: $total\nTerbaru dari $user ($rating/5):\n$comment"
        }, {
            preview.text = "Ulasan belum dapat dimuat."
        })
    }

    private fun openPublicReviews() {
        val item = product

        startActivity(
            Intent(this, ProductReviewsActivity::class.java)
                .putExtra("product_id", productId)
                .putExtra("product_name", item?.nama ?: "Ulasan Pembeli")
        )
    }

    private fun buildExtraInfo(item: Product): String {
        val text = StringBuilder()

        fun add(label: String, value: String) {
            if (value.isNotBlank()) {
                text.append(label).append(": ").append(value).append("\n")
            }
        }

        add("Satuan", item.satuan)
        add("Isi per Satuan", item.isiPerSatuan)
        add("Berat", item.berat)
        add("Masa Simpan", item.masaSimpan)
        add("Saran Penyimpanan", item.saranPenyimpanan)
        add("Saran Penyajian", item.saranPenyajian)

        return text.toString().trim().ifBlank { "Informasi tambahan belum tersedia." }
    }

    private fun addToCart() {
        val item = product ?: return

        if (!SessionManager(this).isLogin()) {
            GuestCartDbHelper(this).add(item, 1)
            toast("Masuk keranjang")
            if (openCartAfterAdd) openCart()
            return
        }

        val body = JSONObject()
            .put("produk_id", item.id)
            .put("jumlah", 1)

        ApiClient.request(this, Request.Method.POST, "/cart/items", body, {
            toast("Masuk keranjang")
            if (openCartAfterAdd) openCart()
        }, {
            toast(it)
        })
    }

    private fun openCart() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("open_tab", "cart")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
