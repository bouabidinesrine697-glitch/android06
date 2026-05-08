package com.example.myapplication1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication1.TrottinetteListBottomSheet
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val PERMISSION_REQUEST_CODE = 1

    private var stations: List<StationDisp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.5)
        mapView.controller.setCenter(GeoPoint(36.8065, 10.1815))

        val focusLat = intent.getDoubleExtra("focus_lat", 0.0)
        val focusLng = intent.getDoubleExtra("focus_lng", 0.0)
        if (focusLat != 0.0 && focusLng != 0.0) {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(GeoPoint(focusLat, focusLng))
        }

        setupLocationButton()
        checkPermissions()
        fetchStations()
    }

    // ── API ───────────────────────────────────────────────────────────────────

    private fun fetchStations() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getStations()
                if (response.isSuccessful) {
                    stations = response.body() ?: emptyList()
                    val (uLat, uLng) = getUserLocation()
                    refreshMarkersFromUserLocation(uLat, uLng)
                } else {
                    showToast("Erreur serveur : ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Connexion impossible : ${e.message}")
            }
        }
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    private fun getUserLocation(): Pair<Double, Double> =
        if (::locationOverlay.isInitialized && locationOverlay.myLocation != null)
            Pair(locationOverlay.myLocation.latitude, locationOverlay.myLocation.longitude)
        else
            Pair(36.8065, 10.1815)

    // ── Markers ───────────────────────────────────────────────────────────────

    private fun refreshMarkersFromUserLocation(userLat: Double, userLng: Double) {

        // ✅ CopyOnWriteArrayList safe removal
        val toRemove = mapView.overlays.filterIsInstance<Marker>()
        mapView.overlays.removeAll(toRemove)

        if (::locationOverlay.isInitialized &&
            !mapView.overlays.contains(locationOverlay)
        ) {
            mapView.overlays.add(locationOverlay)
        }

        if (stations.isEmpty()) {
            mapView.invalidate()
            return
        }

        val sorted = stations.sortedBy { s ->
            haversine(userLat, userLng, s.latitude, s.longitude)
        }

        for ((index, station) in sorted.withIndex()) {
            val dist    = haversine(userLat, userLng, station.latitude, station.longitude)
            val distStr = if (dist < 1000) "${dist}m"
            else "${"%.1f".format(dist / 1000.0)}km"

            val scootersAvail = station.trottinettes.count { it.status == "disponible" }

            val marker = Marker(mapView)
            marker.position = GeoPoint(station.latitude, station.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = createPinDrawable(
                bikes     = station.nombre_disponibles,
                scooters  = scootersAvail,
                isNearest = index == 0
            )
            marker.title   = station.nom
            marker.snippet = buildSnippet(station, distStr)

            marker.setOnMarkerClickListener { m, _ ->
                showStationPopup(station, userLat, userLng)
                m.showInfoWindow()
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun buildSnippet(station: StationDisp, distStr: String): String {
        val scootersAvail = station.trottinettes.count { it.status == "disponible" }
        return buildString {
            append("🚲 ${station.nombre_disponibles} trottinette")
            if (scootersAvail > 0) append(" • 🛴 $scootersAvail trottinettes")
            append(" • $distStr")
        }
    }

    // ── Pin drawable ──────────────────────────────────────────────────────────

    private fun createPinDrawable(
        bikes: Int,
        scooters: Int = 0,
        isNearest: Boolean = false
    ): android.graphics.drawable.BitmapDrawable {

        val size   = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx     = size / 2f
        val r      = size * 0.35f

        val color = when {
            isNearest                   -> Color.parseColor("#FF6F00")
            bikes == 0 && scooters == 0 -> Color.parseColor("#BBBBBB")
            else                        -> Color.parseColor("#E8001C")
        }

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style      = Paint.Style.FILL
        }.also {
            val path = Path()
            path.addCircle(cx, r + 4, r, Path.Direction.CW)
            path.moveTo(cx - r * 0.5f, r + 4 + r * 0.7f)
            path.lineTo(cx, size.toFloat() - 4)
            path.lineTo(cx + r * 0.5f, r + 4 + r * 0.7f)
            path.close()
            canvas.drawPath(path, it)
        }

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color  = if (isNearest) Color.parseColor("#FFD700") else Color.WHITE
            style       = Paint.Style.STROKE
            strokeWidth = if (isNearest) 6f else 4f
        }.also { canvas.drawCircle(cx, r + 4, r, it) }

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize   = 26f
            typeface   = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign  = Paint.Align.CENTER
        }.also {
            val label = if (scooters > 0) "$bikes+$scooters" else bikes.toString()
            canvas.drawText(label, cx, r + 4 + 10f, it)
        }

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────

    private fun showStationPopup(
        station: StationDisp,
        userLat: Double,
        userLng: Double
    ) {
        StationDetailBottomSheet.newInstance(station, userLat, userLng)
            .show(supportFragmentManager, "station_detail")
    }

    // ── Buttons & nav ─────────────────────────────────────────────────────────

    private fun setupLocationButton() {
        findViewById<ImageButton>(R.id.btn_locate).setOnClickListener {
            if (::locationOverlay.isInitialized) {
                locationOverlay.enableFollowLocation()
                locationOverlay.myLocation?.let { loc ->
                    mapView.controller.animateTo(loc)
                } ?: showToast("Localisation en cours...")
            } else {
                checkPermissions()
            }
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_center
        )?.setOnClickListener {
            val (uLat, uLng) = getUserLocation()
            startActivity(
                Intent(this, NearestStationActivity::class.java).apply {
                    putExtra("user_lat", uLat)
                    putExtra("user_lng", uLng)
                }
            )
        }

        findViewById<ImageButton>(R.id.btn_close_popup)?.setOnClickListener {
            findViewById<CardView>(R.id.station_popup)?.visibility = View.GONE
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navIds = listOf(
            R.id.nav_carte,
            R.id.nav_trajet,
            R.id.nav_info,
            R.id.nav_profil
        )

        navIds.forEach { id ->
            findViewById<LinearLayout>(id)?.setOnClickListener {
                navIds.forEach { navId ->
                    val item = findViewById<LinearLayout>(navId)
                    (item?.getChildAt(0) as? ImageButton)
                        ?.setColorFilter(Color.parseColor("#AAAAAA"))
                    (item?.getChildAt(1) as? TextView)
                        ?.setTextColor(Color.parseColor("#AAAAAA"))
                }
                val sel = findViewById<LinearLayout>(id)
                (sel?.getChildAt(0) as? ImageButton)
                    ?.setColorFilter(Color.parseColor("#E8001C"))
                (sel?.getChildAt(1) as? TextView)
                    ?.setTextColor(Color.parseColor("#E8001C"))

                when (id) {
                    R.id.nav_trajet -> {
                        val (uLat, uLng) = getUserLocation()
                        startActivity(
                            Intent(this, NearestStationActivity::class.java).apply {
                                putExtra("user_lat", uLat)
                                putExtra("user_lng", uLng)
                            }
                        )
                    }
                    R.id.nav_profil ->
                        startActivity(Intent(this, ProfileActivity::class.java))

                    R.id.nav_info -> {
                        val clientId = TokenManager.getUserId(this)
                        if (clientId == 0) {
                            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
                        } else {
                            startActivity(
                                Intent(this, ReservationsActivity::class.java)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val denied = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) enableLocation()
        else ActivityCompat.requestPermissions(
            this, denied.toTypedArray(), PERMISSION_REQUEST_CODE
        )
    }

    private fun enableLocation() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                val loc = locationOverlay.myLocation ?: return@runOnUiThread
                mapView.controller.setZoom(15.0)
                mapView.controller.animateTo(loc)
                refreshMarkersFromUserLocation(loc.latitude, loc.longitude)
                fetchStations()
            }
        }
    }

    // ── Haversine ─────────────────────────────────────────────────────────────

    private fun haversine(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Int {
        val R    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = Math.sin(dLat / 2).pow(2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2).pow(2)
        return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toInt()
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume()  { super.onResume();  mapView.onResume()  }
    override fun onPause()   { super.onPause();   mapView.onPause()   }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach()  }

    fun openTrottinetteList(stationId: Int, userLat: Double, userLng: Double, distStr: String) {
        val station = stations.find { it.id == stationId } ?: return
        TrottinetteListBottomSheet.newInstance(station, userLat, userLng, distStr)
            .show(supportFragmentManager, "trottinette_list")
    }
}