package kr.hyosang.drivediary.client;

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kr.hyosang.drivediary.client.util.SharedPref

public class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = if(SharedPref.getInstance().getRegistered()) {
            Intent(this, MainActivity::class.java)
        }else {
            Intent(this, RegisterActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
