package kr.hyosang.drivediary.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.hyosang.drivediary.client.R;
import kr.hyosang.drivediary.client.service.GpsService;
import kr.hyosang.drivediary.client.service.IGpsService;
import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends BaseActivity {
	public static IGpsService mService = null;
	private boolean bTrackingMap = true;
	
	private LinearLayout llNotLogging;
	private LinearLayout llLogging;
	private Button btnSetting;
	private Button btnStartLog;
	private Button btnStopLog;
	private Button btnStopService;
	private Button btnFuelNow;
	private CheckBox chkTracking;
	private EditText txtLatitude;
	private EditText txtLongitude;
	private EditText txtAltitude;
	private EditText txtSpeed;
	private EditText txtTimestamp;
	private EditText txtAccuracy;
	private EditText txtProvider;
	private EditText txtBearing;
	private EditText txtRecCount;
	private LinearLayout llInfoArea;
	private Button btnHideInfo;
	
	private MapView mMapView;
	private MapPolyline mTrackline;
	
	private SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.main);
		
		llNotLogging = (LinearLayout)findViewById(R.id.ll_notlogging);
		llLogging = (LinearLayout)findViewById(R.id.ll_logging);
		llInfoArea = (LinearLayout)findViewById(R.id.ll_info);
		txtLatitude = (EditText)findViewById(R.id.txtLatitude);
		txtLongitude = (EditText)findViewById(R.id.txtLongitude);
		txtAltitude = (EditText)findViewById(R.id.txtAltitude);
		txtSpeed = (EditText)findViewById(R.id.txtSpeed);
		txtTimestamp = (EditText)findViewById(R.id.txtTimestamp);
		txtAccuracy = (EditText)findViewById(R.id.txtAccuracy);
		txtProvider = (EditText)findViewById(R.id.txtProvider);
		txtBearing = (EditText)findViewById(R.id.txtBearing);
		txtRecCount = (EditText)findViewById(R.id.txtRecCount);
		
		btnSetting = (Button)findViewById(R.id.btn_settings);
		btnHideInfo = (Button)findViewById(R.id.btn_hideinfo);
		btnFuelNow = (Button)findViewById(R.id.btn_addfuel);
		btnStartLog = (Button)findViewById(R.id.btn_startlog);
		btnStopLog = (Button)findViewById(R.id.btn_stoplog);
		btnStopService = (Button)findViewById(R.id.btn_stopservice);
		
		chkTracking = (CheckBox)findViewById(R.id.chk_track_map);
		
		btnSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(i);
            }
		});
		
		btnStartLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					if(mService == null) {
						log("Warning : mService is null");
					}else {
						mService.startLog();
					}
				}catch(RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		btnFuelNow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Location loc = null;
				String posData = null;
				
				try {
					loc = mService.getLastPosition();
				}catch(RemoteException e) {
				}
				
				if(loc != null) {
					posData = String.format("%f|%f", loc.getLatitude(), loc.getLongitude());
				}else {
					posData = "";
				}
				
				FuelDialog dlg = new FuelDialog(MainActivity.this, posData);
				dlg.show();
			}
		});
		btnStopLog.setOnClickListener(mStopButton);
		btnStopService.setOnClickListener(mStopButton);
		
		btnHideInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (llInfoArea.getVisibility() == View.VISIBLE) {
					llInfoArea.setVisibility(View.GONE);
					btnHideInfo.setText(R.string.show_info);
				} else {
					llInfoArea.setVisibility(View.VISIBLE);
					btnHideInfo.setText(R.string.hide_info);
				}
			}
		});
		
		chkTracking.setChecked(bTrackingMap);
		chkTracking.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				bTrackingMap = isChecked;
			}
		});
		
		initMap();

		/*
		mTrackline = new MapPolyline();
		mTrackline.setLineColor(Color.argb(0xff, 0xee, 0x00, 0x00));
		mMapView.addPolyline(mTrackline);
		
		mMapView.moveCamera(CameraUpdateFactory.newMapPoint(MapPoint.mapPointWithGeoCoord(38.22354, 127.40204)));

		 */

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (requestPermission()) {
				startService();
			}
		}else {
			startService();
		}
		
		log("App started");
	}
	
	private void initMap() {
		/*
	    mMapView = new MapView(this);
	    mMapView.setDaumMapApiKey(Definition.DAUM_API_KEY);


	    ViewGroup container = (ViewGroup) findViewById(R.id.map_layout);
	    container.addView(mMapView);
		 */

	}

	private boolean requestPermission() {
		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
			return false;
		}else {
			return true;
		}
	}

	private void startService() {
		Intent i = new Intent();
		i.setClass(MainActivity.this, GpsService.class);

		boolean res = getApplicationContext().bindService(i, mServiceConn, BIND_AUTO_CREATE);
	}
	
	private void setButton() {
		if(isLogging()) {
			llLogging.setVisibility(View.VISIBLE);
			llNotLogging.setVisibility(View.GONE);
		}else {
			llLogging.setVisibility(View.GONE);
			llNotLogging.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if(requestCode == 1) {
			startService();
		}
	}

	private OnClickListener mStopButton = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(mService != null) {
				try {
					if(v.getId() == R.id.btn_stoplog) {
						mService.stopLog();
					}else if(v.getId() == R.id.btn_stopservice) {
					    mService.stopLog();
					    finish();
					}
				}catch(RemoteException e) {
				}
			}
		}
	};
	
	@Override
	protected void onDestroy() {
	    try {
	        unbindService(mServiceConn);
	    }catch(IllegalArgumentException e) {
	    }
		
		if(!isLogging()) {
			Intent i = new Intent(MainActivity.this, GpsService.class);
			stopService(i);
			
			log("stopService called");
		}
		
		
		super.onDestroy();
	}
	
	private boolean isLogging() {
		try {
			if(mService != null && mService.isLogging()) {
				return true;
			}
		}catch(RemoteException e) {
			mService = null;
		}
		
		return false;
	}
	
	private void displayInfo(Location loc) {
		float speedms = loc.getSpeed();
		float speedkmh = speedms * 3.6f;
		
		String sats = "0";
		
		Bundle extra = loc.getExtras();
		if(extra != null) {
			sats = extra.getString("satellites");
		}
		
		txtLatitude.setText(String.valueOf(loc.getLatitude()));
		txtLongitude.setText(String.valueOf(loc.getLongitude()));
		txtAltitude.setText(String.format("%.1f", loc.getAltitude()));
		txtSpeed.setText(String.format("%.2f", speedkmh));
		txtTimestamp.setText(mDateFormatter.format(new Date(loc.getTime())));
		txtAccuracy.setText(String.valueOf(loc.getAccuracy()));
		txtProvider.setText(String.format("%s (%s)", loc.getProvider(), sats));
		txtBearing.setText(String.valueOf(loc.getBearing()));
		
		try {
			txtRecCount.setText(
					String.format("%d 건", 
							mService.getRecordCount(mService.getCurrentTimeKey())
					)
			);
		}catch(RemoteException e) {
			txtRecCount.setText("ERROR");
		}

	}
			
	
	

	@Override
	public String getTag() {
		return "MainActivity";
	}
	
	private Messenger mServiceListener = new Messenger(new Handler() {
		public void handleMessage(Message msg) {
			if(msg.arg1 == Definition.Event.LOCATION_UPDATED) {
				Location loc = (Location)msg.obj;

				mTrackline.addPoint(MapPoint.mapPointWithGeoCoord(loc.getLatitude(), loc.getLongitude()));
				
				if(bTrackingMap) {
                    mMapView.moveCamera(CameraUpdateFactory.newMapPoint(MapPoint.mapPointWithGeoCoord(loc.getLatitude(), loc.getLongitude())));
                }
				
				displayInfo(loc);
				
			}else if(msg.arg1 == Definition.Event.LOG_STATE_CHANGED) {
				setButton();
			}else if(msg.arg1 == Definition.Event.LAST_LOCATION) {
				if(msg.obj != null) {
					Location loc = (Location)msg.obj;

					if(bTrackingMap) {
					    mMapView.moveCamera(CameraUpdateFactory.newMapPoint(MapPoint.mapPointWithGeoCoord(loc.getLatitude(), loc.getLongitude())));
					}
				}
			}else if(msg.arg1 == Definition.Event.TERMINATE_APP) {
			    //앱 종료
			    
			}
		};
	});
	
	private ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName cname, IBinder binder) {
			log("Service Connected " + cname.getPackageName());
			
			mService = IGpsService.Stub.asInterface(binder);
			
			setButton();

			//지속적으로 켬
			Intent i = new Intent(MainActivity.this, GpsService.class);
			i.putExtra(Definition.EXTRA_MESSENGER, mServiceListener);
			startService(i);
		}

		@Override
		public void onServiceDisconnected(ComponentName cname) {
			log("Service disconnected " + cname.getPackageName());
			
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if(item.getItemId() == R.id.action_settings) {
		}
		return super.onMenuItemSelected(featureId, item);
	}
}
