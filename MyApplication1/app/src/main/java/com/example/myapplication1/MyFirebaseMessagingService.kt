package com.example.myapplication1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Réservation"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Votre réservation est confirmée"

        val type = remoteMessage.data["type"] ?: "reservation"

        when (type) {
            "reservation_confirmed" -> sendNotification(
                title   = title,
                body    = body,
                icon    = R.drawable.ic_map,
                color   = "#E8001C",
                intent  = Intent(this, MapActivity::class.java)
            )
            "payment_success" -> sendNotification(
                title   = title,
                body    = body,
                icon    = R.drawable.ic_info,
                color   = "#2E7D32",
                intent  = Intent(this, MapActivity::class.java)
            )
            "reservation_reminder" -> sendNotification(
                title   = title,
                body    = body,
                icon    = R.drawable.ic_pin,
                color   = "#FF6F00",
                intent  = Intent(this, MapActivity::class.java)
            )
            else -> sendNotification(title, body,
                R.drawable.ic_info, "#E8001C",
                Intent(this, MapActivity::class.java))
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // ✅ Envoie le token au backend Django
        sendTokenToServer(token)
    }

    private fun sendNotification(
        title:  String,
        body:   String,
        icon:   Int,
        color:  String,
        intent: Intent
    ) {
        val channelId   = "cvelo_notifications"
        val requestCode = System.currentTimeMillis().toInt()

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri    = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val manager     = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // ✅ Crée le canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications TrottiTours",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description       = "Réservations et paiements"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(android.graphics.Color.parseColor(color))
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(requestCode, notification)
    }

    private fun sendTokenToServer(token: String) {
        // TODO: envoyer via Retrofit au backend Django
        // retrofitService.updateFcmToken(userId, token)
        android.util.Log.d("FCM", "Token: $token")
    }
}