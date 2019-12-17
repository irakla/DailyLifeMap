package com.example.dailylifemap

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.os.SystemClock.sleep
import android.text.format.DateUtils
import android.util.Log
import androidx.work.*
import java.lang.Exception
import java.util.concurrent.TimeUnit


class TimelineConstructor(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams){

    companion object {
        private const val TIMELINE_WORK_TAG = "TimelineWork"
        private const val KEY_DELAY = "KeyDelay"
        private const val INTERVAL_DELAY = 7 * DateUtils.MINUTE_IN_MILLIS

        fun checkTimelineConstructionWorker(appContext: Context){
            if(!isRunningTimelineConstruction(appContext))
            {
                val timelineWorkNoDelay = PeriodicWorkRequestBuilder<TimelineConstructor>(
                    15, TimeUnit.MINUTES
                    , 5, TimeUnit.MINUTES
                ).setInputData(workDataOf(
                    KEY_DELAY to INTERVAL_DELAY
                )).build()

                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    TIMELINE_WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, timelineWorkNoDelay
                )
            }
            else
            {
                cancelStrangeWorkers(appContext)
            }
        }

        //for check that TimelineConstructor is already enqueued.
        fun isRunningTimelineConstruction(context: Context) : Boolean {
            val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(TIMELINE_WORK_TAG)
            val workList = workInfos.get()

            var isRunning = false

            workList.forEach {
                isRunning = isRunning || (it.state == WorkInfo.State.RUNNING) || (it.state == WorkInfo.State.ENQUEUED)
            }

            Log.d("Timeline Work Check"
                , "isRunning : $isRunning")

            return isRunning
        }

        private fun cancelStrangeWorkers(context: Context){
            val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(TIMELINE_WORK_TAG)
            val workList = workInfos.get()

            Log.d("workList Size", workList.size.toString())
            if(workList.size > 1) {

                workList.dropLast(0).forEach{
                    WorkManager.getInstance(context).cancelWorkById(it.id)
                    Log.d("workList 작업종료됨", it.id.toString())
                }
            }
        }
    }

    private val locationStamper = LStamper(appContext)
    private val timelineRepository = LTimelineRepoEntry(appContext)

    override fun doWork(): Result {
        return try{
            val delay = inputData.getLong(KEY_DELAY, 0)

            locationStamper.requestNowLocation(PermissionManager.IS_NOT_ACTIVITY){
                    Log.d("Location WorkManager", "location is arrived : ${it.toString()}")
                    it?.let{
                        try {
                            timelineRepository.locationInsert(it)
                        }catch(e: SQLiteConstraintException){
                            Log.d("Location Insert", "This is Not a New Location.")
                        }

                        Log.d("Worker's Location work","Work is done.")
                    }
            }

            sleep(delay)

            locationStamper.requestNowLocation(PermissionManager.IS_NOT_ACTIVITY){
                Log.d("Location WorkManager", "location is arrived : ${it.toString()}")
                it?.let{
                    try {
                        timelineRepository.locationInsert(it)
                    }catch(e: SQLiteConstraintException){
                        Log.d("Location Insert", "This is Not a New Location.")
                    }

                    Log.d("Worker's Location work","Work is done.")
                }
            }

            Result.success()
        }catch(exception: Exception){
            Log.d("TimelineConstructor", "exception : ", exception)
            Result.failure()
        }
    }
}