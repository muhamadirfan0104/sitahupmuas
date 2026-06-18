package muhamad.irfan.sitahupm.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.util.FormatUtil
import muhamad.irfan.sitahupm.util.ImageUtil

data class CartUiItem(
    val id: Int,
    val productId: Int,
    val name: String,
    val price: Double,
    val qty: Int,
    val subtotal: Double,
    val stock: Int,
    val unit: String,
    val imageUrl: String?,
    var selected: Boolean = true
)

class CartPrototypeAdapter(
    private val items: MutableList<CartUiItem>,
    private val onChecked: (CartUiItem, Boolean) -> Unit,
    private val onPlus: (CartUiItem) -> Unit,
    private val onMinus: (CartUiItem) -> Unit,
    private val onRemove: (CartUiItem) -> Unit
) : RecyclerView.Adapter<CartPrototypeAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.chkItem)
        val image: ImageView = view.findViewById(R.id.imgProduct)
        val name: TextView = view.findViewById(R.id.txtName)
        val unit: TextView = view.findViewById(R.id.txtUnit)
        val stock: TextView = view.findViewById(R.id.txtStock)
        val priceQty: TextView = view.findViewById(R.id.txtPriceQty)
        val subtotal: TextView = view.findViewById(R.id.txtSubtotal)
        val minus: TextView = view.findViewById(R.id.btnMinus)
        val qty: TextView = view.findViewById(R.id.txtQty)
        val plus: TextView = view.findViewById(R.id.btnPlus)
        val remove: TextView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart_prototype, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = item.selected
        holder.check.setOnCheckedChangeListener { _, checked ->
            onChecked(item, checked)
        }

        ImageUtil.load(holder.image, item.imageUrl)
        holder.name.text = item.name
        holder.unit.text = item.unit.ifBlank { "pack" }
        holder.stock.text = "Stok ${item.stock}"
        holder.priceQty.text = "${FormatUtil.rupiah(item.price)} x ${item.qty}"
        holder.subtotal.text = FormatUtil.rupiah(item.subtotal)
        holder.qty.text = item.qty.toString()

        holder.plus.setOnClickListener { onPlus(item) }
        holder.minus.setOnClickListener { onMinus(item) }
        holder.remove.setOnClickListener { onRemove(item) }
    }

    fun setItems(newItems: List<CartUiItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun currentItems(): List<CartUiItem> = items
}
