package com.example.dailylifemap

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val INTERVAL_MILLI_FOR_GMT0900 = 9 * 60 * 60 * 1000
const val DAY_BY_MILLI_SEC = 24 * 60 * 60 * 1000
const val MINUTE_BY_MILLI_SEC = 60000
const val ZERODAY = 0

class TimeSupporter {
    companion object {
        fun translateTimeToWeekday(epochMilliSecond: Long) : String{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timezdt =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilliSecond), ZoneId.of("Asia/Seoul"))

                val format = DateTimeFormatter.ofPattern("EEE요일")

                return timezdt.format(format)
            }

            val translateToFullTimeFormat = SimpleDateFormat("EEE")

            return translateToFullTimeFormat.format(
                Date(epochMilliSecond + INTERVAL_MILLI_FOR_GMT0900)
            )
        }

        fun translateTimeToDate(epochMilliSecond: Long) : String{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timezdt =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilliSecond), ZoneId.of("Asia/Seoul"))

                val format = DateTimeFormatter.ofPattern("MM월 dd일")

                return timezdt.format(format)
            }

            val translateToFullTimeFormat = SimpleDateFormat("MM-dd")

            return translateToFullTimeFormat.format(
                Date(epochMilliSecond + INTERVAL_MILLI_FOR_GMT0900)
            )
        }
    }
}