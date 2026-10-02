package com.aistudio.drinkyourwater.hydra.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.drinkyourwater.hydra.MainActivity
import com.aistudio.drinkyourwater.hydra.R
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.data.model.UserProfile
import com.aistudio.drinkyourwater.hydra.ui.components.soundOptions

object NotificationHelper {
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    /** Pre-chime channel with the system default sound. Removed so it doesn't linger in Settings. */
    private const val LEGACY_CHANNEL_ID = "hydration_reminders"
    private const val CHANNEL_PREFIX = "hydration_reminders_"
    private const val DEFAULT_SOUND = "tone1"

    private val toneResources = mapOf(
        "tone1" to R.raw.tone1,
        "tone2" to R.raw.tone2,
        "tone3" to R.raw.tone3,
        "tone4" to R.raw.tone4
    )

    private fun soundKey(sound: String?): String = sound?.takeIf { it in toneResources } ?: DEFAULT_SOUND

    /**
     * Android 8+ fixes a channel's sound when the channel is created, so each chime gets its
     * own channel and the profile's chosen chime picks which channel a reminder posts to.
     */
    fun channelIdFor(sound: String?): String = CHANNEL_PREFIX + soundKey(sound)

    private fun soundUri(context: Context, sound: String?): Uri =
        Uri.parse("android.resource://${context.packageName}/${toneResources.getValue(soundKey(sound))}")

    /** Creates the channel for [sound] if needed and returns its id. */
    fun ensureChannel(context: Context, sound: String? = DEFAULT_SOUND): String {
        val channelId = channelIdFor(sound)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val manager = context.getSystemService(NotificationManager::class.java) ?: return channelId
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(channelId) == null) {
            val title = soundOptions.firstOrNull { it.id == soundKey(sound) }?.title ?: "Chime"
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                channelId,
                "Hydration Reminders ($title)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to complete your hydration and habit check-ins"
                enableVibration(true)
                setSound(soundUri(context, sound), audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    /** Plays [sound] once, for previewing chimes in Settings. */
    fun previewSound(context: Context, sound: String) {
        MediaPlayer.create(context, toneResources.getValue(soundKey(sound)))?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    fun showReminderNotification(context: Context, reminder: Reminder, profile: UserProfile?) {
        val channelId = ensureChannel(context, profile?.notificationSound)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = when (profile?.motivationalStyle) {
            "affirmations" -> "You show up for yourself. Every single time."
            "scripture" -> "\"Let anyone who is thirsty come to me and drink.\" - John 7:37"
            else -> "Tap to confirm and unlock your screen."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.text)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri(context, profile?.notificationSound)) // pre-Android 8; channels own it after
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }
}
