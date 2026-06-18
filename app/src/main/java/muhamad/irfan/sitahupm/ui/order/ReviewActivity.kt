package muhamad.irfan.sitahupm.ui.order

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import java.io.File

class ReviewActivity : AppCompatActivity() {
    private var orderId = 0
    private var productId = 0
    private val foto = mutableListOf<Uri>()
    private var fotoKamera: Uri? = null
    private val batasFoto = 5

    private val izinKamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) bukaKamera() else pesan("Izin kamera ditolak")
    }

    private val ambilGaleri = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { data ->
        foto.addAll(data.take(batasFoto - foto.size))
        tampilFoto()
    }

    private val ambilKamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { sukses ->
        if (sukses && foto.size < batasFoto) fotoKamera?.let { foto.add(it) }
        tampilFoto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_simple)

        orderId = intent.getIntExtra("order_id", 0)
        productId = intent.getIntExtra("product_id", 0)
        findViewById<TextView>(R.id.txtProduct).text = intent.getStringExtra("product_name") ?: "Produk"

        findViewById<TextView>(R.id.btnPhoto).setOnClickListener { pilihFoto() }
        findViewById<TextView>(R.id.btnClearPhotos).setOnClickListener { foto.clear(); tampilFoto() }
        findViewById<TextView>(R.id.btnSubmit).setOnClickListener { kirim() }
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tampilFoto()
    }

    private fun pilihFoto() {
        AlertDialog.Builder(this)
            .setTitle("Foto Ulasan")
            .setItems(arrayOf("Kamera", "Galeri")) { _, posisi ->
                if (posisi == 0) izinKamera.launch(Manifest.permission.CAMERA)
                else ambilGaleri.launch("image/*")
            }
            .show()
    }

    private fun bukaKamera() {
        val file = File.createTempFile("foto_ulasan_", ".jpg", cacheDir)
        fotoKamera = FileProvider.getUriForFile(this, "$packageName.provider", file)
        fotoKamera?.let {
            ambilKamera.launch(it)
        }
    }

    private fun tampilFoto() {
        findViewById<TextView>(R.id.txtPhoto).text =
            if (foto.isEmpty()) "Belum memilih foto" else "${foto.size} foto dipilih"
    }

    private fun kirim() {
        val rating = findViewById<EditText>(R.id.edtRating).text.toString().toIntOrNull()
        val komentar = findViewById<EditText>(R.id.edtComment).text.toString().trim()

        if (rating == null || rating !in 1..5) return pesan("Rating harus 1 sampai 5")

        val tombol = findViewById<TextView>(R.id.btnSubmit)
        tombol.isEnabled = false
        tombol.text = "Mengirim..."

        ApiClient.uploadReview(this, orderId, productId, rating, komentar, foto, {
            pesan("Ulasan berhasil dikirim")
            finish()
        }, {
            tombol.isEnabled = true
            tombol.text = "Kirim Ulasan"
            pesan(it)
        })
    }

    private fun pesan(teks: String) {
        Toast.makeText(this, teks, Toast.LENGTH_SHORT).show()
    }
}
