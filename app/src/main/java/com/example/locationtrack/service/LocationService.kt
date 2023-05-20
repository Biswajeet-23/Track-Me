package com.example.locationtrack.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.locationtrack.R
import com.example.locationtrack.api.RetrofitInstance
import com.example.locationtrack.databinding.ActivityDummyBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefs: SharedPreferences
    private lateinit var dataPrefs: SharedPreferences
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

//    private val locationRequest: LocationRequest = create().apply {
//        interval = 10000  //3000
//        fastestInterval = 10000 //3000
//        priority = PRIORITY_BALANCED_POWER_ACCURACY
//        maxWaitTime = 12000 //5000
//    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
                Toast.makeText(this@LocationService, "Latitude: " + location.latitude.toString() + '\n' +
                        "Longitude: "+ location.longitude, Toast.LENGTH_LONG).show()
                Log.d("Location la", location.latitude.toString())
                Log.d("Location lg", location.longitude.toString())

                val ltc = location.latitude.toString()
                val loc = location.longitude.toString()
                val spc = location.speed.toString()

                //shared preferences for location
                val editor = dataPrefs.edit()
                editor.putString("latitude", location.latitude.toString())
                editor.putString("longitude", location.longitude.toString())
                editor.putString("speed", location.speed.toString())
                editor.apply()

                // shared preferences
                val phoneNoc = prefs.getString("phone", null)

                sendData(ltc, loc, spc, phoneNoc)

            }
        }
    }


    @SuppressLint("WakelockTimeout", "UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //initialise shared preferences
        prefs = getSharedPreferences("myPref", MODE_PRIVATE)
        dataPrefs = getSharedPreferences("dataPref", MODE_PRIVATE)


//        val locationRequest = LocationRequest.Builder(60000)
//            .setIntervalMillis(50000)
//            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
////            .setMinUpdateIntervalMillis(60000)
//            .setWaitForAccurateLocation(false)
//            .setMaxUpdateDelayMillis(70000)
//            .build()

        // initialize alarm manager and set the alarm to wake up the service every minute
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LocationService::class.java)
        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val intervalMillis = 60 * 1000L // 1 minute
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, intervalMillis, pendingIntent)

        // Acquire a partial wake lock to ensure that the service keeps running even when the screen is off
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LocationService::Wakelock"
        )
        wakeLock.acquire( )

        // Keep the Wi-Fi awake
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LocationServiceWifiLock")
        wifiLock.acquire()

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) createNotificationChanel() else startForeground(1, Notification())

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(applicationContext, "Permission required", Toast.LENGTH_LONG).show()
            return
        }
//        else{
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
//        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChanel() {
        val notificationChannelId = "Location channel id"
        val channelName = "Background Service"
        val chan = NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, notificationChannelId)
        val notification: Notification = notificationBuilder
            .setContentTitle("Location updates:")
            .setOngoing(true)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("SimpleDateFormat")
    private fun sendData(ltc: String?, loc: String?, spc: String?, phoneNoc: String?) {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            AlertDialog.Builder(this@LocationService).setTitle("Error")
                .setMessage(exception.message)
                .setPositiveButton(R.string.ok) { _, _ -> }
                .setIcon(R.drawable.baseline_assignment_late_24).show()
        }
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        coroutineScope.launch(errorHandler) {
            try {
                val response = RetrofitInstance.api.postLocation("$ltc", "$loc", "$spc", "$phoneNoc", "5")
                if (response.isSuccessful) {
                    // Process the response as needed
                    withContext(Dispatchers.Main) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        val currentDate = sdf.format(Date(System.currentTimeMillis()))
                        Toast.makeText(this@LocationService, "Response sent to server successfully,  ${response.code()} ", Toast.LENGTH_SHORT).show()
                        Log.d("Service Success", response.code().toString())
                        Log.d("Service Success", currentDate)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LocationService, "Response not sent to server", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //activity lifecycle which is responsible when the phone resumes, and help to perform task even when phone is in idle state
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //initialise shared preferences
        prefs = getSharedPreferences("myPref", MODE_PRIVATE)
        dataPrefs = getSharedPreferences("dataPref", MODE_PRIVATE)

        val locationRequest = LocationRequest.Builder(60000)
            .setIntervalMillis(60000)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(70000)
            .build()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        createNotificationChanel()

//        val coroutineScope = CoroutineScope(Dispatchers.Main)
//        coroutineScope.launch {
//            while(true){
//                delay(60 * 1000L)
//                sendLocationData()
//            }
//        }
        return START_STICKY
    }

    private fun sendLocationData() {
        val lt = dataPrefs.getString("latitude",null)
        val lg = dataPrefs.getString("longitude",null)
        val sp = dataPrefs.getString("speed",null)
        val phone = prefs.getString("phone", null)

        if(lt!=null && lg!=null && sp!=null && phone!=null){
            sendData(lt, lg, sp, phone)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        wifiLock.release()
        alarmManager.cancel(pendingIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}