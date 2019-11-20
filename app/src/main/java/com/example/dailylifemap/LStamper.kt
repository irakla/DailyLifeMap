package com.example.dailylifemap

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import org.jetbrains.anko.toast

class LStamper(private val appContext : Context) {

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var settingLocationRequest: LocationRequest
    private var nowActiveCallback: OnlyForOneLocation? = null

    class OnlyForOneLocation(
        private val appContext: Context,
        private val locationProvider: FusedLocationProviderClient
        ) : LocationCallback() {
        private var positionLast: Location? = null

        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            positionLast = locationResult?.lastLocation

            val positionCurrent = positionLast
            if(positionCurrent != null) {
                appContext.toast("${"%.6f".format(positionCurrent.latitude)}, ${"%.6f".format(positionCurrent.longitude)}")
            }

            locationProvider.removeLocationUpdates(this)
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

     fun getNowLocation(){
        nowActiveCallback = OnlyForOneLocation(appContext, locationProvider)
        locationProvider.requestLocationUpdates(settingLocationRequest, nowActiveCallback, Looper.myLooper())
    }
}