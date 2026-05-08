package com.example.myapplication1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TrottinetteListBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(
            station: StationDisp,
            userLat: Double,
            userLng: Double,
            distStr: String
        ): TrottinetteListBottomSheet {
            val fragment = TrottinetteListBottomSheet()
            val args = Bundle()
            args.putInt("station_id",       station.id)
            args.putString("station_name",  station.nom)
            args.putDouble("station_lat",   station.latitude)
            args.putDouble("station_lng",   station.longitude)
            args.putDouble("user_lat",      userLat)
            args.putDouble("user_lng",      userLng)
            args.putString("dist_str",      distStr)

            // Pack trottinettes as parallel arrays
            val ids      = station.trottinettes.map { it.id }.toIntArray()
            val qrs      = station.trottinettes.map { it.QR_code }.toTypedArray()
            val models   = station.trottinettes.map { it.model }.toTypedArray()
            val statuses = station.trottinettes.map { it.status }.toTypedArray()
            val prices = DoubleArray(station.trottinettes.size) { i -> station.trottinettes[i].price_per_minute ?: 0.0 }
            val batteries= station.trottinettes.map { it.battery }.toIntArray()

            args.putIntArray("ids",        ids)
            args.putStringArray("qrs",     qrs)
            args.putStringArray("models",  models)
            args.putStringArray("statuses",statuses)
            args.putDoubleArray("prices",  prices)
            args.putIntArray("batteries",  batteries)

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_trottinettes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stationName = arguments?.getString("station_name") ?: ""
        val stationLat  = arguments?.getDouble("station_lat")  ?: 0.0
        val stationLng  = arguments?.getDouble("station_lng")  ?: 0.0
        val distStr     = arguments?.getString("dist_str")     ?: ""

        val ids       = arguments?.getIntArray("ids")          ?: intArrayOf()
        val qrs       = arguments?.getStringArray("qrs")       ?: arrayOf()
        val models    = arguments?.getStringArray("models")    ?: arrayOf()
        val statuses  = arguments?.getStringArray("statuses")  ?: arrayOf()
        val prices    = arguments?.getDoubleArray("prices")    ?: doubleArrayOf()
        val batteries = arguments?.getIntArray("batteries")    ?: intArrayOf()

        view.findViewById<TextView>(R.id.tv_sheet_station_name).text = stationName

        val available = statuses.count { it == "disponible" }
        view.findViewById<TextView>(R.id.tv_sheet_count).text =
            "$available trottinette${if (available > 1) "s" else ""} disponible${if (available > 1) "s" else ""}"
        view.findViewById<TextView>(R.id.tv_sheet_badge).text = "$available"

        val container = view.findViewById<LinearLayout>(R.id.ll_trottinettes)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_empty)

        if (ids.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        for (i in ids.indices) {
            val itemView = inflater.inflate(R.layout.item_trottinette, container, false)

            itemView.findViewById<TextView>(R.id.tv_model).text   = models.getOrNull(i) ?: ""
            itemView.findViewById<TextView>(R.id.tv_qr).text      = "QR: ${qrs.getOrNull(i) ?: ""}"
            itemView.findViewById<TextView>(R.id.tv_price).text   =
                "${"%.1f".format(prices.getOrNull(i) ?: 0.0)} €/min"

            val battery = batteries.getOrNull(i) ?: 0
            itemView.findViewById<ProgressBar>(R.id.pb_battery).progress = battery
            itemView.findViewById<TextView>(R.id.tv_battery).text = "$battery%"

            val status   = statuses.getOrNull(i) ?: ""
            val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
            if (status == "disponible") {
                tvStatus.text = "Disponible"
                tvStatus.setBackgroundResource(R.drawable.bg_status_badge)
            } else {
                tvStatus.text = "Indisponible"
                tvStatus.setBackgroundColor(Color.parseColor("#BBBBBB"))
            }

            // Click → BookingActivity
            itemView.setOnClickListener {
                val intent = Intent(requireContext(), BookingActivity::class.java)
                intent.putExtra("trottinette_id",   ids[i])
                intent.putExtra("trottinette_name", "${models.getOrNull(i)} — $stationName")
                intent.putExtra("batterie",         battery)
                intent.putExtra("station_lat",      stationLat)
                intent.putExtra("station_lng",      stationLng)
                intent.putExtra("dist_str",         distStr)
                intent.putExtra("price_per_minute", prices.getOrNull(i) ?: 0.9)
                startActivity(intent)
                dismiss()
            }

            // Gray out unavailable
            if (status != "disponible") {
                itemView.alpha    = 0.5f
                itemView.isEnabled = false
                itemView.setOnClickListener(null)
            }

            container.addView(itemView)
        }
    }
}