package kr.hyosang.drivediary.client.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.*

class SharedPref {
    private val KEY_VEH_UUID = "vehicle_uuid"
    private val KEY_REGISTERED = "is_registered"

    companion object {
        private lateinit var inst: SharedPref

        fun init(context: Context) {
            inst = SharedPref(context)
        }

        fun getInstance(): SharedPref {
            return inst
        }
    }

    private val pref: SharedPreferences
    constructor(context: Context) {
        pref = context.getSharedPreferences("cardiary.pref", Context.MODE_PRIVATE)
    }

    fun getVehicleUuid(): String {
        var uuid = pref.getString(KEY_VEH_UUID, null)
        if(uuid == null) {
            uuid = UUID.randomUUID().toString()
            pref.edit { putString(KEY_VEH_UUID, uuid) }
        }

        return uuid
    }

    fun getRegistered(): Boolean {
        return pref.getBoolean(KEY_REGISTERED, false)
    }

    fun setRegistered(yes: Boolean) {
        pref.edit { putBoolean(KEY_REGISTERED, yes) }
    }


}