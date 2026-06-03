package com.hire.smartcompress.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hire.smartcompress.MainActivity
import com.hire.smartcompress.R

class CompressionForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "compression_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_PROGRESS = "progress"

        fun startIntent(context: Context, fileName: String): Intent =
            Intent(context, CompressionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FILE_NAME, fileName)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, CompressionForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "file"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                startForeground(NOTIFICATION_ID, buildNotification(fileName, progress))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    fun updateProgress(fileName: String, progress: Int) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(fileName, progress))
    }

    private fun buildNotification(fileName: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compressing…")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "File Compression",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows compression progress"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
