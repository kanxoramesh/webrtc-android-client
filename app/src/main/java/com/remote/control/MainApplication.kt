package com.remote.control

import android.Manifest
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.*
import com.orhanobut.hawk.Hawk
import com.remote.remote.screenshare.RemoteScreenActivity
import com.remote.remote.screenshare.CallerUser
import com.remote.remote.screenshare.RemoteBuilder
import com.remote.remote.screenshare.LibraryBuilderListener
import java.util.*

/**
 * Main Application
 * This class initialize [RemoteBuilder]
 * Developed by : Ramesh
 */

class MainApplication : Application(), LibraryBuilderListener {

    var notificationManager: NotificationManager? = null


    override fun onCreate() {
        super.onCreate()
        Hawk.init(this).build()
        sInstance = this

        val settings: FirebaseFirestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            /* .setHost("10.0.2.2:8080")
             .setSslEnabled(false)*/
            .build()

        FirebaseFirestore.getInstance().setFirestoreSettings(settings)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
        createNotificationChannel()
         RemoteBuilder.initInstance(applicationContext, this)
    }

    /**
     * This method first checks Imei( i.e Unique Remote id ) and calculate imei if not .
     * If already has imei, then initialize the firebase document change listener for the domunt with imei
     */
    public fun checkListenerFeasibilityFromService() {
        if (getIMEI() != null) {
            RemoteBuilder.getInstance()?.initListener(getIMEI()!!)
        } else
            openTelePhony(applicationContext)
    }

    /**
     * Save Imei in Hawk library
     * @param imei this is unique device id which is used for unique remote id while controlling remote device
     */
    override fun saveIMEI(imei: String) {
        Hawk.put("IMEI", imei.trim())
    }

    /**
     * get Imei from Hawk library
     * @return imei this is unique device id which is used for unique remote id while controlling remote device
     */
    override fun getIMEI(): String? {
        //return "EMULATOR30X0X0X0"
        return Hawk.get<String>("IMEI", null)
    }

    /**
     * this will open telephony to identify the device serial number.
     * if serial number is not supported, we use some random uuid to identify devices.
     * this uuid is valid over the single application instance
     */
    fun openTelePhony(context: Context?) {
        context?.let {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //check permission
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        var a = android.os.Build.getSerial()
                        RemoteBuilder.getInstance()?.initListener(a)
                    } else {

                    }
                } else {
                    RemoteBuilder.getInstance()?.initListener(android.os.Build.SERIAL)

                }
            } else {

                RemoteBuilder.getInstance()?.initListener(UUID.randomUUID().toString());
            }
        }
    }


    /**
     * @param i unique notification id
     * @param callerUser this user wants to access to your device . this is admin or super admin
     * @param requestControlTitle title of why user wants to control device
     * @param requestControlDescription details of why user wants to control device
     *
     */
    override fun notifyNotification(
        i: Int,
        callerUser: CallerUser,
        requestControlTitle: String?,
        requestControlDescription: String?
    ) {
        notificationManager?.notify(
            3,createNotification(
                callerUser
                , requestControlTitle, requestControlDescription
            )
        )

    }

    /**
     * crete notification alert in notification system tray
     * @param user this user wants to access to your device . this is admin or super admin
     * @param request_control_title title of why user wants to control device
     * @param request_control_description details of why user wants to control device
     *
     */
     fun createNotification(
        user: com.remote.remote.screenshare.CallerUser?,
        request_control_title: String?,
        request_control_description: String?
    ): Notification {

        val denyIntent = Intent(this, ControlService::class.java)
        denyIntent.action = DENY_REMOTE
        val flags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> FLAG_IMMUTABLE
            else -> FLAG_UPDATE_CURRENT
        }
        val denyIntentPendingIntent = PendingIntent.getService(this, 22, denyIntent, flags)


        val allowIntent = Intent(this, RemoteScreenActivity::class.java)
        allowIntent.action = ACCEPT_REMOTE

        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntentWithParentStack(allowIntent)

        val resultPendingIntent =
            stackBuilder.getPendingIntent(33, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, ControlService.CHANNEL_ID)
            .setContentTitle(
                request_control_title?.let { it } ?: kotlin.run { "remote control" }
            )
            .setContentText(
                request_control_description?.let { user?.callerName + " " + it } ?: kotlin.run {
                    " Currently ${
                        user?.callerName
                    } wants to view your device. Please grant the access "
                }
            )
            .setSmallIcon(R.drawable.ic_launcher_background)
            .addAction(
                R.drawable.ic_baseline_cancel_24, getString(R.string.allow),
                resultPendingIntent
            ).addAction(
                R.drawable.ic_baseline_cancel_24, getString(R.string.deny),
                denyIntentPendingIntent
            )
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)
            .build()

    }


    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                ControlService.CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * this will remove the current instance of requested user or admin/ super admin
     */
    fun resetCaller() {
        RemoteBuilder.getInstance()?.resetCaller()

    }

    /**
     * remove notification with id from tray
     * @param i notification id to remove
     */
    override fun removeNotification(i: Int) {
       notificationManager?.cancel(i)
    }


    companion object {
        /**
         * Mainapplication instance
         */
        private var sInstance: MainApplication? = null

        var DENY_REMOTE = "DENY_REMOTE"
        var ACCEPT_REMOTE = "ACCEPT_REMOTE"
        fun getInstance(): MainApplication? {
            return MainApplication.sInstance
        }

    }

}