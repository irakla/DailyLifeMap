package com.example.dailylifemap

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MainActivity : AppCompatActivity() {

    private var mapInstance: NaverMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = mapView as MapFragment?
            ?: MapFragment.newInstance().also{
                supportFragmentManager.beginTransaction().add(R.id.mapView, it).commit()
            }

        try{
            val info = packageManager.getPackageInfo("com.example.dailylifemap", PackageManager.GET_SIGNATURES)
            for(signature in info.signatures){
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.e("KeyHash : ", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        }catch(e: PackageManager.NameNotFoundException){ e.printStackTrace() }
        catch(e: NoSuchAlgorithmException){ e.printStackTrace() }

        mapFragment.getMapAsync {
            mapInstance = it
        }
    }
}