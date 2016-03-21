package kr.hyosang.drivediary.client;

import android.app.Activity;
import android.util.Log;

public abstract class BaseActivity extends Activity implements BaseUtil {
	public void log(String log) {
		Log.d("DriveDiary", String.format("[%s] %s", getTag(), log));
	}

}
