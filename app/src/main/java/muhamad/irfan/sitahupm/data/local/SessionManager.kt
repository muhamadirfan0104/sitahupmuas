package muhamad.irfan.sitahupm.data.local

import android.content.Context

class SessionManager(context: Context) {
    private val pref = context.getSharedPreferences("sitahu_session", Context.MODE_PRIVATE)

    fun save(token: String, name: String, email: String, telepon: String = "") {
        pref.edit()
            .putString("token", token)
            .putString("name", name)
            .putString("email", email)
            .putString("telepon", clean(telepon))
            .apply()
    }

    fun saveUser(name: String, email: String, telepon: String) {
        pref.edit()
            .putString("name", name)
            .putString("email", email)
            .putString("telepon", clean(telepon))
            .apply()
    }

    private fun clean(value: String): String = if (value.trim() == "null") "" else value.trim()

    fun token(): String = pref.getString("token", "") ?: ""
    fun name(): String = pref.getString("name", "Pembeli") ?: "Pembeli"
    fun email(): String = pref.getString("email", "") ?: ""
    fun telepon(): String = pref.getString("telepon", "") ?: ""
    fun isLogin(): Boolean = token().isNotBlank()
    fun clear() = pref.edit().clear().apply()
}
