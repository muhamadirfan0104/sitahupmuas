package muhamad.irfan.sitahupm.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import muhamad.irfan.sitahupm.R

class ProfileVideoActivity : AppCompatActivity() {
    private lateinit var video: VideoView
    private lateinit var kontrol: MediaController

    private val daftarVideo = listOf(R.raw.video_profilumkm, R.raw.video_profilumkm2)
    private var posisi = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_video)

        video = findViewById(R.id.videoProfil)
        kontrol = MediaController(this)

        kontrol.setAnchorView(video)
        kontrol.setPrevNextListeners({ gantiVideo(1) }, { gantiVideo(-1) })

        video.setMediaController(kontrol)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        putarVideo()
    }

    private fun gantiVideo(arah: Int) {
        posisi = (posisi + arah + daftarVideo.size) % daftarVideo.size
        putarVideo()
    }

    private fun putarVideo() {
        val uri = Uri.parse("android.resource://$packageName/${daftarVideo[posisi]}")
        video.stopPlayback()
        video.setVideoURI(uri)
        video.setOnPreparedListener {
            video.seekTo(100)
            video.start()
            kontrol.show(4000)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        video.stopPlayback()
    }
}
