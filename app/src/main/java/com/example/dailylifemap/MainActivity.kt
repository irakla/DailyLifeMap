package com.example.dailylifemap

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.UiThread
import com.example.dailylifemap.PermissionManager.existDeniedpermission
import com.example.dailylifemap.PermissionManager.showOnlyRequestAnd
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

val permissionsForLocation = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION
    , Manifest.permission.ACCESS_FINE_LOCATION
    , Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

class MainActivity : AppCompatActivity() {

    companion object{
        //for permission check
        private const val STARTING = 10000
        const val IN_NEW_LOCATION = 10001
    }

    private var mapInstance: NaverMap? = null
    private lateinit var locationStamper: LStamper
    private var myPathOverlay: PathOverlay = PathOverlay()

    init{

    }

    //For Test
    private lateinit var timelineRepository: LTimelineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startupCheck()
        mountMap()
        TimelineConstructor.isRunningTimelineConstruction(this)

        locationStamper = LStamper(this)

         timelineRepository = LTimelineRepository(this.application)

        buttonGetLocation.setOnClickListener {
            /*locationStamper.requestNowLocation(IN_NEW_LOCATION){
                it?.let{
                    mapInstance?.moveCamera(CameraUpdate.scrollTo((LatLng(it.latitude, it.longitude))))
                    timelineRepository.locationInsert(it)
                }
            }*/

            TimelineConstructor.isRunningTimelineConstruction(this)
        }

        buttonPrintLocation.setOnClickListener {
            //TODO : is still exploding
            timelineRepository.locationSelect {timeline ->
                timeline.forEach {
                    Log.d("print", "${it.time} : ${it.toString()}")
                }
            }
        }
    }

    private fun startupCheck(){
        if(existDeniedpermission(this, permissionsForLocation))
            showOnlyRequestAnd(this, permissionsForLocation, STARTING,
                "어플리케이션의 기능을 정상적으로 사용하기 위해 " +
                        "위치 조회, 앱 오프라인 위치조회 권한이 필요합니다."){ _, _ ->
                showOnlyRequestAnd(this, permissionsForLocation, STARTING,
                    "위치권한이 없으면 DailyLifeMap 앱 전반의 이용이 어렵습니다!\n" +
                            "가급적이면 권한을 허가해주세요!"){ _, _ ->
                    toast("기능 대부분이 작동하지 않습니다.")
                }
            }

        TimelineConstructor.checkTimelineConstructionWorker(this)
    }

    private fun mountMap(){
        val mapFragment = fragmentMap as MapFragment?
            ?: MapFragment.newInstance().also{
                supportFragmentManager.beginTransaction().add(fragmentMap.id, it).commit()
            }

        mapFragment.getMapAsync { mountedMap ->
            mapInstance = mountedMap

            timelineRepository.locationSelect {locationList ->
                if(locationList.size >= 2) {
                        myPathOverlay.coords =
                            locationList.map { LatLng(it.latitude, it.longitude) }
                    runOnUiThread {
                        myPathOverlay.map = mountedMap
                        mapInstance?.moveCamera(CameraUpdate.scrollTo(myPathOverlay.coords.last()))
                    }
                }
            }

            if(myPathOverlay.coords.size >= 2) {

            }
        }
    }

    override fun onRequestPermissionsResult(
        functionCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(functionCode){
            STARTING -> {
                if(existDeniedpermission(this, permissions))
                    showOnlyRequestAnd(this, permissionsForLocation, STARTING,
                        "위치권한이 없으면 DailyLifeMap 앱 전반의 이용이 어렵습니다!\n" +
                                "가급적이면 권한을 허가해주세요!"){ _, _ ->
                        toast("기능 대부분이 작동하지 않습니다.")
                    }
            }
            /*IN_NEW_LOCATION -> {
                if(!PermissionManager.existDeniedpermission(this, permissions))
                    setMapToNowLocation()
                else
                    toast("권한이 허가되지 않아 기능을 이용할 수 없습니다.")
            }*/
        }
    }
}