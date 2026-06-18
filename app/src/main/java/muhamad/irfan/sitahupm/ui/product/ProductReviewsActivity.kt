package muhamad.irfan.sitahupm.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.ui.profile.ReviewMediaViewerActivity
import muhamad.irfan.sitahupm.util.ImageUtil
import org.json.JSONObject

data class PublicReviewItem(
    val user: String,
    val rating: Int,
    val komentar: String,
    val foto: List<String>
)

class ProductReviewsActivity : AppCompatActivity() {
    private val data = mutableListOf<PublicReviewItem>()
    private val adapter = PublicReviewAdapter(data) { bukaFoto(it) }
    private var productId = 0
    private var productName = "Ulasan Pembeli"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_reviews)

        productId = intent.getIntExtra("product_id", 0)
        productName = intent.getStringExtra("product_name") ?: "Ulasan Pembeli"

        findViewById<TextView>(R.id.txtTitle).text = productName
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnRefresh).setOnClickListener { loadData() }

        findViewById<RecyclerView>(R.id.rvReviews).apply {
            layoutManager = LinearLayoutManager(this@ProductReviewsActivity)
            adapter = this@ProductReviewsActivity.adapter
        }

        loadData()
    }

    private fun loadData() {
        ApiClient.request(this, Request.Method.GET, "/products/$productId/reviews", null, { res ->
            val arr = res.getJSONObject("data").optJSONArray("data")
            data.clear()

            if (arr != null) for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                data.add(
                    PublicReviewItem(
                        item.optString("user", "Pembeli"),
                        item.optInt("rating"),
                        bersih(item.optString("komentar")),
                        bacaFoto(item)
                    )
                )
            }

            adapter.notifyDataSetChanged()
            findViewById<TextView>(R.id.txtEmpty).visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        }, { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() })
    }

    private fun bacaFoto(item: JSONObject): List<String> {
        val arr = item.optJSONArray("foto_ulasan_list")
        val hasil = mutableListOf<String>()
        if (arr != null) for (i in 0 until arr.length()) hasil.add(arr.getString(i))
        return hasil
    }

    private fun bersih(teks: String): String {
        return if (teks.isBlank() || teks == "null") "-" else teks
    }

    private fun bukaFoto(item: PublicReviewItem) {
        if (item.foto.isEmpty()) return
        startActivity(
            Intent(this, ReviewMediaViewerActivity::class.java)
                .putExtra("title", productName)
                .putStringArrayListExtra("photo_urls", ArrayList(item.foto))
        )
    }
}

class PublicReviewAdapter(
    private val data: List<PublicReviewItem>,
    private val klikFoto: (PublicReviewItem) -> Unit
) : RecyclerView.Adapter<PublicReviewAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val user: TextView = v.findViewById(R.id.txtUser)
        val rating: TextView = v.findViewById(R.id.txtRating)
        val komentar: TextView = v.findViewById(R.id.txtComment)
        val gambar: ImageView = v.findViewById(R.id.imgReview)
        val tombol: TextView = v.findViewById(R.id.btnPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_public_review, parent, false)
        return VH(v)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(h: VH, p: Int) {
        val item = data[p]
        h.user.text = item.user
        h.rating.text = "Rating: ${item.rating}/5"
        h.komentar.text = item.komentar

        if (item.foto.isNotEmpty()) {
            h.gambar.visibility = View.VISIBLE
            h.tombol.visibility = View.VISIBLE
            ImageUtil.load(h.gambar, item.foto[0])
            h.gambar.setOnClickListener { klikFoto(item) }
            h.tombol.setOnClickListener { klikFoto(item) }
        } else {
            h.gambar.visibility = View.GONE
            h.tombol.visibility = View.GONE
        }
    }
}
