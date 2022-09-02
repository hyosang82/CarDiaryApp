package kr.hyosang.drivediary.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegistActivityViewModel: ViewModel() {
    val pinDigit1Image = MutableLiveData<Int>()
    val pinDigit2Image = MutableLiveData<Int>()
    val pinDigit3Image = MutableLiveData<Int>()
    val pinDigit4Image = MutableLiveData<Int>()
    val vehicleUuid = MutableLiveData<String>()
    val message = MutableLiveData<String>()
}