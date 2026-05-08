package com.example.myapplication1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.databinding.ActivityNearestBinding

class NearestStationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearestBinding

    private var userLat = 45.7800
    private var userLng = 3.0870
    private var radius  = 1000f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityNearestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userLat = intent.getDoubleExtra("user_lat", 45.7800)
        userLng = intent.getDoubleExtra("user_lng", 3.0870)

        // ✅ Back button
        binding.btnBack.setOnClickListener { finish() }

        updateStats(getSortedStations())

        binding.radiusSlider.addOnChangeListener { _, value, _ ->
            radius = value
            binding.tvRadius.text = "${value.toInt()} m"
            updateStats(getSortedStations())
        }
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val R    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng/2) * Math.sin(dLng/2)
        return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toInt()
    }

    private fun getSortedStations(): List<Pair<Station, Int>> {
        return StationRepository.getStations()
            .map { Pair(it, haversine(userLat, userLng, it.lat, it.lng)) }
            .filter { it.second <= radius }
            .sortedBy { it.second }
    }

    private fun updateStats(list: List<Pair<Station, Int>>) {
        if (list.isEmpty()) {
            binding.tvNearestDist.text  = "—"
            binding.tvNearestBikes.text = "—"
            return
        }
        val nearest = list.first()
        val dist    = nearest.second
        binding.tvNearestDist.text  = if (dist < 1000) "${dist}m"
        else "${"%.1f".format(dist / 1000.0)}km"
        binding.tvNearestBikes.text = "${nearest.first.bikes}"
    }
}