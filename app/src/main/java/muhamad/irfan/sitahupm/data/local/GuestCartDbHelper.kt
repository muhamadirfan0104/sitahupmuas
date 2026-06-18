package muhamad.irfan.sitahupm.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import muhamad.irfan.sitahupm.data.model.Product

data class GuestCartItem(
    val produkId: Int,
    val nama: String,
    val harga: Double,
    val jumlah: Int,
    val subtotal: Double,
    val stok: Int,
    val satuan: String,
    val gambar: String?
)

class GuestCartDbHelper(context: Context) : SQLiteOpenHelper(context, "sitahu_guest_cart.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE guest_cart(
                produk_id INTEGER PRIMARY KEY,
                nama TEXT,
                harga REAL,
                jumlah INTEGER,
                stok INTEGER,
                satuan TEXT,
                gambar TEXT,
                waktu INTEGER
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS guest_cart")
        onCreate(db)
    }

    fun add(product: Product, qty: Int = 1) {
        val current = getQty(product.id)
        val newQty = current + qty
        writableDatabase.insertWithOnConflict("guest_cart", null, ContentValues().apply {
            put("produk_id", product.id)
            put("nama", product.nama)
            put("harga", product.harga)
            put("jumlah", newQty)
            put("stok", product.stok)
            put("satuan", product.satuan)
            put("gambar", product.gambar ?: "")
            put("waktu", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun getQty(productId: Int): Int {
        readableDatabase.rawQuery("SELECT jumlah FROM guest_cart WHERE produk_id=?", arrayOf(productId.toString())).use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun increment(productId: Int) {
        writableDatabase.execSQL("UPDATE guest_cart SET jumlah = jumlah + 1, waktu=? WHERE produk_id=?", arrayOf(System.currentTimeMillis(), productId))
    }


    fun decrement(productId: Int) {
        val current = getQty(productId)
        if (current <= 1) {
            remove(productId)
        } else {
            writableDatabase.execSQL(
                "UPDATE guest_cart SET jumlah = jumlah - 1, waktu=? WHERE produk_id=?",
                arrayOf(System.currentTimeMillis(), productId)
            )
        }
    }

    fun remove(productId: Int) {
        writableDatabase.delete("guest_cart", "produk_id=?", arrayOf(productId.toString()))
    }

    fun clear() {
        writableDatabase.delete("guest_cart", null, null)
    }

    fun items(): List<GuestCartItem> {
        val rows = mutableListOf<GuestCartItem>()
        readableDatabase.rawQuery(
            "SELECT produk_id,nama,harga,jumlah,stok,satuan,gambar FROM guest_cart ORDER BY waktu DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val harga = c.getDouble(2)
                val jumlah = c.getInt(3)
                rows.add(
                    GuestCartItem(
                        produkId = c.getInt(0),
                        nama = c.getString(1) ?: "",
                        harga = harga,
                        jumlah = jumlah,
                        subtotal = harga * jumlah,
                        stok = c.getInt(4),
                        satuan = c.getString(5) ?: "pack",
                        gambar = c.getString(6).ifBlank { null }
                    )
                )
            }
        }
        return rows
    }

    fun total(): Double = items().sumOf { it.subtotal }
}
