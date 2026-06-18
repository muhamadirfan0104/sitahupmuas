package muhamad.irfan.sitahupm.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.model.Order
import muhamad.irfan.sitahupm.util.FormatUtil

class OrderSimpleAdapter(
    private val items: MutableList<Order>,
    private val onDetail: (Order) -> Unit,
    private val onSecond: (Order) -> Unit
) : RecyclerView.Adapter<OrderSimpleAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val invoice: TextView = view.findViewById(R.id.txtInvoice)
        val status: TextView = view.findViewById(R.id.txtStatus)
        val bayar: TextView = view.findViewById(R.id.txtBayar)
        val metode: TextView = view.findViewById(R.id.txtMetode)
        val total: TextView = view.findViewById(R.id.txtTotal)
        val detail: TextView = view.findViewById(R.id.btnDetail)
        val second: TextView = view.findViewById(R.id.btnQr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_simple, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]

        holder.invoice.text = order.invoice.ifBlank { "INV-${order.id}" }
        holder.status.text = "Pesanan: ${order.status.ifBlank { "-" }}"
        holder.bayar.text = "Bayar: ${order.statusBayar.ifBlank { "-" }}"
        holder.metode.text = "${order.metodeAmbil.ifBlank { "-" }} • ${order.metodeBayar.ifBlank { "-" }}"
        holder.total.text = FormatUtil.rupiah(order.total)
        holder.second.text = if (order.isSelesai) "Ulasan" else "QR"

        holder.detail.setOnClickListener { onDetail(order) }
        holder.second.setOnClickListener { onSecond(order) }
    }

    fun setItems(newItems: List<Order>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
