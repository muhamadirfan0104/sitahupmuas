package muhamad.irfan.sitahupm.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.GuestCartDbHelper
import muhamad.irfan.sitahupm.data.local.HistoryDbHelper
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.data.model.Product
import muhamad.irfan.sitahupm.ui.main.MainActivity
import muhamad.irfan.sitahupm.ui.product.ProductCardAdapter
import muhamad.irfan.sitahupm.ui.product.ProductDetailActivity
import org.json.JSONObject

class HomeFragment : Fragment() {
    private lateinit var adapter: ProductCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.btnAction).setOnClickListener {
            (activity as? MainActivity)?.openProductsFromHome()
        }
        view.findViewById<TextView>(R.id.txtSeeAll).setOnClickListener {
            (activity as? MainActivity)?.openProductsFromHome()
        }
        adapter = ProductCardAdapter(mutableListOf(), { openDetail(it) }, { add(it) })
        view.findViewById<RecyclerView>(R.id.rvList).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            isNestedScrollingEnabled = false
            adapter = this@HomeFragment.adapter
        }
        loadStoreInfo(view)
        load()
    }

    private fun loadStoreInfo(view: View) {
        val ctx = context ?: return
        ApiClient.request(ctx, Request.Method.GET, "/store", null, { response ->
            if (!isAdded) return@request
            val data = response.optJSONObject("data") ?: JSONObject()
            val nama = data.optString("nama").ifBlank { "MAR Tahu" }
            val alamat = data.optString("alamat").ifBlank { "Kediri" }
            val jam = data.optString("jam_buka").ifBlank { "07.00 - 17.00" }
            val tentang = data.optString("tentang").ifBlank {
                "Pesan tahu fresh dengan mudah dan ambil sesuai jadwal."
            }
            val area = data.optString("area_pengiriman").ifBlank { "Ambil toko atau kurir toko" }

            view.findViewById<TextView>(R.id.txtHeroTitle).text = "Halo, Pembeli 👋"
            view.findViewById<TextView>(R.id.txtHeroDesc).text = tentang
            view.findViewById<TextView>(R.id.txtBadgeOpen).text = "Buka $jam"
            view.findViewById<TextView>(R.id.txtPageDesc).text = "Produk terbaru dari $nama"
            view.findViewById<TextView>(R.id.txtStoreInfo).text = "$nama • $alamat • $area"
        }, {
            // Kalau API toko gagal, teks bawaan XML tetap dipakai.
        })
    }

    private fun load() {
        val ctx = context ?: return
        ApiClient.request(ctx, Request.Method.GET, "/products?per_page=4&sort=latest", null, { response ->
            if (!isAdded) return@request
            val arr = response.getJSONObject("data").getJSONArray("data")
            val rows = mutableListOf<Product>()
            for (i in 0 until arr.length()) rows.add(Product.fromJson(arr.getJSONObject(i)))
            adapter.setItems(rows)
        }, { if (isAdded) toast(it) })
    }

    private fun add(product: Product) {
        val ctx = context ?: return
        if (!SessionManager(ctx).isLogin()) {
            GuestCartDbHelper(ctx).add(product)
            toast("Masuk keranjang")
            return
        }
        ApiClient.request(ctx, Request.Method.POST, "/cart/items", JSONObject().put("produk_id", product.id).put("jumlah", 1), {
            if (isAdded) toast("Masuk keranjang")
        }, { if (isAdded) toast(it) })
    }

    private fun openDetail(product: Product) {
        val ctx = context ?: return
        HistoryDbHelper(ctx).saveViewed(product)
        startActivity(Intent(ctx, ProductDetailActivity::class.java).putExtra("id", product.id))
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
