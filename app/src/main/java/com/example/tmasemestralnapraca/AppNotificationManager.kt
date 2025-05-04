package com.example.tmasemestralnapraca

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.tmasemestralnapraca.player.PlayerModel
import com.example.tmasemestralnapraca.post.PostModel
import java.util.concurrent.atomic.AtomicInteger

class AppNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "app_notifications"
        private val notificationIdGenerator = AtomicInteger(1000)

        fun getNextNotificationId(): Int {
            return notificationIdGenerator.incrementAndGet()
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikácie aplikácie"
            val descriptionText = "Notifikácie o zmenách v aplikácii"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Získanie PendingIntent pre otvorenie hlavnej aktivity
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
    }

    /**
     * Zobrazenie notifikácie
     */
    private fun showNotification(
        title: String,
        text: String,
        notificationId: Int = getNextNotificationId()
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(notificationId, builder.build())
    }

    fun showPlayerAddedNotification(player: PlayerModel) {
        val playerName = "${player.firstName} ${player.lastName}"
        val notificationTitle = "Nový hráč pridaný"
        val notificationText = "Hráč $playerName bol pridaný do súpisky, bež to pozrieť!"

        showNotification(notificationTitle, notificationText)
    }

    fun showPostAddedNotification(post: PostModel) {
        val notificationTitle = "Nový príspevok pridaný"
        val notificationText = "Príspevok \"${post.postHeader}\" bol pridaný, bež to pozrieť!"

        showNotification(notificationTitle, notificationText)
    }

    fun showImageAddedNotification() {
        val notificationTitle = "Nový obrázok v galérii"
        val notificationText = "Do galérie bol pridaný nový obrázok, bež to pozrieť!"

        showNotification(notificationTitle, notificationText)
    }

    fun showMultipleImagesAddedNotification(count: Int) {
        val notificationTitle = "Nové obrázky v galérii"
        val notificationText = "Do galérie bolo pridaných $count nových obrázkov, bež to pozrieť!"

        showNotification(notificationTitle, notificationText)
    }
}