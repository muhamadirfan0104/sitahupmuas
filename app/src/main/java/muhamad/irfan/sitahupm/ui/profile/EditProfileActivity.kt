package muhamad.irfan.sitahupm.ui.profile

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.SessionManager
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_simple)

        session = SessionManager(this)

        findViewById<EditText>(R.id.edtName).setText(session.name())
        findViewById<EditText>(R.id.edtEmail).setText(session.email())
        findViewById<EditText>(R.id.edtPhone).setText(session.telepon())

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            saveProfile()
        }

        findViewById<TextView>(R.id.btnChangePassword).setOnClickListener {
            changePassword()
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun saveProfile() {
        val name = findViewById<EditText>(R.id.edtName).text.toString().trim()
        val email = findViewById<EditText>(R.id.edtEmail).text.toString().trim()
        val phone = findViewById<EditText>(R.id.edtPhone).text.toString().trim()

        if (name.isBlank() || email.isBlank()) {
            toast("Nama dan email wajib diisi")
            return
        }

        val body = JSONObject()
            .put("name", name)
            .put("email", email)
            .put("telepon", phone)

        val button = findViewById<TextView>(R.id.btnSave)
        button.isEnabled = false
        button.text = "Menyimpan..."

        ApiClient.request(this, Request.Method.PUT, "/profile", body, { response ->
            val user = response.optJSONObject("user")
            val newName = user?.optString("name") ?: name
            val newEmail = user?.optString("email") ?: email
            val newPhone = user?.optString("telepon") ?: phone

            session.saveUser(newName, newEmail, newPhone)
            button.isEnabled = true
            button.text = "Simpan Profil"
            toast("Profil berhasil diperbarui")
        }, {
            button.isEnabled = true
            button.text = "Simpan Profil"
            toast(it)
        })
    }

    private fun changePassword() {
        val oldPassword = findViewById<EditText>(R.id.edtOldPassword).text.toString()
        val newPassword = findViewById<EditText>(R.id.edtNewPassword).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.edtConfirmPassword).text.toString()

        if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            toast("Semua kolom password wajib diisi")
            return
        }

        if (newPassword.length < 6) {
            toast("Password baru minimal 6 karakter")
            return
        }

        if (newPassword != confirmPassword) {
            toast("Konfirmasi password tidak sama")
            return
        }

        val body = JSONObject()
            .put("password_lama", oldPassword)
            .put("password", newPassword)
            .put("password_confirmation", confirmPassword)

        val button = findViewById<TextView>(R.id.btnChangePassword)
        button.isEnabled = false
        button.text = "Mengganti..."

        ApiClient.request(this, Request.Method.PUT, "/password", body, {
            clearPasswordFields()
            button.isEnabled = true
            button.text = "Ganti Password"
            toast("Password berhasil diganti")
        }, {
            button.isEnabled = true
            button.text = "Ganti Password"
            toast(it)
        })
    }

    private fun clearPasswordFields() {
        findViewById<EditText>(R.id.edtOldPassword).setText("")
        findViewById<EditText>(R.id.edtNewPassword).setText("")
        findViewById<EditText>(R.id.edtConfirmPassword).setText("")
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
