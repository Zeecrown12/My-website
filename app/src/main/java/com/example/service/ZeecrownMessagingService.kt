package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.CommunicationRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZeecrownMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        val appCtx = applicationContext
        serviceScope.launch {
            CommunicationRepository(appCtx).registerDeviceToken(uid, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        ensureChannels(applicationContext)

        val data = message.data
        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: data["title"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: data["body"]?.takeIf { it.isNotBlank() }
            ?: return

        val category = data["category"].orEmpty().uppercase()
        val channelId = if (category == "PROMOTION" || category == "ANNOUNCEMENT") {
            CHANNEL_PROMOS
        } else {
            CHANNEL_ORDERS
        }

        val targetRoute = data["targetRoute"].orEmpty()
        val orderId = data["orderId"].orEmpty()

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetRoute.isNotBlank()) putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            if (orderId.isNotBlank()) putExtra(EXTRA_ORDER_ID, orderId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(this, requestCode, tapIntent, pendingFlags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(this).notify(requestCode, builder.build())
        }
    }

    companion object {
        const val CHANNEL_ORDERS = "zeecrown_orders"
        const val CHANNEL_PROMOS = "zeecrown_promos"
        const val EXTRA_TARGET_ROUTE = "zeecrown_target_route"
        const val EXTRA_ORDER_ID = "zeecrown_order_id"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                context.getString(R.string.notif_channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_orders_desc)
            }

            val promosChannel = NotificationChannel(
                CHANNEL_PROMOS,
                context.getString(R.string.notif_channel_promos_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_promos_desc)
            }

            manager.createNotificationChannels(listOf(ordersChannel, promosChannel))
        }
    }
}
