package com.example.dailylifemap

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.util.Log
import android.view.View
import com.example.dailylifemap.PermissionManager.existDeniedPermission
import com.example.dailylifemap.PermissionManager.showOnlyRequestAnd
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.MarkerIcons
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.lang.Math.abs

val permissionsForLocation = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION
    , Manifest.permission.ACCESS_FINE_LOCATION
    , Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

class MapActivity : AppCompatActivity() {

    companion object{
        //for permission check
        private const val STARTING = 10000
        const val IN_NEW_LOCATION = 10001
    }

    var mapDisplaying: NaverMap? = null
        private set
    private lateinit var locationStamper: LStamper
    private var myPathOverlay: PathOverlay = PathOverlay()

    private val markersOfWeekdays = mutableListOf<List<Marker>>()
    private var placesOfWeekdays: PlaceData? = null

    //For Test
    private lateinit var timelineRepoEntry: LTimelineRepoEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startupCheck()
        mountMap()
        TimelineConstructor.isRunningTimelineConstruction(this)

        locationStamper = LStamper(this)

        timelineRepoEntry = LTimelineRepoEntry(this.application)

        /*buttonGetLocation.setOnClickListener {
            *//*locationStamper.requestNowLocation(IN_NEW_LOCATION){
                it?.let{
                    mapInstance?.moveCamera(CameraUpdate.scrollTo((LatLng(it.latitude, it.longitude))))
                    timelineRepository.locationInsert(it)
                }
            }

            TimelineConstructor.isRunningTimelineConstruction(this)
        }*/

        /*buttonPrintLocation.setOnClickListener {
            timelineRepoEntry.locationSelectAll { timeline ->
                timeline.forEach {
                    Log.d("print", "${it.time} : ${it.toString()}")
                }
            }
        }*/

        buttonShowPlaces.setOnClickListener {
            viewWeekDayList.visibility = View.VISIBLE
            viewWeekDayList.adapter = WeekRecyclerViewAdapter(this)
            it.visibility = View.INVISIBLE
        }
    }

    private fun setPlaceStayingAndMarkers() {
        val weekdayDataMaker = WeekdayDataMaker(this)
        placesOfWeekdays = weekdayDataMaker.getLongTimePlacesOnThisWeek()
        placesOfWeekdays?.let{
            it.forEach { placesOfWeekdays ->
                val nowDayMarkerList = mutableListOf<Marker>()

                placesOfWeekdays.forEach { nowPlaceLocation ->
                    nowDayMarkerList.add(
                        Marker().apply {
                            position = LatLng(nowPlaceLocation.latitude, nowPlaceLocation.longitude)
                            map = mapDisplaying
                            width = 80
                            height = 120
                            icon = MarkerIcons.LIGHTBLUE
                            isVisible = true
                        })
                }

                markersOfWeekdays.add(nowDayMarkerList)
            }
        }
    }

    private fun startupCheck(){
        if(existDeniedPermission(this, permissionsForLocation))
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
            mapDisplaying = mountedMap

            var showingTimeStart = System.currentTimeMillis()
            showingTimeStart -= showingTimeStart % DateUtils.DAY_IN_MILLIS

            if(myPathOverlay.coords.size >= 2) {

            }

            mountedMap.onMapClickListener =
                NaverMap.OnMapClickListener { _, _ ->
                    viewWeekDayList.visibility = View.INVISIBLE
                    buttonShowPlaces.visibility = View.VISIBLE
                }

            setPlaceStayingAndMarkers()
        }
    }

    override fun onRequestPermissionsResult(
        functionCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(functionCode){
            STARTING -> {
                if(existDeniedPermission(this, permissions))
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

    fun setNewWeekday(numWeekday: Int){
        markersOfWeekdays.forEachIndexed { nowWeekday, nowWeekdayMarkers ->
            if(nowWeekdayMarkers.isEmpty())
                return@forEachIndexed

            if(nowWeekday == numWeekday) {
                val nowDayTime = System.currentTimeMillis() % DAY_IN_MILLIS
                var placePresumedIndex = 0
                var placeIsTwoOrMore = false
                var minTimeDifference = 0L

                placesOfWeekdays?.let { placesOfWeekdays ->
                    if (placesOfWeekdays[nowWeekday].count() > 1)
                        placeIsTwoOrMore = true
                }

                nowWeekdayMarkers.forEachIndexed { nowPlaceOrderNum, marker ->
                    marker.isVisible = true

                    if(placeIsTwoOrMore){
                        placesOfWeekdays?.let { placesOfWeekdays ->
                            val pastPlaceTime =
                                placesOfWeekdays[nowWeekday][nowPlaceOrderNum].time % DAY_IN_MILLIS

                            val nowTimeDifference = kotlin.math.abs(nowDayTime - pastPlaceTime)

                            if(nowTimeDifference < minTimeDifference) {
                                minTimeDifference = nowTimeDifference
                                placePresumedIndex = nowPlaceOrderNum
                            }
                        }
                    }
                }

                placesOfWeekdays?.let {
                    val nowWeekdayPlaces = it[nowWeekday]
                    val placePresumed = nowWeekdayPlaces[placePresumedIndex]
                    mapDisplaying?.moveCamera(
                        CameraUpdate.scrollTo(
                            LatLng(placePresumed.latitude, placePresumed.longitude)
                        ).animate(CameraAnimation.Easing)
                    )
                }
            }
            else
                nowWeekdayMarkers.forEach { marker ->
                    marker.isVisible = false
                }
        }
    }
}