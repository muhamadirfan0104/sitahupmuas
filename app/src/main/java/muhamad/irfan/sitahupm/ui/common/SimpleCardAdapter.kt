package muhamad.irfan.sitahupm.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.util.ImageUtil

data class SimpleCard(
    val top: String,
    val mid: String,
    val bottom: String,
    val primary: String = "Detail",
    val second: String = "Tambah",
    val dataId: Int = 0,
    val imageUrl: String? = null
)

class SimpleCardAdapter(
    private val items: MutableList<SimpleCard>,
    private val onPrimary: (SimpleCard) -> Unit,
    private val onSecond: (SimpleCard) -> Unit
) : RecyclerView.Adapter<SimpleCardAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgThumb)
        val top: TextView = view.findViewById(R.id.txtTop)
        val mid: TextView = view.findViewById(R.id.txtMid)
        val bottom: TextView = view.findViewById(R.id.txtBottom)
        val primary: Button = view.findViewById(R.id.btnPrimary)
        val second: Button = view.findViewById(R.id.btnSecond)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_simple_card, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        ImageUtil.load(holder.image, item.imageUrl)
        holder.top.text = item.top
        holder.mid.text = item.mid
        holder.bottom.text = item.bottom
        holder.primary.text = item.primary
        holder.second.text = item.second
        holder.primary.setOnClickListener { onPrimary(item) }
        holder.second.setOnClickListener { onSecond(item) }
    }

    fun setItems(newItems: List<SimpleCard>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
