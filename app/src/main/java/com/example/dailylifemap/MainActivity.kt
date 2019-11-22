package com.example.dailylifemap

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dailylifemap.PermissionManager.existDeniedpermission
import com.example.dailylifemap.PermissionManager.showOnlyRequestAnd
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

val permissionsForLocation = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION
    , Manifest.permission.ACCESS_FINE_LOCATION
    , Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

class MainActivity : AppCompatActivity() {

    private var mapInstance: NaverMap? = null
    private lateinit var locationStamper: LStamper

    companion object{
        //for permission check
        private const val STARTING = 10000
        private const val MOVE_TO_NOW_LOCATION = 10001
    }

    //For Test
    private lateinit var locationTimelineViewModel: LTimelineViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        val mapFragment = fragmentMap as MapFragment?
            ?: MapFragment.newInstance().also{
                supportFragmentManager.beginTransaction().add(fragmentMap.id, it).commit()
            }

        mapFragment.getMapAsync {
            mapInstance = it
        }

        locationStamper = LStamper(this)

        locationTimelineViewModel = LTimelineViewModel(this.application)

        buttonGetLocation.setOnClickListener {
            locationStamper.requestNowLocation(MOVE_TO_NOW_LOCATION){
                it?.let{
                    mapInstance?.moveCamera(CameraUpdate.scrollTo((LatLng(it.latitude, it.longitude))))
                    //locationTimelineViewModel.insertLocation(it)
                }
            }
        }

        buttonPrintLocation.setOnClickListener {
            //locationTimelineViewModel.selectLocation()
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
            /*MOVE_TO_NOW_LOCATION -> {
                if(!PermissionManager.existDeniedpermission(this, permissions))
                    setMapToNowLocation()
                else
                    toast("권한이 허가되지 않아 기능을 이용할 수 없습니다.")
            }*/
        }
    }
}