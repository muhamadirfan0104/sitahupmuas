package muhamad.irfan.sitahupm.data.api

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.ImageView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import muhamad.irfan.sitahupm.R
import muhamad.irfan.sitahupm.data.local.SessionManager
import org.json.JSONObject

object ApiClient {
    private var queue: RequestQueue? = null

    private fun q(context: Context): RequestQueue {
        return queue ?: Volley.newRequestQueue(context.applicationContext).also { queue = it }
    }

    fun request(
        context: Context,
        method: Int,
        path: String,
        body: JSONObject? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val req = object : JsonObjectRequest(
            method,
            ApiConfig.BASE_URL + path,
            body,
            Response.Listener(onSuccess),
            Response.ErrorListener { e ->
                val msg = e.networkResponse?.data?.let { String(it) } ?: e.message ?: "Koneksi gagal"
                onError(parseMessage(msg))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = hashMapOf("Accept" to "application/json")
                val token = SessionManager(context).token()
                if (token.isNotBlank()) headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        q(context).add(req)
    }

    fun loadImage(context: Context, url: String?, target: ImageView) {
        if (url.isNullOrBlank()) {
            target.setImageResource(R.drawable.ic_box)
            return
        }

        val req = ImageRequest(
            url,
            { bmp -> target.setImageBitmap(bmp) },
            700,
            700,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            { target.setImageResource(R.drawable.ic_box) }
        )

        q(context).add(req)
    }


    fun requestText(
        context: Context,
        url: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val req = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET,
            url,
            Response.Listener(onSuccess),
            Response.ErrorListener { e ->
                val msg = e.networkResponse?.data?.let { String(it) } ?: e.message ?: "Koneksi gagal"
                onError(parseMessage(msg))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = hashMapOf("Accept" to "text/html")
                val token = SessionManager(context).token()
                if (token.isNotBlank()) headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        q(context).add(req)
    }

    fun uploadFile(
        context: Context,
        path: String,
        fieldName: String,
        uri: Uri,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val filename = getFileName(context, uri)
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"

        val req = object : VolleyMultipartRequest(
            Request.Method.POST,
            ApiConfig.BASE_URL + path,
            Response.Listener(onSuccess),
            Response.ErrorListener { e ->
                onError(e.networkResponse?.data?.let { parseMessage(String(it)) } ?: (e.message ?: "Upload gagal"))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val token = SessionManager(context).token()
                return hashMapOf("Accept" to "application/json", "Authorization" to "Bearer $token")
            }

            override fun getByteData(): MutableMap<String, VolleyMultipartRequest.DataPart> {
                return hashMapOf(fieldName to VolleyMultipartRequest.DataPart(filename, bytes, mime))
            }
        }

        q(context).add(req)
    }

    fun uploadReview(
        context: Context,
        orderId: Int,
        productId: Int,
        rating: Int,
        comment: String,
        photoUris: List<Uri>,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val files = photoUris.mapIndexed { index, uri ->
            "foto_ulasan[]" to uriToPart(context, uri, "foto-ulasan-${index + 1}.jpg", "image/jpeg")
        }

        val req = object : VolleyMultipartRequest(
            Request.Method.POST,
            ApiConfig.BASE_URL + "/reviews",
            Response.Listener(onSuccess),
            Response.ErrorListener { e ->
                onError(e.networkResponse?.data?.let { parseMessage(String(it)) } ?: (e.message ?: "Ulasan gagal dikirim"))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val token = SessionManager(context).token()
                return hashMapOf("Accept" to "application/json", "Authorization" to "Bearer $token")
            }

            override fun getTextData(): MutableMap<String, String> {
                return hashMapOf(
                    "pesanan_id" to orderId.toString(),
                    "produk_id" to productId.toString(),
                    "rating" to rating.toString(),
                    "komentar" to comment
                )
            }

            override fun getByteDataList(): List<Pair<String, VolleyMultipartRequest.DataPart>> {
                return files
            }
        }

        q(context).add(req)
    }

    private fun uriToPart(
        context: Context,
        uri: Uri,
        defaultName: String,
        defaultMime: String
    ): VolleyMultipartRequest.DataPart {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val filename = getFileName(context, uri).ifBlank { defaultName }
        val mime = context.contentResolver.getType(uri) ?: defaultMime
        return VolleyMultipartRequest.DataPart(filename, bytes, mime)
    }

    private fun getFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return "file-upload"
    }

    private fun parseMessage(raw: String): String {
        return try {
            JSONObject(raw).optString("message", raw)
        } catch (_: Exception) {
            raw.take(180)
        }
    }
}
