package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import kotlinx.coroutines.launch
import android.util.Log

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recyclerView = findViewById(R.id.recycler_trottinettes)
        progressBar  = findViewById(R.id.progress_bar)
        tvError      = findViewById(R.id.tv_error)
        tvWelcome    = findViewById(R.id.tv_welcome)
        tvTotal      = findViewById(R.id.tv_total)

        // ✅ Affiche nom utilisateur connecté
        val firstName = TokenManager.getFirstName(this) ?: "Utilisateur"
        val lastName  = TokenManager.getLastName(this)  ?: ""
        tvWelcome.text = "Bonjour, $firstName $lastName 👋"

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Bouton déconnexion
        findViewById<ImageButton>(R.id.btn_logout)?.setOnClickListener {
            TokenManager.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bouton carte
        findViewById<CardView>(R.id.btn_map)?.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        loadTrottinettes()
    }

    private fun loadTrottinettes() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility     = View.GONE

        lifecycleScope.launch {
            try {
                val api      = RetrofitClient.getAuthenticatedClient(this@HomeActivity)
                val response = api.getListtrottinettes()
                Log.d("API_TEST", "Body: $response")

                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!

                    tvTotal.text = "${list.size} trottinettes disponibles"
                    recyclerView.adapter = TrottinetteAdapter(list) { trottinette ->
                        // ✅ Clic → ouvrir BookingActivity
                        val intent = Intent(this@HomeActivity, BookingActivity::class.java)
                        intent.putExtra("trottinette_id",   trottinette.id)
                        intent.putExtra("trottinette_name", trottinette.model)
                        intent.putExtra("batterie",         trottinette.battery)
                        startActivity(intent)
                    }
                } else {
                    showError("Erreur ${response.code()} — ${response.message()}")
                }

            } catch (e: Exception) {
                showError("Erreur réseau : ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        tvError.text       = message
        tvError.visibility = View.VISIBLE
    }
}

// ✅ Adapter RecyclerView
class TrottinetteAdapter(
    private val list:    List<Trottinette>,
    private val onClick: (Trottinette) -> Unit
) : RecyclerView.Adapter<TrottinetteAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModel:    TextView  = view.findViewById(R.id.tv_model)
        val tvStatus:   TextView  = view.findViewById(R.id.tv_status)
        val tvBattery:  TextView  = view.findViewById(R.id.tv_battery)
        val tvPrice:    TextView  = view.findViewById(R.id.tv_price)
        val tvQr:       TextView  = view.findViewById(R.id.tv_qr)
        val progressBar: ProgressBar = view.findViewById(R.id.tv_batterie_bar)
        val btnBook:    CardView  = view.findViewById(R.id.btn_book)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trottinette, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = list[position]

        holder.tvModel.text   = t.model
        holder.tvQr.text      = "QR : ${t.QR_code}"
        holder.tvBattery.text = "🔋 ${t.battery}%"
        holder.tvPrice.text   = "${"%.2f".format(t.price_per_minute ?: 0.0)} €/min"

        holder.progressBar.progress = t.battery
        holder.progressBar.progressTintList =
            android.content.res.ColorStateList.valueOf(
                when {
                    t.battery >= 60 -> android.graphics.Color.parseColor("#2E7D32")
                    t.battery >= 30 -> android.graphics.Color.parseColor("#F57C00")
                    else            -> android.graphics.Color.parseColor("#E8001C")
                }
            )

        // Status badge
        when (t.status.lowercase()) {
            "available", "disponible" -> {
                holder.tvStatus.text = "Disponible"
                holder.tvStatus.setBackgroundColor(
                    android.graphics.Color.parseColor("#E8F5E9"))
                holder.tvStatus.setTextColor(
                    android.graphics.Color.parseColor("#2E7D32"))
            }
            "in_use", "en_cours" -> {
                holder.tvStatus.text = "En cours"
                holder.tvStatus.setBackgroundColor(
                    android.graphics.Color.parseColor("#FFF8E1"))
                holder.tvStatus.setTextColor(
                    android.graphics.Color.parseColor("#F57C00"))
            }
            else -> {
                holder.tvStatus.text = t.status
                holder.tvStatus.setBackgroundColor(
                    android.graphics.Color.parseColor("#FFEEEE"))
                holder.tvStatus.setTextColor(
                    android.graphics.Color.parseColor("#E8001C"))
            }
        }

        holder.btnBook.setOnClickListener { onClick(t) }
    }

    override fun getItemCount() = list.size
}