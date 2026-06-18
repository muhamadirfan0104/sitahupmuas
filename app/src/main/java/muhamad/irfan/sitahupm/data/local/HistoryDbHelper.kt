package muhamad.irfan.sitahupm.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import muhamad.irfan.sitahupm.data.model.Product

class HistoryDbHelper(context: Context) : SQLiteOpenHelper(context, "sitahu_local.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE riwayat_pencarian(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                keyword TEXT UNIQUE,
                waktu INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE produk_dilihat(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                produk_id INTEGER UNIQUE,
                nama TEXT,
                harga REAL,
                stok INTEGER,
                satuan TEXT,
                gambar TEXT,
                deskripsi TEXT,
                waktu INTEGER
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS riwayat_pencarian")
        db.execSQL("DROP TABLE IF EXISTS produk_dilihat")
        onCreate(db)
    }

    fun saveSearch(keyword: String) {
        val q = keyword.trim()
        if (q.isBlank()) return
        writableDatabase.delete("riwayat_pencarian", "LOWER(keyword)=LOWER(?)", arrayOf(q))
        writableDatabase.insert("riwayat_pencarian", null, ContentValues().apply {
            put("keyword", q)
            put("waktu", System.currentTimeMillis())
        })
    }

    fun latest(): List<String> {
        val rows = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT keyword FROM riwayat_pencarian ORDER BY waktu DESC LIMIT 8", null).use {
            while (it.moveToNext()) rows.add(it.getString(0))
        }
        return rows
    }

    fun clearSearch() {
        writableDatabase.delete("riwayat_pencarian", null, null)
    }

    fun saveViewed(product: Product) {
        writableDatabase.delete("produk_dilihat", "produk_id=?", arrayOf(product.id.toString()))
        writableDatabase.insert("produk_dilihat", null, ContentValues().apply {
            put("produk_id", product.id)
            put("nama", product.nama)
            put("harga", product.harga)
            put("stok", product.stok)
            put("satuan", product.satuan)
            put("gambar", product.gambar ?: "")
            put("deskripsi", product.deskripsi)
            put("waktu", System.currentTimeMillis())
        })
    }

    fun latestViewed(): List<Product> {
        val rows = mutableListOf<Product>()
        readableDatabase.rawQuery(
            "SELECT produk_id,nama,harga,stok,satuan,gambar,deskripsi FROM produk_dilihat ORDER BY waktu DESC LIMIT 5",
            null
        ).use { c ->
            while (c.moveToNext()) {
                rows.add(
                    Product(
                        id = c.getInt(0),
                        nama = c.getString(1) ?: "",
                        harga = c.getDouble(2),
                        stok = c.getInt(3),
                        satuan = c.getString(4) ?: "pack",
                        gambar = c.getString(5).ifBlank { null },
                        deskripsi = c.getString(6) ?: ""
                    )
                )
            }
        }
        return rows
    }

    fun clearViewed() {
        writableDatabase.delete("produk_dilihat", null, null)
    }
}
