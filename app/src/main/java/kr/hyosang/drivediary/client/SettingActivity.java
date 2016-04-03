package kr.hyosang.drivediary.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import kr.hyosang.drivediary.client.R;

public class SettingActivity extends PreferenceActivity implements BaseUtil {
	private static final String KEY_INT_TIME = "pref_interval_time";
	private static final String KEY_INT_DIST = "pref_interval_displacement";
	private static final String KEY_TRIG_SPD = "pref_trigger_speed";
	private static final String KEY_VLD_ACCU = "pref_valid_accuracy";
	private static final String KEY_CONT_LOG = "pref_continue_logging";
	private static final String KEY_VIN = "pref_vin";
	private static final String KEY_INT_UPLD = "pref_interval_upload";
	private static final String KEY_UPLD_NOW = "pref_upload_now";
	private static final String KEY_SERV_HOST = "pref_server_host";
	private static final String KEY_DOWNLOAD = "pref_download_apk";
	
	public static int sIntervalTime = 5;
	public static int sIntervalDist = 10;
	public static int sTriggerSpeed = 20;
	public static float sValidAccuracy = 100.0f;
	public static String sVin = "TESTVIN";
	public static int sIntervalUpload = 30;
	public static int sContinueLogTime = 30;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		findPreference(KEY_INT_TIME).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_INT_DIST).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_TRIG_SPD).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_CONT_LOG).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_VLD_ACCU).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_VIN).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_INT_UPLD).setOnPreferenceChangeListener(mChangeListener);
		findPreference(KEY_UPLD_NOW).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				try {
					MainActivity.mService.requestUpload();
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}

		});
		findPreference(KEY_DOWNLOAD).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://raw.githubusercontent.com/hyosang82/CarDiaryApp/master/bin/CarDiaryApp.apk"));
				startActivity(i);
				return false;
			}
		});

		loadPreferences(this);
		updateSummary();
	}
	
	
	private OnPreferenceChangeListener mChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference pref, Object newValue) {
			(new Handler()).postDelayed(new Runnable() {
				@Override
				public void run() {
					//return true 이후 실제 적용되므로 지연을 준다
					loadPreferences(SettingActivity.this);
					updateSummary();
				}
			}, 500);
			return true;
		}
	};
	
	public static void loadPreferences(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		sIntervalTime = getInt(sharedPref, KEY_INT_TIME, 10);
		sIntervalDist = getInt(sharedPref, KEY_INT_DIST, 10);
		sTriggerSpeed = getInt(sharedPref, KEY_TRIG_SPD, 20);
		sValidAccuracy = (float)getInt(sharedPref, KEY_VLD_ACCU, 100);
		sVin = sharedPref.getString(KEY_VIN, "");
		sIntervalUpload = getInt(sharedPref, KEY_INT_UPLD, 30);
		sContinueLogTime = getInt(sharedPref, KEY_CONT_LOG, 30);
		
		
		slog("VIN : " + sVin);
		slog("Interval Time : " + sIntervalTime);
		slog("Interval Distance : " + sIntervalDist);
		slog("Trigger Speed : " + sTriggerSpeed);
		slog("Valid Accuracy : " + sValidAccuracy);
		slog("Upload interval : " + sIntervalUpload);
		slog("Continue log time : " + sContinueLogTime);
		
		
	}
	
	@SuppressWarnings("deprecation")
	private void updateSummary() {
		findPreference(KEY_VIN).setSummary(sVin);
		
		findPreference(KEY_INT_TIME).setSummary(
				String.format("%d초 마다 기록", sIntervalTime)
				);
		
		findPreference(KEY_INT_DIST).setSummary(
				String.format("%dm 마다 기록", sIntervalDist)
				);
		
		findPreference(KEY_TRIG_SPD).setSummary(
				String.format("%dkm/h가 넘으면 기록 시작", sTriggerSpeed)
				);
		
		findPreference(KEY_VLD_ACCU).setSummary(
				String.format("정확도 %.1f 미만인 경우에만 유효한 데이터로 간주", sValidAccuracy)
				);
		
		findPreference(KEY_INT_UPLD).setSummary(
				String.format("%d분 주기로 업로드", sIntervalUpload)
				);

		findPreference(KEY_CONT_LOG).setSummary(
				String.format("로깅 종료 후 %d분 이내 재시작시 로그 연결", sContinueLogTime)
		);
		
		findPreference(KEY_SERV_HOST).setSummary(Definition.getServerHost());
	}
	
	private static int getInt(SharedPreferences sharedPref, String key, int def) {
		try {
			String val = sharedPref.getString(key, String.valueOf(def));
			int ival = Integer.parseInt(val, 10);
			
			return ival;
		}catch(NumberFormatException e) {
			return def;
		}
	}
			
			

	public String getTag() {
		return "Setting";
	}

	@Override
	public void log(String log) {
		Log.d(getTag(), log);
	}
	
	public static void slog(String log) {
		Log.d("Setting", log);
	}

}
