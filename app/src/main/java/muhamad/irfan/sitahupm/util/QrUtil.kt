package muhamad.irfan.sitahupm.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

object QrUtil {
    fun generate(payload: String, size: Int = 320): Bitmap? {
        return try {
            BarcodeEncoder().encodeBitmap(payload, BarcodeFormat.QR_CODE, size, size)
        } catch (_: Exception) {
            null
        }
    }
}
