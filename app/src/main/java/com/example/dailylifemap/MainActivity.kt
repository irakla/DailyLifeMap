package com.example.dailylifemap

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dailylifemap.PermissionManager.isExist_deniedPermission
import com.example.dailylifemap.PermissionManager.showRequest
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

val permissionCodeForLocation = 1000
val permissionsForLocation = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION
    , Manifest.permission.ACCESS_FINE_LOCATION
    //, Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

class MainActivity : AppCompatActivity() {

    private var mapInstance: NaverMap? = null
    private lateinit var locationStamper: LStamper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isExist_deniedPermission(this, permissionsForLocation))
            showRequest(this, permissionsForLocation, permissionCodeForLocation,
                "위치관련", "위치조회, 앱 오프라인 위치조회")

        val mapFragment = fragmentMap as MapFragment?
            ?: MapFragment.newInstance().also{
                supportFragmentManager.beginTransaction().add(fragmentMap.id, it).commit()
            }

        mapFragment.getMapAsync {
            mapInstance = it
        }

        locationStamper = LStamper(this)

        buttonPrintLocation.setOnClickListener {
            locationStamper.updateTheLatestLocation {
                it?.let{ mapInstance?.moveCamera(CameraUpdate.scrollTo((LatLng(it.latitude, it.longitude))))}
            }
        }
    }
}