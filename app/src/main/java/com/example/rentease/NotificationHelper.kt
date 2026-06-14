package com.example.rentease

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Local notification helper for RentEase.
 * Uses NotificationManager (no FCM needed).
 */
object NotificationHelper {

    private const val CHANNEL_RENTAL = "rental_updates"
    private const val CHANNEL_OWNER = "owner_alerts"
    private const val CHANNEL_OVERDUE = "overdue_alerts"

    private var notificationIdCounter = 1000

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val rentalChannel = NotificationChannel(
            CHANNEL_RENTAL,
            "Update Penyewaan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi saat rental disetujui, ditolak, atau dikembalikan"
            enableVibration(true)
        }

        val ownerChannel = NotificationChannel(
            CHANNEL_OWNER,
            "Permintaan Sewa Barang",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi saat ada yang ingin menyewa barang Anda"
            enableVibration(true)
        }

        val overdueChannel = NotificationChannel(
            CHANNEL_OVERDUE,
            "Peringatan Keterlambatan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan saat rental melewati batas pengembalian"
            enableVibration(true)
        }

        manager.createNotificationChannel(rentalChannel)
        manager.createNotificationChannel(ownerChannel)
        manager.createNotificationChannel(overdueChannel)
    }

    /**
     * Show notification when rental status changes (approved/rejected).
     */
    fun showRentalStatusNotification(
        context: Context,
        itemName: String,
        newStatus: String
    ) {
        val (title, message, iconRes) = when (newStatus) {
            "approved" -> Triple(
                "Rental Disetujui!",
                "Penyewaan '$itemName' telah disetujui oleh petugas.",
                android.R.drawable.ic_dialog_info
            )
            "rejected" -> Triple(
                "Rental Ditolak",
                "Penyewaan '$itemName' telah ditolak oleh petugas.",
                android.R.drawable.ic_dialog_alert
            )
            "returned" -> Triple(
                "Barang Dikembalikan",
                "'$itemName' telah berhasil dikembalikan. Terima kasih!",
                android.R.drawable.ic_dialog_info
            )
            else -> return
        }

        showNotification(context, CHANNEL_RENTAL, title, message, iconRes, "rental_status")
    }

    /**
     * Show notification to item owner when someone rents their item.
     */
    fun showOwnerRentalRequestNotification(
        context: Context,
        renterName: String,
        itemName: String,
        duration: Int
    ) {
        val title = "Permintaan Sewa Baru!"
        val message = "$renterName ingin menyewa '$itemName' selama $duration hari."
        showNotification(context, CHANNEL_OWNER, title, message, android.R.drawable.ic_dialog_email, "owner_request")
    }

    /**
     * Show overdue notification (for petugas).
     */
    fun showOverdueNotification(
        context: Context,
        itemName: String,
        renterName: String,
        daysOverdue: Int
    ) {
        val title = "Rental Terlewat Batas!"
        val message = "'$itemName' oleh $renterName sudah lewat $daysOverdue hari dari batas pengembalian."
        showNotification(context, CHANNEL_OVERDUE, title, message, android.R.drawable.ic_dialog_alert, "overdue")
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        iconRes: Int,
        tag: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app when notification tapped
        val intent = Intent(context, SplashScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationIdCounter, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        manager.notify(tag, notificationIdCounter++, notification)
    }
}
