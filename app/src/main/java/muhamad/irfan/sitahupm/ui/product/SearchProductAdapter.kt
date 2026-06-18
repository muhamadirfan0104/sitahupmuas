package muhamad.irfan.sitahupm.ui.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.model.Product
import muhamad.irfan.sitahupm.util.FormatUtil
import muhamad.irfan.sitahupm.util.ImageUtil

class SearchProductAdapter(
    private val items: MutableList<Product>,
    private val onDetail: (Product) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgProduct)
        val name: TextView = view.findViewById(R.id.txtName)
        val price: TextView = view.findViewById(R.id.txtPrice)
        val stock: TextView = view.findViewById(R.id.txtStock)
        val detail: TextView = view.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_search_product, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = items[position]
        ImageUtil.load(holder.image, product.gambar)
        holder.name.text = product.nama.ifBlank { "Produk" }
        holder.price.text = FormatUtil.rupiah(product.harga)
        holder.stock.text = "Stok ${product.stok} ${product.satuan.ifBlank { "pack" }}"
        holder.detail.setOnClickListener { onDetail(product) }
        holder.itemView.setOnClickListener { onDetail(product) }
    }

    fun setItems(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
