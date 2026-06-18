package muhamad.irfan.sitahupm.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.model.Order
import muhamad.irfan.sitahupm.util.FormatUtil

class OrderPrototypeAdapter(
    private val items: MutableList<Order>,
    private val onDetail: (Order) -> Unit,
    private val onQr: (Order) -> Unit
) : RecyclerView.Adapter<OrderPrototypeAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val invoice: TextView = view.findViewById(R.id.txtInvoice)
        val orderStatus: TextView = view.findViewById(R.id.txtOrderStatus)
        val paymentStatus: TextView = view.findViewById(R.id.txtPaymentStatus)
        val meta: TextView = view.findViewById(R.id.txtMeta)
        val total: TextView = view.findViewById(R.id.txtTotal)
        val detail: TextView = view.findViewById(R.id.btnDetail)
        val qr: TextView = view.findViewById(R.id.btnQr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_order_prototype, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]
        holder.invoice.text = order.invoice.ifBlank { "INV-${order.id}" }
        holder.orderStatus.text = "Pesanan: ${order.status.ifBlank { "-" }}"
        holder.paymentStatus.text = "Bayar: ${order.statusBayar.ifBlank { "-" }}"
        holder.meta.text = "${order.metodeAmbil.ifBlank { "-" }} • ${order.metodeBayar.ifBlank { "-" }}"
        holder.total.text = FormatUtil.rupiah(order.total)
        holder.detail.setOnClickListener { onDetail(order) }
        holder.qr.setOnClickListener { onQr(order) }
    }

    fun setItems(newItems: List<Order>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
