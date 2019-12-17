package com.example.dailylifemap

import android.content.Context
import android.location.Location
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.WEEK_IN_MILLIS
import android.util.Log
import com.naver.maps.geometry.LatLng
import com.example.dailylifemap.TimeSupporter.Companion.translateTimeToDate
import com.example.dailylifemap.TimeSupporter.Companion.translateTimeToWeekday
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.math.pow

typealias PlaceData = List<List<Location>>

class WeekdayDataMaker(private val context: Context) {
    companion object{
        fun getWeekdayTitleLatestOrder() : List<String> {
            val daysString = mutableListOf<String>()
            val nowTime = System.currentTimeMillis()

            daysString.add("오늘 (${translateTimeToDate(nowTime)})")

            for(fromToday in 1 .. 7) {
                val theDayTime = nowTime - DAY_IN_MILLIS * fromToday
                daysString.add("${translateTimeToWeekday(theDayTime)} (${translateTimeToDate(theDayTime)})")
            }

            return daysString
        }
    }

    private val timelineDB = LTimelineRepoEntry(context)

    fun getLongTimePlacesOnThisWeek() : PlaceData{
        val weekLRecords: MutableList<Location> = mutableListOf()
        val nowTime = System.currentTimeMillis()

        runBlocking {
            weekLRecords.addAll(async(start = CoroutineStart.DEFAULT) {
                timelineDB.locationSelectSince(
                    ((nowTime / DAY_IN_MILLIS) - 8) * DAY_IN_MILLIS
                )
            }.await())
        }

        val daysOfLRecord = convertFromWeekToDays(weekLRecords, nowTime)

        val placeLongTimeByDaysOfWeek: PlaceData = daysOfLRecord.map { recordsL ->
            val places = mutableListOf<Location>()

            when(recordsL.count()) {
                0 -> return@map places
                1, 2 -> return@map places.apply{
                    this.addAll(recordsL)
                }
            }

            var distanceSum = 0.0

            var averageDistancePlaceCount = 0
            var prevPosition = recordsL.first()
            var movingFlag = false

            var placeStayingLatitude = prevPosition.latitude
            var placeStayingLongitude =prevPosition.longitude
            var placeStayingTime = 0L
            var countUsedLocations = 1

            recordsL.drop(1).forEach { nowLocation ->
                val nowPassedDistance = calDistanceOnFlat(
                    prevPosition.latitude, prevPosition.longitude
                    , nowLocation.latitude, nowLocation.longitude
                )

                val averageDisplaceIdle = distanceSum / averageDistancePlaceCount
                if(nowPassedDistance > averageDisplaceIdle * 2
                    || nowPassedDistance > 0.0005){                   //user is still on the move
                    movingFlag = true
                }else if(movingFlag){                               //user stopped
                    movingFlag = false

                    val newLongtimePlace = Location("place calculated").apply{
                        latitude = placeStayingLatitude / countUsedLocations
                        longitude = placeStayingLongitude / countUsedLocations
                        time = placeStayingTime / countUsedLocations
                    }
                    places.add(newLongtimePlace)

                    placeStayingLatitude = nowLocation.latitude
                    placeStayingLongitude = nowLocation.longitude
                    placeStayingTime = nowLocation.time
                    countUsedLocations = 1
                }else{                                               ////user is still stop
                    placeStayingLatitude += nowLocation.latitude
                    placeStayingLongitude += nowLocation.longitude
                    placeStayingTime += nowLocation.time
                    countUsedLocations += 1

                    //stop상태 평균거리 계산
                    averageDistancePlaceCount += 1
                    distanceSum += nowPassedDistance
                }

                prevPosition = nowLocation
            }


            if(places.isEmpty()) {
                Log.d("isEmpty", "true")
                places.add(
                    Location("place calculated").apply{
                        latitude = placeStayingLatitude / countUsedLocations
                        longitude = placeStayingLongitude / countUsedLocations
                        time = placeStayingTime / countUsedLocations
                    }
                )
            }

            places
        }

        return placeLongTimeByDaysOfWeek
    }

    private fun convertFromWeekToDays(weekLRecords: List<Location>, nowTime: Long) : List<List<Location>> {
        val daysOfLRecord = mutableListOf<List<Location>>()

        daysOfLRecord.add(
            weekLRecords.filter { it.time > (nowTime - (nowTime % DAY_BY_MILLI_SEC)) }
        )

        daysOfLRecord[0].forEachIndexed { index, it -> Log.d("converting", "$index, ${it.time - nowTime + (nowTime % DAY_IN_MILLIS)}") }

        for(fromToday in 1 .. 6) {
            daysOfLRecord.add(weekLRecords.filter {
                (it.time > (nowTime / DAY_IN_MILLIS - fromToday) * DAY_IN_MILLIS)
                        && (it.time < (nowTime / DAY_IN_MILLIS - fromToday + 1) * DAY_IN_MILLIS)
            })
        }

        return daysOfLRecord
    }

    private fun calDistanceOnFlat(axis1_1: Double, axis2_1: Double, axis1_2: Double, axis2_2: Double)
            : Double {
        return (axis1_1 - axis1_2).pow(2) + (axis2_1 - axis2_2).pow(2)
    }
}