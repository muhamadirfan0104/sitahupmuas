package muhamad.irfan.sitahupm.ui.order

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.api.ApiConfig

class InvoiceViewerActivity : AppCompatActivity() {
    private lateinit var invoiceContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_viewer)

        val orderId = intent.getIntExtra("order_id", 0)
        val invoice = intent.getStringExtra("invoice") ?: "Invoice"
        val invoiceUrl = intent.getStringExtra("invoice_url")
            ?: "${ApiConfig.BASE_URL}/orders/$orderId/invoice"

        findViewById<TextView>(R.id.txtTitle).text = invoice
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        invoiceContent = findViewById(R.id.txtInvoiceContent)

        loadInvoice(invoiceUrl)
    }

    private fun loadInvoice(url: String) {
        ApiClient.requestText(this, url, { html ->
            invoiceContent.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        }, {
            toast(it)
            invoiceContent.text = "Invoice gagal dimuat."
        })
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
