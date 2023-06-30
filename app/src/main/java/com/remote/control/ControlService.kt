package com.remote.control

import android.app.*
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.TelephonyManager
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import com.orhanobut.hawk.Hawk

/**
 * Main foreground service
 *
 */

class ControlService : Service() {


    var notification: Notification? = null
    var networkStatsManager: NetworkStatsManager? = null
    var telephonyManager: TelephonyManager? = null

    /**
     * initialize hawk database
     * and start to listen remote control request
     */
    override fun onCreate() {
        super.onCreate()
        if (!Hawk.isBuilt())
            Hawk.init(this).build();
        createNotificationChannel()

        notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build()
        startForeground(1, notification)
        networkStatsManager =
            applicationContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager


        MainApplication.getInstance()?.checkListenerFeasibilityFromService()


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            if (it.action == MainApplication.DENY_REMOTE) {
                MainApplication.getInstance()?.resetCaller()
                MainApplication.getInstance()?.removeNotification(3)

            }
        }
        return START_STICKY
    }


    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }


    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

}
