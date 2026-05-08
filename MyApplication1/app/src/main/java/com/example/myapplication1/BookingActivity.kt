package com.example.myapplication1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.databinding.ActivityBookingBinding
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

data class TrottinetteBooking(
    val id: Int = 0,
    val trottinetteId: Int,
    val userId: Int,
    val startTime: Calendar,
    val endTime: Calendar? = null,
    val totalCost: Double = 0.0
)

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding

    private var startCalendar = Calendar.getInstance()
    private var endCalendar   = Calendar.getInstance().apply { add(Calendar.HOUR, 1) }
    private val pricePerHour  = 2.5

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    private var trottinetteId   = 0
    private var trottinetteName = ""
    private var batterie        = 100
    private var stationLat      = 0.0
    private var stationLng      = 0.0
    private var distStr         = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        trottinetteId   = intent.getIntExtra("trottinette_id", 0)
        trottinetteName = intent.getStringExtra("trottinette_name") ?: "Station #$trottinetteId"
        batterie        = intent.getIntExtra("batterie", 0)
        stationLat      = intent.getDoubleExtra("station_lat", 0.0)
        stationLng      = intent.getDoubleExtra("station_lng", 0.0)
        distStr         = intent.getStringExtra("dist_str") ?: ""

        binding.tvTrottinetteName.text = trottinetteName
        binding.tvBatterie.text        = "Trottinette disponibles : $batterie"
        binding.tvBatterieBar.progress = ((batterie.toFloat() / 12f) * 100).toInt().coerceIn(0, 100)

        if (distStr.isNotEmpty()) {
            binding.tvDistance.text       = "Distance : $distStr"
            binding.tvDistance.visibility = android.view.View.VISIBLE
        }

        updateDateDisplays()
        updateCost()

        binding.btnStartTime.setOnClickListener {
            pickDateTime(startCalendar) {
                if (startCalendar.after(endCalendar)) {
                    endCalendar = startCalendar.clone() as Calendar
                    endCalendar.add(Calendar.HOUR, 1)
                }
                updateDateDisplays()
                updateCost()
            }
        }

        binding.btnEndTime.setOnClickListener {
            pickDateTime(endCalendar) {
                if (endCalendar.before(startCalendar)) {
                    Toast.makeText(this,
                        "La fin doit être après le début",
                        Toast.LENGTH_SHORT).show()
                    endCalendar = startCalendar.clone() as Calendar
                    endCalendar.add(Calendar.HOUR, 1)
                }
                updateDateDisplays()
                updateCost()
            }
        }

        // ✅ Confirmer → PaymentActivity
        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun pickDateTime(calendar: Calendar, onSet: () -> Unit) {
        DatePickerDialog(this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR,         year)
                calendar.set(Calendar.MONTH,        month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(this,
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE,      minute)
                        onSet()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplays() {
        binding.tvStartTime.text = sdf.format(startCalendar.time)
        binding.tvEndTime.text   = sdf.format(endCalendar.time)
    }

    private fun updateCost() {
        val diffMs    = endCalendar.timeInMillis - startCalendar.timeInMillis
        val diffHours = diffMs / (1000.0 * 60 * 60)
        val cost      = if (diffHours > 0) diffHours * pricePerHour else 0.0
        val hours     = diffMs / (1000 * 60 * 60)
        val minutes   = (diffMs / (1000 * 60)) % 60

        binding.tvDuration.text  = "${hours}h ${minutes}min"
        binding.tvTotalCost.text = "${"%.2f".format(cost)} €"

        binding.btnConfirmBooking.isEnabled = diffHours > 0
        binding.btnConfirmBooking.alpha     = if (diffHours > 0) 1.0f else 0.5f
    }
/*
    private fun confirmBooking() {
        val diffMs    = endCalendar.timeInMillis - startCalendar.timeInMillis
        val diffHours = diffMs / (1000.0 * 60 * 60)

        if (diffHours <= 0) {
            Toast.makeText(this,
                "Veuillez sélectionner une durée valide",
                Toast.LENGTH_SHORT).show()
            return
        }

        val totalCost = diffHours * pricePerHour

        // ✅ Redirige vers PaymentActivity
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("total_cost",   totalCost)
        intent.putExtra("station_name", trottinetteName)
        intent.putExtra("booking_id",   trottinetteId)
        intent.putExtra("start_time",   sdf.format(startCalendar.time))
        intent.putExtra("end_time",     sdf.format(endCalendar.time))
        intent.putExtra("duration",
            "${diffMs/(1000*60*60)}h ${(diffMs/(1000*60))%60}min")
        intent.putExtra("dist_str",     distStr)
        startActivity(intent)
    }*/
private fun confirmBooking() {
    val diffMs    = endCalendar.timeInMillis - startCalendar.timeInMillis
    val diffHours = diffMs / (1000.0 * 60 * 60)

    if (diffHours <= 0) {
        Toast.makeText(this, "Veuillez sélectionner une durée valide", Toast.LENGTH_SHORT).show()
        return
    }

    val totalCost = diffHours * pricePerHour

    binding.btnConfirmBooking.isClickable = false
    binding.btnConfirmBooking.alpha       = 0.6f
    binding.tvConfirmText.text            = "Envoi en cours..."

    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE)

    val request = BookingRequest(
        Trottinette = trottinetteId,
        client        = getUserId(),
        start_time  = isoFormat.format(startCalendar.time),
        end_time    = isoFormat.format(endCalendar.time),
        total_cost  = totalCost
    )

    lifecycleScope.launch {
        try {
            val response = RetrofitClient.apiService.addBooking(request)
            if (response.isSuccessful) {

                val intent = Intent(this@BookingActivity, PaymentActivity::class.java)
                intent.putExtra("total_cost",   totalCost)
                intent.putExtra("station_name", trottinetteName)
                intent.putExtra("booking_id",   response.body()?.id ?: trottinetteId)
                intent.putExtra("start_time",   sdf.format(startCalendar.time))
                intent.putExtra("end_time",     sdf.format(endCalendar.time))
                intent.putExtra("duration",
                    "${diffMs/(1000*60*60)}h ${(diffMs/(1000*60))%60}min")
                intent.putExtra("dist_str", distStr)
                startActivity(intent)
                TokenManager.saveTrottinetteId(this as Context, trottinetteId)

            } else {
                Toast.makeText(
                    this@BookingActivity,
                    "Erreur : ${response.code()} — ${response.errorBody()?.string()}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnConfirmBooking.isClickable = true
                binding.btnConfirmBooking.alpha       = 1.0f
                binding.tvConfirmText.text            = "Confirmer la réservation"
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@BookingActivity,
                "Connexion impossible : ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            binding.btnConfirmBooking.isClickable = true
            binding.btnConfirmBooking.alpha       = 1.0f
            binding.tvConfirmText.text            = "Confirmer la réservation"
        }
    }
}

    private fun getUserId(): Int {
        return TokenManager.getUserId(this)

    }
}
