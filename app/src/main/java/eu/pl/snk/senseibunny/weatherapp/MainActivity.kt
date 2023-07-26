package eu.pl.snk.senseibunny.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import eu.pl.snk.senseibunny.weatherapp.databinding.ActivityMainBinding
import eu.pl.snk.senseibunny.weatherapp.models.WeatherResponse
import eu.pl.snk.senseibunny.weatherapp.network.WeatherService
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding ?=null

    private lateinit var LocationProvider: FusedLocationProviderClient

    public var mLatitude: Double = 0.0

    public var mLongitude: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        LocationProvider = LocationServices.getFusedLocationProviderClient(this)

        if (isLocationEnabled().equals(false)) { //if location is disabled go to sett
            Toast.makeText(this, "Location is not enabled", Toast.LENGTH_LONG).show()
            val Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(Intent)

        } else { //else we want to get permissions
            Dexter.withActivity(this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Location Enabled",
                            Toast.LENGTH_LONG
                        ).show()
                        requestNewLocationData()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()


        }
    }

    private fun getLocationWeatherDetails(){
        if(Const.isNetworkAvailable(this)){
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(getString(R.string.BASE_URL)) //pass the base url
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService = retrofit.create(WeatherService::class.java) //We make URL complete

            val listCall: Call<WeatherResponse> = service.getWeather(mLatitude, mLongitude,getString(R.string.API_KEY), getString(R.string.METRIC_UNIT)) //We make call to the server

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) { //We get response from the server
                    if(response.isSuccessful){
                        val weatherList: WeatherResponse = response.body()!!
                        Log.d("TAG", "onResponse: ${weatherList.toString()}")
                    }
                    else{
                        val rc= response.code()
                        when(rc){
                            400 -> Log.e("TAG", "onResponse 400: ${response.errorBody()}")
                            404-> Log.e("TAG", "onResponse 400: ${response.errorBody()}")
                            else -> Log.e("TAG", "error")
                        }
                    }
                }
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) { //We get error from the server
                    Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
        }
        else{
            Toast.makeText(this,"Network is not available", Toast.LENGTH_LONG).show()
        }
    }

    private fun isLocationEnabled(): Boolean { //asking if location is enabled
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // getting location manager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showRationaleDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you didnt give permissions thats sad L").setPositiveButton("Go to Settings") { dialog, which ->
            try{
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } catch (e:Exception){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }.show()

    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()

        LocationProvider.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            mLatitude=mLastLocation!!.latitude
            mLongitude=mLastLocation!!.longitude
            Log.d("LOG", "Latitude: " + mLatitude + "Longitude: " + mLongitude)

            getLocationWeatherDetails()

        }
    }
}