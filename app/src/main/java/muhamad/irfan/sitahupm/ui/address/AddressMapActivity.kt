package muhamad.irfan.sitahupm.ui.address

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import muhamad.irfan.sitahupm.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AddressMapActivity : AppCompatActivity() {
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )

        setContentView(R.layout.activity_address_map)

        val lat = intent.getDoubleExtra("lat", -7.8166)
        val lng = intent.getDoubleExtra("lng", 112.0116)
        val title = intent.getStringExtra("title") ?: "Lokasi Alamat"

        findViewById<TextView>(R.id.txtTitle).text = title
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        map = findViewById(R.id.mapAddress)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(18.0)

        val point = GeoPoint(lat, lng)
        map.controller.setCenter(point)

        val marker = Marker(map).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)
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
