package com.example.myapplication1

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.databinding.ActivityReservationsBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ReservationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerReservations.layoutManager = LinearLayoutManager(this)

        loadReservations()
    }

    private fun loadReservations() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getReservations(
                    clientId = TokenManager.getUserId(this@ReservationsActivity)
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    binding.tvCount.text = "${body.count} réservation(s)"
                    binding.recyclerReservations.adapter = ReservationsAdapter(
                        items        = body.reservations,
                        onEndBooking = { bookingId -> endBooking(bookingId) }
                    )
                } else {
                    showError("Aucune réservation trouvée")
                }
            } catch (e: Exception) {
                showError("Erreur réseau : vérifiez votre connexion")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun endBooking(bookingId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Terminer la réservation")
            .setMessage("Voulez-vous vraiment terminer cette réservation ?")
            .setPositiveButton("Terminer") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.apiService.endBooking(
                            bookingId = bookingId,
                            request   = EndBookingRequest(
                                client_id = TokenManager.getUserId(this@ReservationsActivity)
                            )
                        )
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            AlertDialog.Builder(this@ReservationsActivity)
                                .setTitle("✅ Réservation terminée")
                                .setMessage(
                                    "🛴 Trottinette : ${body.reservation.trottinette_modele}\n\n" +
                                            "⏱ Durée        : ${body.reservation.duration}\n\n" +
                                            "💰 Coût total   : ${body.reservation.total_cost} DT"
                                )
                                .setPositiveButton("OK") { _, _ ->
                                    loadReservations()
                                }
                                .setCancelable(false)
                                .show()
                        } else {
                            Toast.makeText(
                                this@ReservationsActivity,
                                "Erreur fin réservation",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ReservationsActivity,
                            "Erreur réseau",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
    }
}

class ReservationsAdapter(
    private val items:        List<ReservationDetail>,
    private val onEndBooking: (bookingId: Int) -> Unit
) : RecyclerView.Adapter<ReservationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModele:      TextView     = view.findViewById(R.id.tvModele)
        val tvStatus:      TextView     = view.findViewById(R.id.tvStatus)
        val tvStartTime:   TextView     = view.findViewById(R.id.tvStartTime)
        val tvEndTime:     TextView     = view.findViewById(R.id.tvEndTime)
        val tvDuration:    TextView     = view.findViewById(R.id.tvDuration)
        val tvCost:        TextView     = view.findViewById(R.id.tvCost)
        val btnEndBooking: MaterialButton = view.findViewById(R.id.btnEndBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvModele.text    = item.trottinette_modele
        holder.tvStartTime.text = formatDate(item.start_time)
        holder.tvEndTime.text   = if (item.end_time != null) formatDate(item.end_time) else "—"
        holder.tvDuration.text  = item.duration_so_far
        holder.tvCost.text      = if (item.total_cost != null) "${item.total_cost} DT" else "—"
        holder.tvStatus.text    = item.status

        // ✅ Status badge color
        val color = when (item.status) {
            "confirmée"  -> Color.parseColor("#1565C0")
            "active"     -> Color.parseColor("#3B6D11")
            "terminée"   -> Color.parseColor("#888888")
            "en_attente" -> Color.parseColor("#E8A000")
            "annulée"    -> Color.parseColor("#E8001C")
            else         -> Color.parseColor("#888888")
        }
        holder.tvStatus.setBackgroundColor(color)

        // ✅ Show button for en_attente and confirmée
        if (item.status == "en_attente" || item.status == "confirmée") {
            holder.btnEndBooking.visibility = View.VISIBLE
            holder.btnEndBooking.setOnClickListener {
                onEndBooking(item.id)
            }
        } else {
            holder.btnEndBooking.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    private fun formatDate(dateStr: String): String {
        return try {
            val input  = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()
            )
            val output = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
            )
            output.format(input.parse(dateStr)!!)
        } catch (e: Exception) {
            dateStr
        }
    }
}