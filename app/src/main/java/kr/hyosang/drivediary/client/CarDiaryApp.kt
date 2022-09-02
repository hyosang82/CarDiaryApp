package kr.hyosang.drivediary.client

import android.app.Application
import kr.hyosang.drivediary.client.util.SharedPref

class CarDiaryApp: Application() {
    override fun onCreate() {
        super.onCreate()

        SharedPref.init(applicationContext)
    }
}