package muhamad.irfan.sitahupm.util

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.api.ApiConfig
import java.net.HttpURLConnection
import java.net.URL

object ImageUtil {
    fun fullUrl(path: String?): String? {
        val value = path?.trim().orEmpty()
        if (value.isEmpty() || value == "null") return null
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("/") -> ApiConfig.STORAGE_BASE + value
            value.startsWith("storage/") -> ApiConfig.STORAGE_BASE + "/" + value
            else -> ApiConfig.STORAGE_BASE + "/storage/" + value
        }
    }

    fun load(imageView: ImageView, path: String?) {
        val url = fullUrl(path)
        imageView.tag = null
        imageView.setPadding(20, 20, 20, 20)
        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        imageView.setImageResource(R.drawable.ic_box)
        if (url == null) return
        val tag = url
        imageView.tag = tag
        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                Handler(Looper.getMainLooper()).post {
                    if (imageView.tag == tag && bitmap != null) {
                        imageView.setPadding(0, 0, 0, 0)
                        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post {
                    if (imageView.tag == tag) {
                        imageView.setPadding(20, 20, 20, 20)
                        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                        imageView.setImageResource(R.drawable.ic_box)
                    }
                }
            }
        }.start()
    }
}
