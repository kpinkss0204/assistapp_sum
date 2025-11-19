package com.example.assistapp_sum.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationTrackingService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var generatedKey: String? = null
    private val database = FirebaseDatabase.getInstance().reference.child("shared_locations")

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_GENERATED_KEY = "generated_key"

        fun startService(context: Context, generatedKey: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_GENERATED_KEY, generatedKey)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        generatedKey = intent?.getStringExtra(EXTRA_GENERATED_KEY)
        if (generatedKey == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).apply { setMinUpdateIntervalMillis(5000L) }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val key = generatedKey ?: return
                    val data = mapOf(
                        "lat" to location.latitude,
                        "lon" to location.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                    database.child(key).setValue(data)
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "위치 추적 서비스",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 위치 공유 중")
            .setContentText("실시간 위치가 공유됩니다")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
