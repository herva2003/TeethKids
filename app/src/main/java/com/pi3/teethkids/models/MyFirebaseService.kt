package com.pi3.teethkids.models

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pi3.teethkids.R
import com.pi3.teethkids.datastore.UserPreferencesRepository
import com.pi3.teethkids.fragments.emergencias.MostrarEmergenciasFragment

private const val USER_PREFERENCES_NAME = "prefs_tokens"
private const val FCMTOKEN_KEY = "fcmToken"

class MyFirebaseService : FirebaseMessagingService() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val msgData = remoteMessage.data
        val notification = remoteMessage.notification
        val msg = msgData["name"]
        showNotification(msg!!)
    }

    private fun sendRegistrationToServer(token: String?) {
        userPreferencesRepository = UserPreferencesRepository.getInstance(this)
        userPreferencesRepository.updateFcmToken(token!!)
    }

    override fun onNewToken(token: String) {
        sendRegistrationToServer(token)

        // Save the token to shared preferences
        val sharedPreferences =
            applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        editor?.putString(FCMTOKEN_KEY, token)
        editor?.apply()
    }

    private fun showNotification(messageBody: String) {
        val intent = Intent(this, MostrarEmergenciasFragment::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(getString(R.string.fcm_default_title_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0, notificationBuilder.build())
    }
}
