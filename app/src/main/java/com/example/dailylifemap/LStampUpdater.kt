package com.example.dailylifemap

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import org.jetbrains.anko.toast

class LStampUpdater() : Service() {

    override fun onCreate() {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}