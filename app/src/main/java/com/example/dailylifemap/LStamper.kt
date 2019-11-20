package com.example.dailylifemap

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class LStamper(private val appContext : Context) {

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var settingLocationRequest: LocationRequest
    private var nowActiveCallback: OnlyForOneLocation? = null

    private inner class OnlyForOneLocation(
        private val appContext: Context
        , private val locationProvider: FusedLocationProviderClient
        , private val callbackWithNewLocation: ((Location?) -> Unit)? = null
        ) : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            val positionCurrent = locationResult?.lastLocation
            positionCurrent?.let{
                Log.d("New Location"
                    , "${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}")
            }

            locationProvider.removeLocationUpdates(this)
            if(nowActiveCallback == this)   //this is the latest callback
                nowActiveCallback = null

            callbackWithNewLocation?.invoke(positionCurrent)
        }
    }

    init{
        setLocationProvider()
    }

    private fun setLocationProvider(){
        locationProvider = LocationServices.getFusedLocationProviderClient(appContext)

        settingLocationRequest = LocationRequest().apply{
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 3000
            maxWaitTime = 30000                                  //milliseconds
        }
    }

    fun updateTheLatestLocation(doingWithNewLocation: ((Location?) -> Unit)? = null){
        nowActiveCallback = OnlyForOneLocation(appContext, locationProvider, doingWithNewLocation)
        locationProvider.requestLocationUpdates(settingLocationRequest, nowActiveCallback, Looper.myLooper())
    }
}