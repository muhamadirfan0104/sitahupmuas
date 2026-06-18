package muhamad.irfan.sitahupm.data.model

import org.json.JSONObject

data class User(val id: Int = 0, val name: String = "", val email: String = "", val telepon: String = "") {
    companion object { fun fromJson(o: JSONObject?) = User(o?.optInt("id") ?: 0, o?.optString("name") ?: "", o?.optString("email") ?: "", o?.optString("telepon") ?: "") }
}

data class Product(
    val id: Int,
    val nama: String,
    val harga: Double,
    val stok: Int,
    val satuan: String,
    val gambar: String?,
    val deskripsi: String = "",
    val isiPerSatuan: String = "",
    val berat: String = "",
    val masaSimpan: String = "",
    val saranPenyimpanan: String = "",
    val saranPenyajian: String = "",
    val aktif: Boolean = true,
    val createdAt: String = "",
    val gambarList: List<String> = emptyList()
) {
    companion object {
        fun fromJson(o: JSONObject) = Product(
            id = o.optInt("id"),
            nama = o.optString("nama"),
            harga = o.optDouble("harga"),
            stok = o.optInt("stok"),
            satuan = o.optString("satuan"),
            gambar = o.optString("gambar_utama").ifBlank { null },
            deskripsi = o.optString("deskripsi"),
            isiPerSatuan = o.optString("isi_per_satuan"),
            berat = o.optString("berat"),
            masaSimpan = o.optString("masa_simpan"),
            saranPenyimpanan = o.optString("saran_penyimpanan"),
            saranPenyajian = o.optString("saran_penyajian"),
            aktif = o.optBoolean("aktif", true),
            createdAt = o.optString("created_at"),
            gambarList = parseImages(o)
        )

        private fun parseImages(o: JSONObject): List<String> {
            val rows = mutableListOf<String>()
            val utama = o.optString("gambar_utama").trim()
            if (utama.isNotBlank() && utama != "null") rows.add(utama)

            val arr = o.optJSONArray("gambar")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val url = arr.optString(i).trim()
                    if (url.isNotBlank() && url != "null" && !rows.contains(url)) rows.add(url)
                }
            }
            return rows
        }
    }
}

data class CartItem(val id: Int, val produkId: Int, val nama: String, val harga: Double, val jumlah: Int, val subtotal: Double, val stok: Int, val satuan: String, val gambar: String?) {
    companion object {
        fun fromJson(o: JSONObject) = CartItem(
            o.optInt("id"),
            o.optInt("produk_id"),
            o.optString("nama_produk"),
            o.optDouble("harga_satuan"),
            o.optInt("jumlah"),
            o.optDouble("subtotal"),
            o.optInt("stok_produk"),
            o.optString("satuan").ifBlank { "pack" },
            o.optString("gambar_utama").ifBlank { null }
        )
    }
}

data class OrderItem(
    val produkId: Int,
    val nama: String,
    val jumlah: Int,
    val subtotal: Double
) {
    companion object {
        fun fromJson(o: JSONObject) = OrderItem(
            produkId = o.optInt("produk_id"),
            nama = o.optString("nama_produk"),
            jumlah = o.optInt("jumlah"),
            subtotal = o.optDouble("subtotal")
        )
    }
}

data class OrderProduct(
    val produkId: Int,
    val nama: String
)

data class Order(
    val id: Int,
    val invoice: String,
    val status: String,
    val statusBayar: String,
    val metodeAmbil: String,
    val metodeBayar: String,
    val total: Double,
    val qrToken: String?,
    val qrUrl: String?,
    val invoiceUrl: String?,
    val products: List<OrderProduct> = emptyList(),
    val reviewedProductIds: List<Int> = emptyList(),
    val statusKey: String = "",
    val statusBayarKey: String = "",
    val metodeBayarKey: String = ""
) {
    val isSelesai: Boolean
        get() = statusKey == "selesai"

    val canCancel: Boolean
        get() = statusKey in listOf("menunggu_pembayaran", "menunggu_verifikasi") &&
            statusKey !in listOf("selesai", "dibatalkan")

    val canConfirmReceived: Boolean
        get() = statusKey == "dalam_pengantaran"

    companion object {
        fun label(key: String): String {
            return when (key) {
                "menunggu_pembayaran" -> "Menunggu Pembayaran"
                "menunggu_verifikasi" -> "Menunggu Verifikasi"
                "diproses" -> "Diproses"
                "disiapkan" -> "Disiapkan"
                "siap_diambil" -> "Siap Diambil"
                "dalam_pengantaran" -> "Dalam Pengantaran"
                "selesai" -> "Selesai"
                "dibatalkan" -> "Dibatalkan"
                "dibayar" -> "Dibayar"
                "ditolak" -> "Ditolak"
                "cod" -> "COD"
                "transfer_bank" -> "Transfer Bank"
                else -> key.replace("_", " ").replaceFirstChar { it.uppercase() }
            }
        }

        fun fromJson(o: JSONObject): Order {
            val products = mutableListOf<OrderProduct>()
            val items = o.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    products.add(OrderProduct(item.optInt("produk_id"), item.optString("nama_produk").ifBlank { "Produk" }))
                }
            }

            val reviewedIds = mutableListOf<Int>()
            val reviewed = o.optJSONArray("produk_sudah_diulas")
            if (reviewed != null) for (i in 0 until reviewed.length()) reviewedIds.add(reviewed.optInt(i))

            val statusRaw = o.optString("status")
            val bayarRaw = o.optString("status_pembayaran")
            val metodeBayarRaw = o.optString("metode_pembayaran")

            return Order(
                id = o.optInt("id"),
                invoice = o.optString("nomor_invoice"),
                status = o.optString("status_label").ifBlank { label(statusRaw) },
                statusBayar = o.optString("status_pembayaran_label").ifBlank { label(bayarRaw) },
                metodeAmbil = o.optString("metode_pengambilan_label"),
                metodeBayar = o.optString("metode_pembayaran_label").ifBlank { label(metodeBayarRaw) },
                total = o.optDouble("total_bayar"),
                qrToken = o.optString("qr_claim_token").ifBlank { null },
                qrUrl = o.optString("qr_claim_url").ifBlank { null },
                invoiceUrl = o.optString("invoice_url").ifBlank { null },
                products = products,
                reviewedProductIds = reviewedIds,
                statusKey = statusRaw,
                statusBayarKey = bayarRaw,
                metodeBayarKey = metodeBayarRaw
            )
        }
    }
}

data class Address(val id: Int, val nama: String, val telepon: String, val alamat: String, val lat: Double?, val lng: Double?, val utama: Boolean) {
    companion object { fun fromJson(o: JSONObject) = Address(o.optInt("id"), o.optString("nama_penerima"), o.optString("telepon"), o.optString("alamat_lengkap"), if (o.isNull("latitude")) null else o.optDouble("latitude"), if (o.isNull("longitude")) null else o.optDouble("longitude"), o.optBoolean("utama")) }
}
