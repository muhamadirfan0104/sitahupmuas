package muhamad.irfan.sitahupm.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import org.json.JSONObject

class ProductsFragment : Fragment() {
    private lateinit var adapter: ProductCardAdapter
    private lateinit var empty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.txtPageTitle).text = "Produk"
        view.findViewById<TextView>(R.id.txtPageDesc).text = "Pilih produk yang tersedia."
        view.findViewById<Button>(R.id.btnAction).visibility = View.GONE
        view.findViewById<TextView>(R.id.txtExtra).visibility = View.GONE
        empty = view.findViewById(R.id.txtEmpty)
        adapter = ProductCardAdapter(mutableListOf(), { openDetail(it) }, { add(it) })
        view.findViewById<RecyclerView>(R.id.rvList).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@ProductsFragment.adapter
        }
        load()
    }

    private fun load() {
        val ctx = context ?: return
        ApiClient.request(ctx, Request.Method.GET, "/products?per_page=30&sort=latest", null, { response ->
            if (!isAdded) return@request
            val arr = response.getJSONObject("data").getJSONArray("data")
            val rows = mutableListOf<Product>()
            for (i in 0 until arr.length()) rows.add(Product.fromJson(arr.getJSONObject(i)))
            adapter.setItems(rows)
            empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
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

    private fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
}
