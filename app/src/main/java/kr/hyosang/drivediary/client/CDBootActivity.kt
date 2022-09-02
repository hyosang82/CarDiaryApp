package kr.hyosang.drivediary.client

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import kr.hyosang.drivediary.client.service.GpsService
import kr.hyosang.drivediary.client.service.IGpsService
import kr.hyosang.drivediary.client.util.SharedPref

class CDBootActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SharedPref.getInstance().getRegistered()) {
            startService()
        }

        finish()
    }

    fun startService() {
        val i = Intent(this@CDBootActivity, GpsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i)
        } else {
            startService(i)
        }
    }
}