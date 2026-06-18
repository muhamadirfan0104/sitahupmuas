package muhamad.irfan.sitahupm.ui.profile

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
import muhamad.irfan.sitahupm.util.ImageUtil
import org.json.JSONObject

data class ReviewMediaItem(
    val nama: String,
    val invoice: String,
    val rating: Int,
    val komentar: String,
    val foto: List<String>
)

class MyReviewsActivity : AppCompatActivity() {
    private val data = mutableListOf<ReviewMediaItem>()
    private val adapter = ReviewMediaAdapter(data) { bukaFoto(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews_simple)

        findViewById<RecyclerView>(R.id.rvReviews).apply {
            layoutManager = LinearLayoutManager(this@MyReviewsActivity)
            adapter = this@MyReviewsActivity.adapter
        }

        findViewById<TextView>(R.id.btnRefresh).setOnClickListener { loadData() }
        loadData()
    }

    private fun loadData() {
        ApiClient.request(this, Request.Method.GET, "/reviews/me", null, { res ->
            val arr = res.getJSONObject("data").optJSONArray("data")
            data.clear()

            if (arr != null) for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                data.add(
                    ReviewMediaItem(
                        item.optString("nama_produk", "Produk"),
                        item.optString("nomor_invoice", "-"),
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

    private fun bukaFoto(item: ReviewMediaItem) {
        if (item.foto.isEmpty()) return
        startActivity(
            Intent(this, ReviewMediaViewerActivity::class.java)
                .putExtra("title", item.nama)
                .putStringArrayListExtra("photo_urls", ArrayList(item.foto))
        )
    }
}

class ReviewMediaAdapter(
    private val data: List<ReviewMediaItem>,
    private val klikFoto: (ReviewMediaItem) -> Unit
) : RecyclerView.Adapter<ReviewMediaAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val produk: TextView = v.findViewById(R.id.txtProduct)
        val info: TextView = v.findViewById(R.id.txtInfo)
        val komentar: TextView = v.findViewById(R.id.txtComment)
        val gambar: ImageView = v.findViewById(R.id.imgReview)
        val media: TextView = v.findViewById(R.id.txtMediaInfo)
        val tombol: TextView = v.findViewById(R.id.btnPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_review_media, parent, false)
        return VH(v)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(h: VH, p: Int) {
        val item = data[p]
        h.produk.text = item.nama
        h.info.text = "Invoice: ${item.invoice}\nRating: ${item.rating}/5"
        h.komentar.text = item.komentar
        h.media.text = "${item.foto.size} foto ulasan"

        if (item.foto.isNotEmpty()) {
            h.gambar.visibility = View.VISIBLE
            h.tombol.visibility = View.VISIBLE
            h.tombol.text = "Lihat Foto"
            ImageUtil.load(h.gambar, item.foto[0])
            h.gambar.setOnClickListener { klikFoto(item) }
            h.tombol.setOnClickListener { klikFoto(item) }
        } else {
            h.gambar.visibility = View.GONE
            h.tombol.visibility = View.GONE
        }
    }
}
