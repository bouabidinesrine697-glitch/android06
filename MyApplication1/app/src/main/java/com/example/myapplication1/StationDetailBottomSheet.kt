package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StationDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(
            station: StationDisp,
            userLat: Double,
            userLng: Double
        ): StationDetailBottomSheet {
            val fragment = StationDetailBottomSheet()
            val args = Bundle()
            args.putInt("id",          station.id)
            args.putString("name",     station.nom)
            args.putInt("bikes",       station.nombre_disponibles)
            args.putInt("slots",       station.nombre_total)
            args.putDouble("lat",      station.latitude)
            args.putDouble("lng",      station.longitude)
            args.putDouble("user_lat", userLat)
            args.putDouble("user_lng", userLng)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_station, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id      = arguments?.getInt("id")          ?: 0
        val name    = arguments?.getString("name")     ?: ""
        val bikes   = arguments?.getInt("bikes")       ?: 0
        val slots   = arguments?.getInt("slots")       ?: 0
        val lat     = arguments?.getDouble("lat")      ?: 0.0
        val lng     = arguments?.getDouble("lng")      ?: 0.0
        val userLat = arguments?.getDouble("user_lat") ?: 36.8065
        val userLng = arguments?.getDouble("user_lng") ?: 10.1815
        val total   = slots.takeIf { it > 0 } ?: bikes

        val dist    = haversine(userLat, userLng, lat, lng)
        val distStr = if (dist < 1000) "${dist}m"
        else "${"%.1f".format(dist / 1000.0)}km"
        val walkMin = (dist / 80.0).toInt().coerceAtLeast(1)

        view.findViewById<TextView>(R.id.tv_station_name).text = name
        view.findViewById<TextView>(R.id.tv_bikes_count).text  = "$bikes"
        view.findViewById<TextView>(R.id.tv_slots_count).text  = "$slots"
        view.findViewById<TextView>(R.id.tv_total).text        = "Total : $total emplacements"
        view.findViewById<TextView>(R.id.tv_distance).text     = "$distStr — $walkMin min à pied"

        val tvStatus = view.findViewById<TextView>(R.id.tv_status)
        when {
            bikes == 0 -> {
                tvStatus.text = "Aucun vélo disponible"
                tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFEEEE"))
                tvStatus.setTextColor(android.graphics.Color.parseColor("#CC0000"))
            }
            bikes <= 2 -> {
                tvStatus.text = "Presque vide"
                tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFF8E1"))
                tvStatus.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            }
            else -> {
                tvStatus.text = "Disponible"
                tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
        }

        val progress = view.findViewById<ProgressBar>(R.id.progress_availability)
        progress.max      = total
        progress.progress = bikes

        // ── Navigate button ───────────────────────────────────────────────
        view.findViewById<CardView>(R.id.btn_navigate).setOnClickListener {
            val uri    = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(
                            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lng"
                        )
                    )
                )
            }
            dismiss()
        }

        // ── Show on map button ────────────────────────────────────────────
        view.findViewById<CardView>(R.id.btn_show_on_map).setOnClickListener {
            val intent = Intent(requireContext(), MapActivity::class.java)
            intent.putExtra("focus_lat", lat)
            intent.putExtra("focus_lng", lng)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            dismiss()
        }

        // ── Book button → open trottinette list ───────────────────────────
        view.findViewById<CardView>(R.id.btn_book).setOnClickListener {
            dismiss()
            (activity as? MapActivity)?.openTrottinetteList(id, userLat, userLng, distStr)
        }
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val R    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toInt()
    }
}