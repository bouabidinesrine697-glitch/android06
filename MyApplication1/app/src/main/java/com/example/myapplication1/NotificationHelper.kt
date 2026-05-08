package com.example.myapplication1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "cvelo_notifications"
    private var notificationId   = 0

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifications TrottiTours",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Réservations et paiements"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // ✅ Notification réservation confirmée
    fun notifyReservationConfirmed(
        context:     Context,
        stationName: String,
        startTime:   String,
        endTime:     String,
        totalCost:   Double
    ) {
        send(
            context = context,
            title   = "✅ Réservation confirmée !",
            body    = "Station : $stationName\n" +
                    "Début : $startTime\n" +
                    "Fin   : $endTime\n" +
                    "Total : ${"%.2f".format(totalCost)} €",
            intent  = Intent(context, MapActivity::class.java)
        )
    }

    // ✅ Notification paiement réussi
    fun notifyPaymentSuccess(
        context:   Context,
        amount:    Double,
        bookingId: Int
    ) {
        send(
            context = context,
            title   = "💳 Paiement réussi !",
            body    = "Montant : ${"%.2f".format(amount)} €\n" +
                    "Réservation #$bookingId confirmée",
            intent  = Intent(context, MapActivity::class.java)
        )
    }

    // ✅ Notification rappel
    fun notifyReminder(
        context:     Context,
        stationName: String,
        startTime:   String
    ) {
        send(
            context = context,
            title   = "⏰ Rappel de réservation",
            body    = "Votre vélo à $stationName\n" +
                    "commence à $startTime",
            intent  = Intent(context, MapActivity::class.java)
        )
    }

    private fun send(
        context: Context,
        title:   String,
        body:    String,
        intent:  Intent
    ) {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId++,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pin)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(android.graphics.Color.parseColor("#E8001C"))
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(notificationId, notification)
    }
}