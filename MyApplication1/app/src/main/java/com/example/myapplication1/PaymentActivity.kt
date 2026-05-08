package com.example.myapplication1

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.databinding.ActivityPaymentBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.content.Intent

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var stripe: Stripe

    private val STRIPE_PUBLISHABLE_KEY = "pk_test_VOTRE_CLE_ICI"
    private val BACKEND_URL = "http://10.0.2.2:8000/api/payment/"

    private var totalCost   = 0.0
    private var stationName = ""
    private var bookingId   = 0
    private var startTime   = ""
    private var endTime     = ""
    private var duration    = ""
    private var distStr     = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Récupère données de BookingActivity
        totalCost   = intent.getDoubleExtra("total_cost", 0.0)
        stationName = intent.getStringExtra("station_name") ?: ""
        bookingId   = intent.getIntExtra("booking_id", 0)
        startTime   = intent.getStringExtra("start_time") ?: ""
        endTime     = intent.getStringExtra("end_time") ?: ""
        duration    = intent.getStringExtra("duration") ?: ""
        distStr     = intent.getStringExtra("dist_str") ?: ""

        // Initialise Stripe
        PaymentConfiguration.init(applicationContext, STRIPE_PUBLISHABLE_KEY)
        stripe = Stripe(applicationContext, STRIPE_PUBLISHABLE_KEY)

        // Affiche résumé
        binding.tvAmount.text    = "${"%.2f".format(totalCost)} €"
        binding.tvStation.text   = stationName
        binding.tvBookingId.text = "Réservation #$bookingId"
        binding.tvStartTime.text = "Début : $startTime"
        binding.tvEndTime.text   = "Fin   : $endTime"
        binding.tvDuration.text  = "Durée : $duration"

        if (distStr.isNotEmpty()) {
            binding.tvDistance.text       = "Distance : $distStr"
            binding.tvDistance.visibility = View.VISIBLE
        }

        // Carte de test
        binding.tvTestCard.text =
            "Mode test — Carte : 4242 4242 4242 4242\n" +
                    "Expiration : 12/34   CVC : 123"

        binding.btnPay.setOnClickListener {
            val amountCents = (totalCost * 100).toInt()
            processPayment(amountCents)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun processPayment(amountCents: Int) {
        binding.btnPay.isEnabled           = false
        binding.progressBar.visibility     = View.VISIBLE

        // ✅ Récupère PaymentMethodCreateParams directement depuis le widget
        val paymentMethodParams = binding.cardInputWidget.paymentMethodCreateParams

        if (paymentMethodParams == null) {
            Toast.makeText(this,
                "Veuillez entrer une carte valide",
                Toast.LENGTH_SHORT).show()
            binding.btnPay.isEnabled       = true
            binding.progressBar.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Étape 1 — Crée PaymentMethod
                val paymentMethod = stripe.createPaymentMethodSynchronous(
                    paymentMethodParams
                )

                if (paymentMethod == null) {
                    showError("Impossible de créer la méthode de paiement")
                    return@launch
                }

                // ✅ Étape 2 — Envoie au backend Django
                val clientSecret = createPaymentIntent(
                    amountCents     = amountCents,
                    paymentMethodId = paymentMethod.id ?: "",
                    bookingId       = bookingId
                )

                if (clientSecret == null) {
                    showError("Erreur serveur — vérifiez votre backend Django")
                    return@launch
                }

                // ✅ Étape 3 — Confirme le paiement
                val result = stripe.confirmPaymentIntentSynchronous(
                    ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        paymentMethodId = paymentMethod.id ?: "",
                        clientSecret    = clientSecret
                    )
                )

                withContext(Dispatchers.Main) {
                    when (result?.status?.toString()) {
                        "Succeeded" -> onPaymentSuccess()
                        "RequiresAction" -> showError("Authentification 3D Secure requise")
                        else -> showError("Paiement échoué : ${result?.status}")
                    }
                }

            } catch (e: Exception) {
                showError("Erreur : ${e.message}")
            }
        }
    }

    private fun createPaymentIntent(
        amountCents: Int,
        paymentMethodId: String,
        bookingId: Int
    ): String? {
        return try {
            val client  = OkHttpClient()
            val json    = JSONObject().apply {
                put("amount",            amountCents)
                put("currency",          "eur")
                put("payment_method_id", paymentMethodId)
                put("booking_id",        bookingId)
            }
            val body    = json.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BACKEND_URL)
                .post(body)
                .build()

            val response     = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody ?: "{}")

            if (jsonResponse.has("client_secret")) {
                jsonResponse.getString("client_secret")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun onPaymentSuccess() {
        binding.progressBar.visibility   = View.GONE
        binding.btnPay.isEnabled         = true
        binding.layoutForm.visibility    = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE

        // ✅ Notification locale paiement réussi
        NotificationHelper.notifyPaymentSuccess(
            context   = this,
            amount    = totalCost,
            bookingId = bookingId
        )

        // ✅ Notification réservation confirmée
        NotificationHelper.notifyReservationConfirmed(
            context     = this,
            stationName = stationName,
            startTime   = startTime,
            endTime     = endTime,
            totalCost   = totalCost
        )

        // Retour carte après 3 secondes
        binding.root.postDelayed({
            startActivity(
                Intent(this, MapActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            finish()
        }, 3000)
    }

    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            binding.progressBar.visibility = View.GONE
            binding.btnPay.isEnabled       = true
            Toast.makeText(
                this@PaymentActivity,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}