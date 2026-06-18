package muhamad.irfan.sitahupm.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.GuestCartDbHelper
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.ui.main.MainActivity
import org.json.JSONObject

class AuthActivity : AppCompatActivity() {
    private var registerMode = false
    private lateinit var name: EditText
    private lateinit var phone: EditText
    private lateinit var email: EditText
    private lateinit var pass: EditText
    private lateinit var confirm: EditText
    private lateinit var title: TextView
    private lateinit var submit: Button
    private lateinit var switch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_auth)
        name = findViewById(R.id.edtName)
        phone = findViewById(R.id.edtPhone)
        email = findViewById(R.id.edtEmail)
        pass = findViewById(R.id.edtPassword)
        confirm = findViewById(R.id.edtPasswordConfirm)
        title = findViewById(R.id.txtAuthTitle)
        submit = findViewById(R.id.btnSubmit)
        switch = findViewById(R.id.txtSwitch)
        submit.setOnClickListener { submit() }
        switch.setOnClickListener { registerMode = !registerMode; render() }
        render()
    }

    private fun render() {
        name.visibility = if (registerMode) View.VISIBLE else View.GONE
        phone.visibility = name.visibility
        confirm.visibility = name.visibility
        title.text = if (registerMode) "Daftar Akun" else "Masuk"
        submit.text = if (registerMode) "Daftar" else "Login"
        switch.text = if (registerMode) "Sudah punya akun? Login" else "Belum punya akun? Daftar"
    }

    private fun submit() {
        val path = if (registerMode) "/auth/register" else "/auth/login"
        val body = JSONObject()
            .put("email", email.text.toString())
            .put("password", pass.text.toString())
            .put("role", "pembeli")
        if (registerMode) {
            body.put("name", name.text.toString())
            body.put("telepon", phone.text.toString())
            body.put("password_confirmation", confirm.text.toString())
        }
        ApiClient.request(this, Request.Method.POST, path, body, { response ->
            val user = response.optJSONObject("user")
            SessionManager(this).save(
                response.optString("token"),
                user?.optString("name") ?: "Pembeli",
                user?.optString("email") ?: email.text.toString(),
                user?.optString("telepon") ?: phone.text.toString()
            )
            syncGuestCartThenOpen()
        }, { Toast.makeText(this, it, Toast.LENGTH_LONG).show() })
    }

    private fun syncGuestCartThenOpen() {
        val db = GuestCartDbHelper(this)
        val items = db.items()
        if (items.isEmpty()) {
            openMain()
            return
        }

        var pending = items.size
        fun done() {
            pending--
            if (pending <= 0) {
                db.clear()
                openMain()
            }
        }

        for (item in items) {
            ApiClient.request(
                this,
                Request.Method.POST,
                "/cart/items",
                JSONObject().put("produk_id", item.produkId).put("jumlah", item.jumlah),
                { done() },
                { done() }
            )
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
