package muhamad.irfan.sitahupm.data.api

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

abstract class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : Request<JSONObject>(method, url, errorListener) {

    private val boundary = "sitahuBoundary${System.currentTimeMillis()}"

    data class DataPart(
        val fileName: String,
        val content: ByteArray,
        val type: String
    )

    open fun getTextData(): MutableMap<String, String> = mutableMapOf()

    open fun getByteData(): MutableMap<String, DataPart> = mutableMapOf()

    open fun getByteDataList(): List<Pair<String, DataPart>> {
        return getByteData().map { it.key to it.value }
    }

    override fun getBodyContentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    override fun getBody(): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)

        getTextData().forEach { (key, value) ->
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
            dos.writeBytes(value)
            dos.writeBytes("\r\n")
        }

        getByteDataList().forEach { (key, part) ->
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${part.fileName}\"\r\n")
            dos.writeBytes("Content-Type: ${part.type}\r\n\r\n")
            dos.write(part.content)
            dos.writeBytes("\r\n")
        }

        dos.writeBytes("--$boundary--\r\n")
        return out.toByteArray()
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
        return try {
            Response.success(
                JSONObject(String(response.data)),
                HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: Exception) {
            Response.error(com.android.volley.ParseError(e))
        }
    }

    override fun deliverResponse(response: JSONObject) {
        listener.onResponse(response)
    }
}
