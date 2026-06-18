package muhamad.irfan.sitahupm.ui.address

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import muhamad.irfan.sitahupm.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AddressPickerActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var txtSelectedPoint: TextView
    private var selectedLat: Double = -7.8166
    private var selectedLng: Double = 112.0116
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )

        setContentView(R.layout.activity_address_picker)

        selectedLat = intent.getDoubleExtra("lat", -7.8166)
        selectedLng = intent.getDoubleExtra("lng", 112.0116)

        txtSelectedPoint = findViewById(R.id.txtSelectedPoint)
        map = findViewById(R.id.mapPicker)

        setupMap()
        setSelectedPoint(selectedLat, selectedLng)

        findViewById<TextView>(R.id.btnUsePoint).setOnClickListener {
            useSelectedPoint()
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(18.0)

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    setSelectedPoint(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    setSelectedPoint(p.latitude, p.longitude)
                }
                return true
            }
        }

        map.overlays.add(MapEventsOverlay(receiver))
    }

    private fun setSelectedPoint(lat: Double, lng: Double) {
        selectedLat = lat
        selectedLng = lng

        val point = GeoPoint(lat, lng)
        map.controller.animateTo(point)

        marker?.let { map.overlays.remove(it) }

        marker = Marker(map).apply {
            position = point
            title = "Titik alamat"
            snippet = "Titik alamat dipilih"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        map.overlays.add(marker)
        map.invalidate()

        txtSelectedPoint.text = "Titik alamat dipilih"
    }

    private fun useSelectedPoint() {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra("lat", selectedLat)
                .putExtra("lng", selectedLng)
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
