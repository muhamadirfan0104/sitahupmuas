package muhamad.irfan.sitahupm.util

import java.text.NumberFormat
import java.util.Locale

object FormatUtil {
    fun rupiah(value: Double): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")
}
