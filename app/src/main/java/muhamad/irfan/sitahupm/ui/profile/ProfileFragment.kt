package muhamad.irfan.sitahupm.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiClient
import muhamad.irfan.sitahupm.data.local.SessionManager
import muhamad.irfan.sitahupm.ui.address.AddressActivity
import muhamad.irfan.sitahupm.ui.auth.AuthActivity

class ProfileFragment : Fragment() {
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile_simple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        session = SessionManager(requireContext())

        if (session.isLogin()) {
            showUserProfile(view)
        } else {
            showGuestProfile(view)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            if (session.isLogin()) showUserProfile(it)
        }
    }

    private fun showUserProfile(view: View) {
        showSessionData(view)
        loadProfileFromApi(view)

        view.findViewById<View>(R.id.profileBox).visibility = View.VISIBLE
        view.findViewById<View>(R.id.btnEditProfile).visibility = View.VISIBLE
        view.findViewById<View>(R.id.menuAlamat).visibility = View.VISIBLE
        view.findViewById<View>(R.id.menuReviews).visibility = View.VISIBLE
        view.findViewById<View>(R.id.menuVideoProfil).visibility = View.VISIBLE
        view.findViewById<View>(R.id.btnLogin).visibility = View.GONE
        view.findViewById<View>(R.id.txtGuestInfo).visibility = View.GONE

        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        view.findViewById<View>(R.id.menuAlamat).setOnClickListener {
            startActivity(Intent(requireContext(), AddressActivity::class.java))
        }

        view.findViewById<View>(R.id.menuReviews).setOnClickListener {
            startActivity(Intent(requireContext(), MyReviewsActivity::class.java))
        }

        view.findViewById<View>(R.id.menuVideoProfil).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileVideoActivity::class.java))
        }
    }

    private fun showSessionData(view: View) {
        view.findViewById<TextView>(R.id.txtName).text = session.name()
        view.findViewById<TextView>(R.id.txtEmail).text = session.email()
        view.findViewById<TextView>(R.id.txtPhone).text =
            session.telepon().ifBlank { "Nomor HP belum diisi" }
    }

    private fun loadProfileFromApi(view: View) {
        ApiClient.request(requireContext(), Request.Method.GET, "/me", null, { response ->
            if (!isAdded) return@request
            val user = response.optJSONObject("user") ?: return@request
            session.saveUser(
                user.optString("name", session.name()),
                user.optString("email", session.email()),
                user.optString("telepon", session.telepon())
            )
            showSessionData(view)
        }, {
            // Kalau koneksi gagal, profil tetap menampilkan data session terakhir.
        })
    }

    private fun showGuestProfile(view: View) {
        view.findViewById<View>(R.id.profileBox).visibility = View.GONE
        view.findViewById<View>(R.id.btnEditProfile).visibility = View.GONE
        view.findViewById<View>(R.id.menuAlamat).visibility = View.GONE
        view.findViewById<View>(R.id.menuReviews).visibility = View.GONE
        view.findViewById<View>(R.id.menuVideoProfil).visibility = View.VISIBLE
        view.findViewById<View>(R.id.btnLogin).visibility = View.VISIBLE
        view.findViewById<View>(R.id.txtGuestInfo).visibility = View.VISIBLE

        view.findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
        }

        view.findViewById<View>(R.id.menuVideoProfil).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileVideoActivity::class.java))
        }
    }
}
