package muhamad.irfan.sitahupm.ui.address

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import mumayank.com.airlocationlibrary.AirLocation
import org.json.JSONObject

class AddressFormActivity : AppCompatActivity() {
    private var addressId = 0
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private lateinit var airLocation: AirLocation

    private val pickPointLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult

        selectedLat = data.getDoubleExtra("lat", 0.0)
        selectedLng = data.getDoubleExtra("lng", 0.0)
        showSelectedPoint()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_form)

        addressId = intent.getIntExtra("id", 0)

        airLocation = AirLocation(
            this,
            object : AirLocation.Callback {
                override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                    toast("Lokasi belum tersedia. Aktifkan lokasi lalu coba lagi.")
                }

                override fun onSuccess(locations: ArrayList<Location>) {
                    if (locations.isNotEmpty()) {
                        selectedLat = locations[0].latitude
                        selectedLng = locations[0].longitude
                        showSelectedPoint()
                        toast("Lokasi berhasil digunakan")
                    }
                }
            },
            true
        )

        if (addressId > 0) {
            setupEditMode()
        } else {
            setupAddMode()
        }

        findViewById<TextView>(R.id.btnGps).setOnClickListener {
            airLocation.start()
        }

        findViewById<TextView>(R.id.btnPickMap).setOnClickListener {
            openPointPicker()
        }

        findViewById<TextView>(R.id.btnSubmit).setOnClickListener {
            saveAddress()
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        showSelectedPoint()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupAddMode() {
        findViewById<TextView>(R.id.txtTitle).text = "Tambah Alamat"
        findViewById<TextView>(R.id.btnSubmit).text = "Simpan Alamat"
    }

    private fun setupEditMode() {
        findViewById<TextView>(R.id.txtTitle).text = "Edit Alamat"
        findViewById<TextView>(R.id.btnSubmit).text = "Simpan Alamat"

        findViewById<EditText>(R.id.edtNama).setText(intent.getStringExtra("nama") ?: "")
        findViewById<EditText>(R.id.edtTelp).setText(intent.getStringExtra("telepon") ?: "")

        val split = splitAddressAndNote(intent.getStringExtra("alamat") ?: "")
        findViewById<EditText>(R.id.edtAlamat).setText(split.first)
        findViewById<EditText>(R.id.autoCatatan).setText(split.second)
        findViewById<CheckBox>(R.id.chkOption).isChecked = intent.getBooleanExtra("utama", false)

        if (intent.getBooleanExtra("has_point", false)) {
            selectedLat = intent.getDoubleExtra("lat", 0.0)
            selectedLng = intent.getDoubleExtra("lng", 0.0)
        }
    }

    private fun openPointPicker() {
        val lat = selectedLat ?: -7.8166
        val lng = selectedLng ?: 112.0116

        val intent = Intent(this, AddressPickerActivity::class.java)
            .putExtra("lat", lat)
            .putExtra("lng", lng)

        pickPointLauncher.launch(intent)
    }

    private fun showSelectedPoint() {
        val lat = selectedLat
        val lng = selectedLng

        findViewById<TextView>(R.id.txtPoint).text =
            if (lat == null || lng == null) {
                "Titik lokasi belum dipilih"
            } else {
                "Titik lokasi sudah dipilih"
            }
    }

    private fun saveAddress() {
        val name = findViewById<EditText>(R.id.edtNama).text.toString().trim()
        val phone = findViewById<EditText>(R.id.edtTelp).text.toString().trim()
        val address = findViewById<EditText>(R.id.edtAlamat).text.toString().trim()
        val note = findViewById<EditText>(R.id.autoCatatan).text.toString().trim()

        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
            toast("Nama, nomor HP, dan alamat wajib diisi")
            return
        }

        val fullAddress = if (note.isBlank()) address else "$address\nPatokan: $note"

        val body = JSONObject()
            .put("nama_penerima", name)
            .put("telepon", phone)
            .put("alamat_lengkap", fullAddress)
            .put("latitude", selectedLat ?: JSONObject.NULL)
            .put("longitude", selectedLng ?: JSONObject.NULL)
            .put("utama", findViewById<CheckBox>(R.id.chkOption).isChecked)

        val button = findViewById<TextView>(R.id.btnSubmit)
        button.isEnabled = false
        button.text = "Menyimpan..."

        if (addressId > 0) {
            updateAddress(body)
        } else {
            createAddress(body)
        }
    }

    private fun createAddress(body: JSONObject) {
        ApiClient.request(this, Request.Method.POST, "/addresses", body, {
            toast("Alamat berhasil disimpan")
            finish()
        }, {
            resetButton()
            toast(it)
        })
    }

    private fun updateAddress(body: JSONObject) {
        ApiClient.request(this, Request.Method.PUT, "/addresses/$addressId", body, {
            toast("Alamat berhasil diperbarui")
            finish()
        }, {
            resetButton()
            toast(it)
        })
    }

    private fun resetButton() {
        val button = findViewById<TextView>(R.id.btnSubmit)
        button.isEnabled = true
        button.text = if (addressId > 0) "Simpan Alamat" else "Simpan Alamat"
    }

    private fun splitAddressAndNote(value: String): Pair<String, String> {
        val marker = "\nPatokan: "
        return if (value.contains(marker)) {
            val parts = value.split(marker, limit = 2)
            Pair(parts.getOrNull(0) ?: value, parts.getOrNull(1) ?: "")
        } else {
            Pair(value, "")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
