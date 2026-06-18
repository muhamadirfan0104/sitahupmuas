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

class ProductCardAdapter(
    private val items: MutableList<Product>,
    private val onDetail: (Product) -> Unit,
    private val onAdd: (Product) -> Unit
) : RecyclerView.Adapter<ProductCardAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgProduct)
        val badge: TextView = view.findViewById(R.id.txtBadge)
        val name: TextView = view.findViewById(R.id.txtName)
        val stock: TextView = view.findViewById(R.id.txtStock)
        val price: TextView = view.findViewById(R.id.txtPrice)
        val detail: TextView = view.findViewById(R.id.btnDetail)
        val add: TextView = view.findViewById(R.id.btnAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = items[position]
        ImageUtil.load(holder.image, product.gambar)
        holder.badge.visibility = View.VISIBLE
        holder.name.text = product.nama.ifBlank { "Produk" }
        holder.stock.text = "Stok ${product.stok} ${product.satuan.ifBlank { "pack" }}"
        holder.price.text = FormatUtil.rupiah(product.harga)
        holder.detail.text = "Detail"
        holder.add.text = "+"
        holder.detail.setOnClickListener { onDetail(product) }
        holder.add.setOnClickListener { onAdd(product) }
        holder.itemView.setOnClickListener { onDetail(product) }
    }

    fun setItems(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
