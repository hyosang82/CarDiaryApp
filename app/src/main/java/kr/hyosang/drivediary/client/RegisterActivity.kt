package kr.hyosang.drivediary.client;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kr.hyosang.drivediary.client.databinding.ActivityRegistBinding
import kr.hyosang.drivediary.client.util.HttpTask
import kr.hyosang.drivediary.client.util.SharedPref
import kr.hyosang.drivediary.client.viewmodel.RegistActivityViewModel
import org.json.JSONObject
import java.util.*

class RegisterActivity : AppCompatActivity() {
    lateinit var binding: ActivityRegistBinding
    lateinit var vm: RegistActivityViewModel

    private val codeRes = arrayOf(
        R.drawable.num_0,
        R.drawable.num_1,
        R.drawable.num_2,
        R.drawable.num_3,
        R.drawable.num_4,
        R.drawable.num_5,
        R.drawable.num_6,
        R.drawable.num_7,
        R.drawable.num_8,
        R.drawable.num_9
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm = RegistActivityViewModel()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_regist)
        binding.lifecycleOwner = this
        binding.activity = this
        binding.vm = vm

        //vm.pinDigit1Image.value = R.drawable.num_0

        loadPincode()
        vm.vehicleUuid.value = SharedPref.getInstance().getVehicleUuid()

    }

    fun loadPincode() {
        val uuid = SharedPref.getInstance().getVehicleUuid()
        HttpTask("https://cardiaryspringserver-6w3qlgf3zq-uc.a.run.app/vehicle/pincode?uuid=$uuid", "GET")
            .execute { task ->
                task.getJsonBody()?.also { j ->
                    val code = j.getString("pincode")
                    vm.pinDigit1Image.postValue(codeRes[(code[0].toInt()) - 48])
                    vm.pinDigit2Image.postValue(codeRes[(code[1].toInt()) - 48])
                    vm.pinDigit3Image.postValue(codeRes[(code[2].toInt()) - 48])
                    vm.pinDigit4Image.postValue(codeRes[(code[3].toInt()) - 48])

                }
                Log.d("TEST", "Response => ${task.responseBody}")
            }
    }

    fun checkComplete() {
        val uuid = SharedPref.getInstance().getVehicleUuid()
        HttpTask("https://cardiaryspringserver-6w3qlgf3zq-uc.a.run.app/vehicle/$uuid", "GET")
            .execute { task ->
                task.getJsonBody()?.also { j ->
                    val rxuuid = j.optString("vehicleUuid")
                    if(uuid == rxuuid) {
                        //OK
                        SharedPref.getInstance().setRegistered(true)
                        nextActivity()
                    }else {
                        val mesg = j.optString("message")
                        if((mesg != null) && (mesg.isNotEmpty())) {
                            vm.message.postValue(mesg)
                        }else {
                            vm.message.postValue("Unknown error? HTTP ${task.responseCode} ${task.responseMessage}")
                        }
                    }
                }
            }
    }

    private fun nextActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }




}