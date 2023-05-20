package com.example.locationtrack

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Menu
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locationtrack.api.ApiInterface
import com.example.locationtrack.api.RetrofitInstance
import com.example.locationtrack.databinding.ActivityDummyBinding
import com.example.locationtrack.service.LocationService
import com.example.locationtrack.util.Util
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*

class DummyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDummyBinding
    private lateinit var phoneNo: String
    private lateinit var lt: String
    private lateinit var lo: String
    private lateinit var sp: String
    private lateinit var latitudeEditText: TextView
    private lateinit var longitudeEditText: TextView
    private lateinit var speedEditText: TextView
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var prefs: SharedPreferences
    private val KEY_TEXT = "saved_phone_no"

    var mLocationService: LocationService = LocationService()
    lateinit var mServiceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDummyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //getting phone number even after resuming the app
        if(savedInstanceState != null) {
            binding.phoneEt.setText(savedInstanceState.getString(KEY_TEXT))
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@DummyActivity)

        latitudeEditText = binding.latitudeEt
        longitudeEditText = binding.longitudeEt
        speedEditText = binding.speedEt

//        binding.getLocation.setOnClickListener {
//            getLocationOf()
//        }

        prefs = getSharedPreferences("myPref", MODE_PRIVATE)

        binding.startServiceBtn.setOnClickListener {
            val phone = binding.phoneEt.text.toString()
            val phonePattern = "^[1-9][0-9]{9}\$".toRegex()
            if(binding.phoneEt.text.toString().isEmpty() || !phone.matches(phonePattern)){
                AlertDialog.Builder(this).setTitle("Invalid Phone Number")
                    .setMessage("Please enter a valid phone number and try again")
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .setIcon(R.drawable.baseline_error_24).show()
            }else{
                val editor = prefs.edit()
                editor.putString("phone", binding.phoneEt.text.toString())
                editor.apply()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                            AlertDialog.Builder(this).apply {
                                setTitle("Background permission")
                                setMessage(R.string.background_location_permission_message)
                                setPositiveButton("Start service anyway",
                                    DialogInterface.OnClickListener { dialog, id ->
                                        starServiceFunc()
                                    })
                                setNegativeButton("Grant background Permission",
                                    DialogInterface.OnClickListener { dialog, id ->
                                        requestBackgroundLocationPermission()
                                    })
                            }.create().show()

                        }else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED){
                            starServiceFunc()
                        }
                    }else{
                        starServiceFunc()
                    }

                }else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        AlertDialog.Builder(this)
                            .setTitle("ACCESS_FINE_LOCATION")
                            .setMessage("Location permission required")
                            .setPositiveButton(
                                "OK"
                            ) { _, _ ->
                                requestFineLocationPermission()
                            }
                            .create()
                            .show()
                    } else {
                        requestFineLocationPermission()
                    }
                }
            }
        }

//        binding.stopServiceBtn.setOnClickListener {
//            stopServiceFunc()
//        }

        lt = binding.latitudeEt.text.toString()
        lo = binding.longitudeEt.text.toString()
        sp = binding.speedEt.text.toString()
        phoneNo = binding.phoneEt.text.toString()

//        binding.submit.setOnClickListener {
////            val scope = CoroutineScope(Dispatchers.IO)
////            scope.launch {
////                try {
////                    val client = OkHttpClient()
////
////                    val requestBody = MultipartBody.Builder()
////                        .setType(MultipartBody.FORM)
////                        .addFormDataPart("lt", lt)
////                        .addFormDataPart("lg", lo)
////                        .addFormDataPart("sp", sp)
////                        .addFormDataPart("ph", phoneNo)
////                        .addFormDataPart("dv", "5")
////                        .build()
////
////                    val request = Request.Builder()
////                        .url("https://api.tranzol.com/apiv1/PostLocation")
////                        .header("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
////                        .header("clientid", "TRANZOLGPS")
////                        .header("clientsecret", "TRANZOLBO436535345SS2238RC")
////                        .post(requestBody)
////                        .build()
////
////                    val call = client.newCall(request)
////                    val response = call.execute()
////                    if (response.isSuccessful) {
////                        // Process the response as needed
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(this@DummyActivity, "Response sent to server successfully", Toast.LENGTH_SHORT).show()
////                        }
////                    } else {
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(this@DummyActivity, "Response not sent to server", Toast.LENGTH_SHORT).show()
////                        }
////                    }
////                }catch (e: Exception) {
////                    e.printStackTrace()
////                }
////            }
//
//            val ltc = binding.latitudeEt.text.toString()
//            val loc = binding.longitudeEt.text.toString()
//            val spc = binding.speedEt.text.toString()
//            val phoneNoc = binding.phoneEt.text.toString()
//
//            if (isNetworkConnected()) {
//                sendData(ltc, loc, spc, phoneNoc)
//            } else {
//                AlertDialog.Builder(this).setTitle("No Internet Connection")
//                    .setMessage("Please check your internet connection and try again")
//                    .setPositiveButton(R.string.ok) { _, _ -> }
//                    .setIcon(R.drawable.baseline_error_24).show()
//            }
//        }
    }

    private fun sendData(ltc: String, loc: String, spc: String, phoneNoc: String) {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            AlertDialog.Builder(this).setTitle("Error")
                .setMessage(exception.message)
                .setPositiveButton(R.string.ok) { _, _ -> }
                .setIcon(R.drawable.baseline_error_24).show()
        }
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        coroutineScope.launch(errorHandler) {
            try {
                val response = RetrofitInstance.api.postLocation("$ltc", "$loc", "$spc", "$phoneNoc", "5")
                if (response.isSuccessful) {
                    // Process the response as needed
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DummyActivity, "Response sent to server successfully,  ${response.code()} ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DummyActivity, "Response not sent to server", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLocationOf() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return

        }
        val location = fusedLocationProviderClient.lastLocation
        location.addOnSuccessListener {
            if(it != null){
                latitudeEditText.setText(it.latitude.toString())
                longitudeEditText.setText(it.longitude.toString())
                speedEditText.setText(it.speed.toString())
            }
        }
    }

    private fun starServiceFunc(){
        mLocationService = LocationService()
        mServiceIntent = Intent(this, mLocationService.javaClass)
        if (!Util.isMyServiceRunning(mLocationService.javaClass, this)) {
            startService(mServiceIntent)

            Toast.makeText(this, getString(R.string.service_start_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.service_already_running), Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopServiceFunc(){
        mLocationService = LocationService()
        mServiceIntent = Intent(this, mLocationService.javaClass)
        if (Util.isMyServiceRunning(mLocationService.javaClass, this)) {
            stopService(mServiceIntent)
            Toast.makeText(this, "Service stopped!!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Service is already stopped!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork

        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        /*  if (::mServiceIntent.isInitialized) {
              stopService(mServiceIntent)
          }*/
        super.onDestroy()
        if (::mServiceIntent.isInitialized) {
              stopService(mServiceIntent)
        }
    }

    private fun requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), MY_BACKGROUND_LOCATION_REQUEST)
    }

    private fun requestFineLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,), MY_FINE_LOCATION_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Toast.makeText(this, requestCode.toString(), Toast.LENGTH_LONG).show()
        when (requestCode) {
            MY_FINE_LOCATION_REQUEST -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                        requestBackgroundLocationPermission()
                    }

                } else {
                    Toast.makeText(this, "ACCESS_FINE_LOCATION permission denied", Toast.LENGTH_LONG).show()
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", this.packageName, null),),)
                    }
                }
                return
            }
            MY_BACKGROUND_LOCATION_REQUEST -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Background location Permission Granted", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Background location permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TEXT, binding.phoneEt.text.toString())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val MY_FINE_LOCATION_REQUEST = 99
        private const val MY_BACKGROUND_LOCATION_REQUEST = 100
    }

}