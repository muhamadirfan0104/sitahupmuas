package muhamad.irfan.sitahupm.ui.checkout

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.data.model.Address
import muhamad.irfan.sitahupm.data.model.CartItem
import muhamad.irfan.sitahupm.ui.auth.AuthActivity
import muhamad.irfan.sitahupm.util.FormatUtil
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.*

class CheckoutActivity : AppCompatActivity() {
    private val addresses = mutableListOf<Address>()
    private val selectedProdukIds = mutableSetOf<Int>()
    private var subtotal = 0.0
    private var ongkir = 0.0
    private var tarifPerKm = 0.0
    private var minimumOngkir = 0.0
    private var radiusMaksimalKm = 0.0
    private var storeLat: Double? = null
    private var storeLng: Double? = null
    private var metodeAmbil = "ambil_toko"
    private var metodeBayar = "cod"
    private var date = ""
    private var time = ""
    private var proofUri: Uri? = null

    private lateinit var loginBlock: View
    private lateinit var checkoutContent: View
    private lateinit var addressBlock: View
    private lateinit var transferBlock: View
    private lateinit var txtItems: TextView
    private lateinit var txtSubtotal: TextView
    private lateinit var txtOngkir: TextView
    private lateinit var txtTotal: TextView
    private lateinit var txtProofName: TextView
    private lateinit var txtBank: TextView
    private lateinit var txtOngkirInfo: TextView
    private lateinit var spinnerAddress: Spinner
    private lateinit var radioPickupStore: RadioButton
    private lateinit var radioPickupCourier: RadioButton
    private lateinit var radioCod: RadioButton
    private lateinit var radioTransfer: RadioButton
    private lateinit var chkAgree: CheckBox

    private val pickProof = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        proofUri = uri
        txtProofName.text = if (uri == null) "Belum ada file dipilih" else getFileName(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        intent.getIntArrayExtra("selected_produk_ids")?.forEach { selectedProdukIds.add(it) }
        bindViews()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        if (!SessionManager(this).isLogin()) {
            renderLoginRequired()
            return
        }

        renderCheckout()
        loadStore()
        loadCart()
        loadAddress()
    }

    private fun bindViews() {
        loginBlock = findViewById(R.id.loginBlock)
        checkoutContent = findViewById(R.id.checkoutContent)
        addressBlock = findViewById(R.id.addressBlock)
        transferBlock = findViewById(R.id.transferBlock)
        txtItems = findViewById(R.id.txtItems)
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtOngkir = findViewById(R.id.txtOngkir)
        txtTotal = findViewById(R.id.txtTotal)
        txtProofName = findViewById(R.id.txtProofName)
        txtBank = findViewById(R.id.txtBank)
        txtOngkirInfo = findViewById(R.id.txtOngkirInfo)
        spinnerAddress = findViewById(R.id.spinnerAddress)
        radioPickupStore = findViewById(R.id.radioPickupStore)
        radioPickupCourier = findViewById(R.id.radioPickupCourier)
        radioCod = findViewById(R.id.radioCod)
        radioTransfer = findViewById(R.id.radioTransfer)
        chkAgree = findViewById(R.id.chkAgree)
    }

    private fun renderLoginRequired() {
        loginBlock.visibility = View.VISIBLE
        checkoutContent.visibility = View.GONE
        findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }

    private fun renderCheckout() {
        loginBlock.visibility = View.GONE
        checkoutContent.visibility = View.VISIBLE

        findViewById<RadioGroup>(R.id.radioPickupGroup).setOnCheckedChangeListener { _, checkedId ->
            metodeAmbil = if (checkedId == R.id.radioPickupCourier) "kurir_toko" else "ambil_toko"
            addressBlock.visibility = if (metodeAmbil == "kurir_toko") View.VISIBLE else View.GONE
            calculateOngkir()
        }

        findViewById<RadioGroup>(R.id.radioPayGroup).setOnCheckedChangeListener { _, checkedId ->
            metodeBayar = if (checkedId == R.id.radioTransfer) "transfer_bank" else "cod"
            transferBlock.visibility = if (metodeBayar == "transfer_bank") View.VISIBLE else View.GONE
        }

        spinnerAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { calculateOngkir() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<TextView>(R.id.btnDate).setOnClickListener { pickDate() }
        findViewById<TextView>(R.id.btnTime).setOnClickListener { pickTime() }
        findViewById<TextView>(R.id.btnPickProof).setOnClickListener { pickProof.launch("image/*") }
        findViewById<TextView>(R.id.btnSubmitOrder).setOnClickListener { checkout() }

        radioPickupStore.isChecked = true
        radioCod.isChecked = true
        updateTotal()
    }

    private fun loadStore() {
        ApiClient.request(this, Request.Method.GET, "/store", null, { response ->
            val data = response.getJSONObject("data")
            tarifPerKm = data.optDouble("tarif_per_km", 0.0)
            minimumOngkir = data.optDouble("biaya_minimum_pengiriman", 0.0)
            radiusMaksimalKm = data.optDouble("radius_maksimal_km", 0.0)
            storeLat = if (data.isNull("latitude_toko")) null else data.optDouble("latitude_toko")
            storeLng = if (data.isNull("longitude_toko")) null else data.optDouble("longitude_toko")

            val rekening = data.optJSONArray("rekening_toko")
            if (rekening != null && rekening.length() > 0) {
                val r = rekening.getJSONObject(0)
                txtBank.text = "${r.optString("nama_bank")} ${r.optString("nomor_rekening")} a.n. ${r.optString("atas_nama")}"
            } else {
                txtBank.text = "${data.optString("bank_nama", "BCA")} ${data.optString("bank_nomor_rekening", "1234567890")} a.n. ${data.optString("bank_atas_nama", "MAR Tahu")}"
            }
            calculateOngkir()
        }, { calculateOngkir() })
    }

    private fun loadCart() {
        ApiClient.request(this, Request.Method.GET, "/cart", null, { response ->
            val data = response.getJSONObject("data")
            val arr = data.getJSONArray("items")
            val itemsText = StringBuilder()
            subtotal = 0.0
            var count = 0

            for (i in 0 until arr.length()) {
                val item = CartItem.fromJson(arr.getJSONObject(i))
                if (selectedProdukIds.isNotEmpty() && !selectedProdukIds.contains(item.produkId)) continue
                subtotal += item.subtotal
                count++
                itemsText.append("${item.jumlah}x ${item.nama} - ${FormatUtil.rupiah(item.subtotal)}\\n")
            }

            txtItems.text = if (count == 0) "Keranjang kosong atau produk belum dipilih." else itemsText.toString().trim()
            updateTotal()
        }, { toast(it) })
    }

    private fun loadAddress() {
        ApiClient.request(this, Request.Method.GET, "/addresses", null, { response ->
            val arr = response.getJSONArray("data")
            addresses.clear()

            for (i in 0 until arr.length()) addresses.add(Address.fromJson(arr.getJSONObject(i)))

            val labels = addresses.map { "${it.nama} - ${it.alamat.take(32)}" }.ifEmpty { listOf("Belum ada alamat") }
            spinnerAddress.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
            calculateOngkir()
        }, {
            spinnerAddress.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Belum ada alamat"))
            calculateOngkir()
        })
    }

    private fun calculateOngkir() {
        if (metodeAmbil != "kurir_toko") {
            ongkir = 0.0
            txtOngkirInfo.text = "Ambil di toko tanpa ongkir."
            updateTotal()
            return
        }

        val address = addresses.getOrNull(spinnerAddress.selectedItemPosition)
        val slat = storeLat
        val slng = storeLng
        if (address?.lat != null && address.lng != null && slat != null && slng != null && tarifPerKm > 0) {
            val jarak = haversineKm(slat, slng, address.lat, address.lng)
            ongkir = max(minimumOngkir, ceil((jarak * tarifPerKm) / 100.0) * 100.0)
            val radiusText = if (radiusMaksimalKm > 0 && jarak > radiusMaksimalKm) " • Di luar area pengiriman" else ""
            txtOngkirInfo.text = "Estimasi ongkir: ${FormatUtil.rupiah(ongkir)}$radiusText"
        } else {
            ongkir = minimumOngkir
            txtOngkirInfo.text = "Lengkapi titik alamat agar ongkir dapat dihitung."
        }
        updateTotal()
    }

    private fun updateTotal() {
        txtSubtotal.text = FormatUtil.rupiah(subtotal)
        txtOngkir.text = FormatUtil.rupiah(ongkir)
        txtTotal.text = FormatUtil.rupiah(subtotal + ongkir)
    }

    private fun pickDate() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            date = "%04d-%02d-%02d".format(y, m + 1, d)
            findViewById<TextView>(R.id.btnDate).text = date
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            time = "%02d:%02d".format(h, m)
            findViewById<TextView>(R.id.btnTime).text = time
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }

    private fun checkout() {
        if (!chkAgree.isChecked) {
            toast("Centang persetujuan dulu")
            return
        }

        if (subtotal <= 0.0) {
            toast("Keranjang kosong atau produk belum dipilih")
            return
        }

        if (metodeAmbil == "kurir_toko" && addresses.isEmpty()) {
            toast("Tambahkan alamat pengiriman dulu")
            return
        }

        if (metodeAmbil == "kurir_toko") {
            val address = addresses.getOrNull(spinnerAddress.selectedItemPosition)
            if (address?.lat == null || address.lng == null || storeLat == null || storeLng == null) {
                toast("Lengkapi titik lokasi alamat dulu")
                return
            }
        }

        if (metodeBayar == "transfer_bank" && proofUri == null) {
            toast("Pilih bukti transfer dulu")
            return
        }

        val body = JSONObject()
            .put("metode_pengambilan", metodeAmbil)
            .put("metode_pembayaran", metodeBayar)
            .put("setuju_pesanan", true)

        if (selectedProdukIds.isNotEmpty()) {
            val arr = org.json.JSONArray()
            selectedProdukIds.forEach { arr.put(it) }
            body.put("selected_produk_ids", arr)
        }

        if (metodeAmbil == "kurir_toko" && addresses.isNotEmpty()) {
            body.put("alamat_pengiriman_id", addresses[spinnerAddress.selectedItemPosition].id)
        }

        if (date.isNotBlank()) body.put("scheduled_date", date)
        if (time.isNotBlank()) body.put("scheduled_time", time)

        ApiClient.request(this, Request.Method.POST, "/checkout", body, { response ->
            if (metodeBayar == "transfer_bank" && proofUri != null) uploadProof(response) else finishSuccess("Pesanan berhasil dibuat")
        }, { toast(it) })
    }

    private fun uploadProof(checkoutResponse: JSONObject) {
        val uri = proofUri ?: return
        val data = checkoutResponse.optJSONObject("data")
        val orderId = data?.optInt("id") ?: data?.optInt("order_id") ?: 0

        if (orderId == 0) {
            finishSuccess("Pesanan dibuat. Bukti transfer bisa dikirim nanti.")
            return
        }

        ApiClient.uploadFile(this, "/orders/$orderId/payment-proof", "bukti_transfer", uri, {
            finishSuccess("Pesanan dan bukti transfer berhasil dikirim")
        }, {
            finishSuccess("Pesanan dibuat, tetapi bukti transfer belum terkirim")
        })
    }

    private fun finishSuccess(message: String) {
        toast(message)
        finish()
    }

    private fun getFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return "bukti_transfer.jpg"
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earth * c
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
