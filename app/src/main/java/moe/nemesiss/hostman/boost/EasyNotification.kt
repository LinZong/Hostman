package moe.nemesiss.hostman.boost

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import moe.nemesiss.hostman.R
import moe.nemesiss.hostman.service.NetTrafficService

object EasyNotification {

    // Notification channel for NetTrafficService foreground notification
    const val NET_TRAFFIC_CHANNEL_ID = "net_traffic_service_channel"
    private const val NET_TRAFFIC_CHANNEL_NAME = "Net Traffic Monitor"
    private const val NET_TRAFFIC_CHANNEL_DESC = "Notifications for Net Traffic Monitor service"

    // Request code for POST_NOTIFICATIONS permission
    const val REQ_POST_NOTIFICATIONS = 0x2001
    const val NET_TRAFFIC_SERVICE_NOTIFICATION_ID = 0x2001

    /**
     * Ensure the notification channel for NetTrafficService exists.
     * Safe to call multiple times; no-op on pre-O devices.
     */
    fun ensureNetTrafficNotificationChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NET_TRAFFIC_CHANNEL_ID,
                NET_TRAFFIC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NET_TRAFFIC_CHANNEL_DESC
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Send a notification indicating the Net Traffic Monitor Service is running.
     * Will silently do nothing on Android 13+ if POST_NOTIFICATIONS is not granted.
     */
    fun notifyNetTrafficServiceRunning(service: NetTrafficService) {
        ensureNetTrafficNotificationChannel(service)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                service, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // No permission to post notifications; skip to avoid SecurityException.
                return
            }
        }

        val notification = createNetTrafficServiceRunningNotification(service)

        service.startForeground(NET_TRAFFIC_SERVICE_NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    fun createNetTrafficServiceRunningNotification(ctx: Context): Notification {
        val notification = NotificationCompat.Builder(ctx, NET_TRAFFIC_CHANNEL_ID)
            .setSmallIcon(R.drawable.speed_24px)
            .setContentTitle(NET_TRAFFIC_CHANNEL_NAME)
            .setContentText(ctx.getString(R.string.net_traffic_monitor_is_running))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return notification
    }

    fun removeNetTrafficServiceRunningNotification(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(NET_TRAFFIC_SERVICE_NOTIFICATION_ID)
    }

    fun checkPostNotificationPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return true
        }
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        return granted
    }

    /**
     * Ensure POST_NOTIFICATIONS permission is granted (Android 13+).
     * - If granted or API < 33: returns immediately.
     * - If we should show rationale: open app notification settings to let user grant manually.
     * - Otherwise: request the permission if current [context] is [Activity], or just open system settings for user to manually allow notifications.
     */
    fun ensurePostNotificationsPermission(context: Context) {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            return
        }

        if (context is Activity) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                                                                    Manifest.permission.POST_NOTIFICATIONS)
            ) {
                // Show system settings for user to manually allow notifications
                openAppNotificationSettings(context)
            } else {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_POST_NOTIFICATIONS
                )
            }
        } else {
            // Show system settings for user to manually allow notifications
            openAppNotificationSettings(context)
        }
    }

    /**
     * Handle POST_NOTIFICATIONS permission result. If denied, open app notification settings.
     * Returns true if this request code was handled.
     */
    fun handlePostNotificationsPermissionResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != REQ_POST_NOTIFICATIONS) return false
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            openAppNotificationSettings(activity)
        }
        return true
    }

    /**
     * Open the app's notification settings (or app details as a fallback).
     */
    fun openAppNotificationSettings(ctx: Context) {
        val pkg = ctx.packageName
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                }

                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:$pkg")
                }
            }
        }

        runCatching {
            EasyIntent.startActivity(ctx, intent)
        }.onFailure {
            // Fallback to app details settings if notification settings action isn't supported
            val fallback = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", pkg, null)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { EasyIntent.startActivity(ctx, fallback) }
        }
    }
}