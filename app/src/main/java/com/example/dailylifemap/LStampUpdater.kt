package com.example.dailylifemap

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.work.Worker
import androidx.work.WorkerParameters

class LStampUpdater() : Service() {

    companion object {
        class BootReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Intent.ACTION_BOOT_COMPLETED.equals(intent.action)) {
                    val serviceIntent = Intent(context, LStampUpdater::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(serviceIntent)
                    else
                        context.startService(serviceIntent)
                }
            }
        }
    }

    private lateinit var locationStamper: LStamper

    class WorkerForPeriodicStamping(
        appContext: Context
        ,workerParams: WorkerParameters, private val locationStamper: LStamper)
        : Worker(appContext, workerParams) {

        override fun doWork(): Result {
            locationStamper.requestNowLocation(PermissionManager.IS_NOT_ACTIVITY){
                it?.let{
                    //TODO : 주기적으로 위치를 받는다면 수행하는 작업
                }
            }

            return Result.success()
        }
    }


    override fun onCreate() {
        /*if (gatheringTimer != null) {
            gatheringTimer?.cancel()
        } else {
            // recreate new
            gatheringTimer = Timer()
        }
        // schedule task

        stamperInBackground = GPSStamper(applicationContext)

        val preference = applicationContext.getSharedPreferences(GPSStamper.nameUsingPreference, Context.MODE_PRIVATE)
        val prevTimeGetLocation = preference.getLong(GPSStamper.prevStampTimeKey, 0)
        Log.i(this.javaClass.name + ".prevGetLocationTime", prevTimeGetLocation.toString())
        val passedTimeFromLastLocation = System.currentTimeMillis() - prevTimeGetLocation
        val periodSettedLocationRefresh = min_PeriodLocationRefresh * MINUTE_BY_MILLI_SEC

        gatheringTimer?.scheduleAtFixedRate(PeriodicLocationGatheringTask(),
            if(passedTimeFromLastLocation < periodSettedLocationRefresh)
                periodSettedLocationRefresh - passedTimeFromLastLocation
            else
                0
            , periodSettedLocationRefresh
        )

        startInForeground()*/

        locationStamper = LStamper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}