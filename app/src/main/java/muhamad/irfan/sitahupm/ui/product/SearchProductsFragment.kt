package muhamad.irfan.sitahupm.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import muhamad.irfan.sitahupm.ui.main.SearchReceiver
import org.json.JSONObject

class SearchProductsFragment : Fragment(), SearchReceiver {
    private lateinit var db: HistoryDbHelper
    private lateinit var historyChipWrap: LinearLayout
    private lateinit var txtNoHistory: TextView
    private lateinit var txtNoViewed: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtDesc: TextView
    private lateinit var btnHistoryMode: TextView
    private lateinit var historyBlock: View
    private lateinit var viewedBlock: View
    private lateinit var rvResults: RecyclerView
    private lateinit var txtEmptyResult: TextView
    private lateinit var viewedAdapter: ProductCardAdapter
    private lateinit var resultAdapter: ProductCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = HistoryDbHelper(requireContext())
        historyChipWrap = view.findViewById(R.id.historyChipWrap)
        txtNoHistory = view.findViewById(R.id.txtNoHistory)
        txtNoViewed = view.findViewById(R.id.txtNoViewed)
        txtTitle = view.findViewById(R.id.txtSearchTitle)
        txtDesc = view.findViewById(R.id.txtSearchDesc)
        btnHistoryMode = view.findViewById(R.id.btnHistoryMode)
        historyBlock = view.findViewById(R.id.historyBlock)
        viewedBlock = view.findViewById(R.id.viewedBlock)
        rvResults = view.findViewById(R.id.rvResults)
        txtEmptyResult = view.findViewById(R.id.txtEmptyResult)

        viewedAdapter = ProductCardAdapter(mutableListOf(), { openDetail(it) }, { add(it) })
        resultAdapter = ProductCardAdapter(mutableListOf(), { openDetail(it) }, { add(it) })

        view.findViewById<RecyclerView>(R.id.rvViewed).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            isNestedScrollingEnabled = false
            adapter = viewedAdapter
        }

        rvResults.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            isNestedScrollingEnabled = false
            adapter = resultAdapter
        }

        view.findViewById<TextView>(R.id.btnClearHistory).setOnClickListener {
            db.clearSearch()
            renderHistoryMode()
        }

        view.findViewById<TextView>(R.id.btnClearViewed).setOnClickListener {
            db.clearViewed()
            renderHistoryMode()
        }

        btnHistoryMode.setOnClickListener { renderHistoryMode() }

        renderHistoryMode()
    }

    override fun onSearchSubmit(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            renderHistoryMode()
            return
        }
        db.saveSearch(q)
        loadResults(q)
    }

    private fun renderHistoryMode() {
        txtTitle.text = "Pencarian Produk"
        txtDesc.text = "Cari produk yang kamu inginkan."
        btnHistoryMode.visibility = View.GONE
        historyBlock.visibility = View.VISIBLE
        viewedBlock.visibility = View.VISIBLE
        rvResults.visibility = View.GONE
        txtEmptyResult.visibility = View.GONE

        renderHistoryChips()

        val viewed = db.latestViewed()
        viewedAdapter.setItems(viewed)
        txtNoViewed.visibility = if (viewed.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderHistoryChips() {
        historyChipWrap.removeAllViews()
        val histories = db.latest()
        txtNoHistory.visibility = if (histories.isEmpty()) View.VISIBLE else View.GONE

        for (keyword in histories) {
            val chip = TextView(requireContext()).apply {
                text = keyword
                setTextColor(resources.getColor(R.color.primary, null))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = resources.getDrawable(R.drawable.bg_button_outline_orange, null)
                setOnClickListener {
                    onSearchSubmit(keyword)
                    (activity as? MainActivity)?.setSearchText(keyword)
                }
            }

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dp(8), 0)
            historyChipWrap.addView(chip, lp)
        }
    }

    private fun loadResults(query: String) {
        val ctx = context ?: return

        txtTitle.text = "Hasil untuk \"$query\""
        txtDesc.text = "Mencari produk..."
        btnHistoryMode.visibility = View.VISIBLE
        historyBlock.visibility = View.GONE
        viewedBlock.visibility = View.GONE
        rvResults.visibility = View.GONE
        txtEmptyResult.visibility = View.GONE

        ApiClient.request(ctx, Request.Method.GET, "/products?per_page=30&sort=latest&q=$query", null, { response ->
            if (!isAdded) return@request

            val arr = response.getJSONObject("data").getJSONArray("data")
            val rows = mutableListOf<Product>()
            for (i in 0 until arr.length()) {
                rows.add(Product.fromJson(arr.getJSONObject(i)))
            }

            txtDesc.text = if (rows.isEmpty()) "Produk tidak ditemukan" else "${rows.size} produk ditemukan"
            resultAdapter.setItems(rows)

            rvResults.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
            txtEmptyResult.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }, {
            if (isAdded) {
                txtDesc.text = "Produk belum bisa ditampilkan"
                toast(it)
            }
        })
    }

    private fun add(product: Product) {
        val ctx = context ?: return
        if (!SessionManager(ctx).isLogin()) {
            GuestCartDbHelper(ctx).add(product)
            toast("Masuk keranjang")
            return
        }

        ApiClient.request(
            ctx,
            Request.Method.POST,
            "/cart/items",
            JSONObject().put("produk_id", product.id).put("jumlah", 1),
            {
                if (isAdded) toast("Masuk keranjang")
            },
            {
                if (isAdded) toast(it)
            }
        )
    }

    private fun openDetail(product: Product) {
        db.saveViewed(product)
        val ctx = context ?: return
        startActivity(Intent(ctx, ProductDetailActivity::class.java).putExtra("id", product.id))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
