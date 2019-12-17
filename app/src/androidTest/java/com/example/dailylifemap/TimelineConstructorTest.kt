package com.example.dailylifemap

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.RuntimeException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class TimelineConstructorTest {
    private lateinit var context: Context
    private lateinit var executor: Executor

    @get:Rule
    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_COARSE_LOCATION
        , Manifest.permission.ACCESS_BACKGROUND_LOCATION
        , Manifest.permission.ACCESS_FINE_LOCATION
    )

    @Before
    fun setup(){
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        //setMockLocation(37.3026, 127.9311, 50.0)
    }

    @Test
    fun testTimelineConstructor(){
        val worker = TestWorkerBuilder<TimelineConstructor>(
            context = context
            , executor = executor
            , inputData = Data.EMPTY
        ).build()

        val result = worker.doWork()
        Assert.assertEquals(result, ListenableWorker.Result.success())
    }

    @Test
    fun checkConstructorStillRunning(){
        Assert.assertEquals(TimelineConstructor.isRunningTimelineConstruction(context), true)
    }
}