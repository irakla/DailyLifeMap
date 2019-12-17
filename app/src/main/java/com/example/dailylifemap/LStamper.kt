package com.example.dailylifemap

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationResult.extractResult
import com.google.android.gms.location.LocationResult.hasResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

class LStamper(private val appContext : Context) {

 private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var settingLocationRequest: LocationRequest
    private var nowActiveCallback: OnlyForOneLocation? = null

    companion object{
        val permissionForLocation: Array<out String> = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private inner class OnlyForOneLocation(
        private val locationProvider: FusedLocationProviderClient
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
            if(nowActiveCallback === this)   //this is the latest callback
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

    fun requestNowLocation(
        functionCode: Int = PermissionManager.IS_NOT_ACTIVITY
        , doingWithNewLocation: ((Location?) -> Unit)? = null
    ){
        val nowActivity = appContext as? Activity
        if(nowActivity != null)             //If invoker is in activity
            nowActivity.let {
                if(!isOnDeviceLocationSetting()) {
                    showSelectionForLocationSetting(it)
                    return
                }

                if (PermissionManager.existDeniedPermission(it, permissionForLocation)) {
                    PermissionManager.showOnlyRequestAnd(
                        it, permissionForLocation, functionCode
                        , "현재 위치를 파악하려면 위치 조회 권한이 필요합니다."
                    )
                    { _, _ ->
                        it.toast("권한이 없어 현재 위치를 조회할 수 없습니다.")
                    }

                    return
                }

                updateTheLatestLocation(doingWithNewLocation)
            }

        else                                //If invoker is not in activity
            if (!PermissionManager.existDeniedPermission(appContext, permissionForLocation)
                && isOnDeviceLocationSetting())
                updateTheLatestLocation(doingWithNewLocation)
    }

    private fun updateTheLatestLocation(
        doingWithNewLocation: ((Location?) -> Unit)? = null){

        if(nowActiveCallback == null)
            nowActiveCallback = OnlyForOneLocation(locationProvider, doingWithNewLocation)

        if(appContext is Activity) {          //Caller is in foreground
            appContext.toast("현재 위치 조회중...")

            locationProvider.requestLocationUpdates(
                settingLocationRequest,
                nowActiveCallback,
                Looper.myLooper()
            )
        }
        else if(doingWithNewLocation != null){  //Caller isn't in foreground
            val intent = Intent(appContext, LocationUpdaterByBroadcastReceiving::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            LocationUpdaterByBroadcastReceiving.reserveLocationWork(
                doingWithNewLocation, locationProvider, pendingIntent)
            locationProvider.requestLocationUpdates(settingLocationRequest, pendingIntent)
        }
    }

    class LocationUpdaterByBroadcastReceiving : BroadcastReceiver(){
        companion object {
            private val locationWorkList
                    = mutableListOf<((Location?) -> Unit)>()
            private val locationRequestSet
                    = mutableSetOf<Pair<FusedLocationProviderClient, PendingIntent>>()

            fun reserveLocationWork(
                locationWork: (Location?) -> Unit
                , locationProvider: FusedLocationProviderClient
                , pendingIntent: PendingIntent){
                synchronized(this){
                    locationWorkList.add(locationWork)
                    locationRequestSet.add(Pair(locationProvider, pendingIntent))
                }
            }
        }

        override fun onReceive(context: Context?, intentWithLocation: Intent?) {
            Log.d("Location custom BR", "is Work.")

            if(intentWithLocation == null)
                return

            if(intentWithLocation.action.equals(context?.getString(R.string.action_location)))
                return

            Log.d("Location custom BR", "is Received.")

            var nowLocation: Location? = null

            if(hasResult(intentWithLocation)) {
                val locationResult = extractResult(intentWithLocation)

                nowLocation = locationResult.lastLocation

                if(nowLocation.accuracy <= 15.0)
                    GlobalScope.launch{
                        val nowReservedWorks = locationWorkList
                        val nowRequestSet = locationRequestSet

                        nowRequestSet.forEach { requestSet ->
                            requestSet.first.removeLocationUpdates(requestSet.second)
                        }

                        nowReservedWorks.forEach { it(nowLocation) }

                        locationWorkList.removeAll(nowReservedWorks)
                    }
            }
        }
    }

    private fun isOnDeviceLocationSetting() : Boolean{
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showSelectionForLocationSetting(activity: Activity){
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)

        builder.setMessage("위치 조회를 위해서 휴대폰의 위치 조회 기능을 켜야 합니다.")
        builder.setPositiveButton("예") { _, _ ->
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        builder.setNegativeButton("아니오"){ _, _ ->
            activity.toast("기능이 켜져있지 않아 위치 조회를 할 수 없습니다.")
        }

        builder.show()
    }
}