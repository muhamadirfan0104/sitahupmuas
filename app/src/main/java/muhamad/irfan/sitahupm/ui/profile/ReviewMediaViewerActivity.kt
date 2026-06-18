package muhamad.irfan.sitahupm.ui.profile

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.util.ImageUtil

class ReviewMediaViewerActivity : AppCompatActivity() {
    private lateinit var gambar: ImageView
    private lateinit var info: TextView

    private val foto = mutableListOf<String>()
    private var posisi = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_media_viewer)

        gambar = findViewById(R.id.imgMedia)
        info = findViewById(R.id.txtHint)

        findViewById<TextView>(R.id.txtTitle).text = intent.getStringExtra("title") ?: "Foto Ulasan"
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        foto.addAll(intent.getStringArrayListExtra("photo_urls") ?: arrayListOf())
        intent.getStringExtra("url")?.let { foto.add(it) }

        if (foto.isEmpty()) {
            Toast.makeText(this, "Foto tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.btnPrev).setOnClickListener { if (posisi > 0) { posisi--; tampil() } }
        findViewById<TextView>(R.id.btnNext).setOnClickListener { if (posisi < foto.lastIndex) { posisi++; tampil() } }

        tampil()
    }

    private fun tampil() {
        ImageUtil.load(gambar, foto[posisi])
        info.text = "Foto ${posisi + 1} dari ${foto.size}"
    }
}
