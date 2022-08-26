package kr.hyosang.drivediary.client.service;

import kr.hyosang.drivediary.client.BaseUtil;
import kr.hyosang.drivediary.client.BuildConfig;
import kr.hyosang.drivediary.client.Definition;
import kr.hyosang.drivediary.client.MainActivity;
import kr.hyosang.drivediary.client.R;
import kr.hyosang.drivediary.client.SettingActivity;
import kr.hyosang.drivediary.client.database.DbHelper;
import kr.hyosang.drivediary.client.network.UploadThread;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class GpsService extends Service implements BaseUtil {
    
    @Deprecated
	private LocationClient mLocationClient;
    private LocationManager mLocationManager;
	private Messenger mViewListener;
	private boolean mIsLogging = false;
	private DbHelper mDb = null;

	private WindowManager windowManager;
	
	private long mTrackTimestamp = 0;
	private int mTrackSeq = 0;
	private long mTrackTimeKey = 0;
	private boolean bZeroLogged = false;
	
	private int mUploadTick = 0;
	private long mLastLoggedTime = 0;
	private Location mLastLocation = null;

	private ImageView ivOverlayIcon = null;
	
	NotificationManager mNotiManager;
	
	private int mStopTickTimer = 30;	//30min
	
	private enum NotificationType {
	    SERVICE_STARTED,
	    GPS_SEARCHING,
	    GPS_RECEIVED,
	    DATA_UPLOADING
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mService.asBinder();
	}

	@Override
	public String getTag() {
		return "GpsService";
	}

	@Override
	public void log(String log) {
		Log.d(getTag(), log);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		log("onCreate");

		SettingActivity.loadPreferences(this);
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mTickListener, filter);
		
		mNotiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		updateNotification(NotificationType.SERVICE_STARTED);

		showOverlay();
		
		log("Service started");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");

		if(intent != null && intent.getExtras() != null) {
			Object obj = intent.getExtras().get(Definition.EXTRA_MESSENGER);
			if(obj != null && obj instanceof Messenger) {
				mViewListener = (Messenger)obj;
				log("Listener Registered");
			}
		}
		
		//초기화가 되어있는 경우 재 생성 하지 않음.
		if(mLocationManager == null) {
		    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    
		    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		            (long) SettingActivity.sIntervalTime,
		            (float) SettingActivity.sIntervalDist,
		            mLocationListener2);
		    
		    updateNotification(NotificationType.GPS_SEARCHING);
		}
		
		if(mDb == null) {
			mDb = new DbHelper(this);
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mTickListener);
		
		if(mLocationManager != null) {
		    mLocationManager.removeUpdates(mLocationListener2);
		}
		
		mNotiManager.cancelAll();
		
		log("onDestroy");
	}

	private void showOverlay() {
		int layoutFlag;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
		}else {
			layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
		}

		int size = (int)(40f * getResources().getDisplayMetrics().density);

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				size, size, layoutFlag,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT
		);
		lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
		lp.alpha = 0.5f;

		View view = LayoutInflater.from(this).inflate(R.layout.overlay_view, null);
		ivOverlayIcon = (ImageView) view.findViewById(R.id.ic_overlay_status);
		windowManager.addView(view, lp);
	}
	
	public void startLog() {
		log("start log..");

		//최종 로그 저장 후 일정시간 미 경과시 이어서 저장
        long lastTime = mDb.getLastInsertedLogTime();
        int contTime = SettingActivity.sContinueLogTime * 60 * 1000;
        mTrackTimestamp = mTrackTimeKey = mTrackSeq = 0;
        if((System.currentTimeMillis() - lastTime) < contTime) {
            mTrackTimestamp = mTrackTimeKey = mDb.getLastTimeKey();
            mTrackSeq = mDb.getLastestSeq();
        }

        if((mTrackSeq == 0) || (mTrackTimeKey == 0)){
            mTrackTimestamp = mTrackTimeKey = System.currentTimeMillis();
            mTrackSeq = (int) mDb.insertNewTrack("New Track", mTrackTimestamp);
        }
		
		bZeroLogged = false;
		
		setLogging(true);
	}
	
	public void stopLog() {
		setLogging(false);
	}
	
	private synchronized void insertLog(Location loc) {
		mDb.insertLocation(mTrackSeq, mTrackTimeKey, loc);
		
		mLastLoggedTime = System.currentTimeMillis();
	}
	
	private synchronized void setLogging(boolean bLog) {
		mIsLogging = bLog;
		
		sendMessage(Definition.Event.LOG_STATE_CHANGED, null);
		
	}
	
	private long getRecordCount(long tkey) {
		return mDb.getRecordCount(tkey);
	}
	
	private void sendMessage(int arg1, Object obj) {
		if(mViewListener != null) {
			try {
				Message msg = Message.obtain();
				msg.arg1 = arg1;
				msg.obj = obj;
				
				mViewListener.send(msg);
			}catch(RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private BroadcastReceiver mTickListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    String action = intent.getAction();
		    
		    if(Intent.ACTION_TIME_TICK.equals(action)) {
    			mUploadTick++;
    			mStopTickTimer--;
    			
    			if(mStopTickTimer < 0) {
    				//기록 중지 및 업로드
    				stopLog();
    				sleep();
    			}else if(mUploadTick > SettingActivity.sIntervalUpload) {
    				//업로드 처리
    				mUploadTick = mUploadTick % SettingActivity.sIntervalUpload;
    			}
		    }else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                
                log("Network state = " + netInfo.getState());
                
                if(netInfo.getState() == State.CONNECTED) {
                    //업로드 스레드 실행
                    (new UploadThread(GpsService.this)).start();
                }
		    }
		}
	};
	
	private ConnectionCallbacks mLocationCallback = new ConnectionCallbacks() {

		@Override
		public void onConnected(Bundle connectionHint) {
			log("LocationService Connected");
			
			updateNotification(NotificationType.GPS_RECEIVED);
			
			//최종 위치 send.
			Location lastLoc = mLocationClient.getLastLocation();
			sendMessage(Definition.Event.LAST_LOCATION, lastLoc);
			
			LocationRequest locReq = LocationRequest.create();
			locReq.setFastestInterval(1000);
			locReq.setInterval(1000);
			locReq.setSmallestDisplacement(SettingActivity.sIntervalDist);
			locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			mLocationClient.requestLocationUpdates(locReq, mLocationListener);
		}

		@Override
		public void onDisconnected() {
			log("LocationService Disconnected");
		}
	};
	
	private void requestUpload() {
	    (new UploadThread(this)).start();
	}
	
	private void sleep() {
	    //앱 종료함
	    sendMessage(Definition.Event.TERMINATE_APP, null);
	}
	
	private void updateNotification(NotificationType type) {
	    updateNotification(type, null);
	}
	
	private void updateNotification(NotificationType type, String customContent) {
	    int icon = R.drawable.noun_72;
	    String title = "DriveDiary Service";
	    String content = "...";
		int ovIcon = R.drawable.ic_x;
	    
	    switch(type) {
	    case SERVICE_STARTED:
	        icon = R.drawable.noun_72;
	        content = "Service Started";
			ovIcon = R.drawable.ic_x;
	        break;
	        
	    case GPS_SEARCHING:
	        icon = R.drawable.noun_72;
	        content = "Searching GPS...";
			ovIcon = R.drawable.ic_x;
	        break;
	        
	    case GPS_RECEIVED:
	        icon = R.drawable.noun_72;
	        content = "Tracking GPS...";
			ovIcon = R.drawable.ic_ok;
	        break;
	        
	    case DATA_UPLOADING:
	        icon = R.drawable.noun_72;
	        content = "Uploading data...";
			ovIcon = R.drawable.ic_upload;
	        break;
	    }
	    
	    if(customContent != null) {
	        content = customContent;
	    }
	        
	    PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

	    Notification.Builder builder;
	    Notification noti;
	    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	    	NotificationChannel channel = new NotificationChannel("CarDiaryNotiChannel", "CarDiary", NotificationManager.IMPORTANCE_DEFAULT);
	    	mNotiManager.createNotificationChannel(channel);

	    	builder = new Notification.Builder(this, "CarDiaryNotiChannel");
		}else {
	    	builder = new Notification.Builder(this);
		}

	    noti = builder
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
				.build();

	    startForeground(1, noti);

		final int ovIconf = ovIcon;
		(new Handler(Looper.getMainLooper())).post(new Runnable() {
			@Override
			public void run() {
				ivOverlayIcon.setImageResource(ovIconf);
			}
		});
	}
	
	private android.location.LocationListener mLocationListener2 = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            float speedKm = location.getSpeed() * 3.6f;
            
            if(mIsLogging) {
                if(location.getAccuracy() < SettingActivity.sValidAccuracy) {
                    if(location.hasBearing()) {
                        //방향정보 갖고있나?
                        if(location.hasSpeed()) {
                            //속도정보 있나?
                            if((System.currentTimeMillis() - mLastLoggedTime) > (SettingActivity.sIntervalTime * 1000)) {
                                //로깅 인터벌 시간 경과했나?
                                insertLog(location);
                                bZeroLogged = false;
                            }
                        }
                    }
                
                    if(!bZeroLogged && location.getSpeed() == 0) {
                        //속도0 기록되었나?
                        insertLog(location);
                        bZeroLogged = true;
                    }
                    
                    //gps로그가 튀는 현상이 있음. 5km/h이상일 경우에만 지속시킴.
                    if(speedKm > 5) {
                        mStopTickTimer = 30;
                    }
                }
            }else {
                //기록 개시 속도에 도달했는지 확인
                if(speedKm > SettingActivity.sTriggerSpeed) {
                    //기록 개시
                    startLog();
                    insertLog(location);
                }
            }
            
            mLastLocation = location;
            sendMessage(Definition.Event.LOCATION_UPDATED, location);            
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String st = "GPS Status changed : ";
            
            switch(status) {
            case LocationProvider.OUT_OF_SERVICE:
                st += "OUT OF SERVICE";
                break;
                
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                st += "TEMPORARILY_UNAVAILABLE";
                break;
                
            case LocationProvider.AVAILABLE:
                st += "AVAILABLE";
                break;
            }
            
            
            updateNotification(NotificationType.GPS_RECEIVED, st);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            
        }
	};
	
	
	@Deprecated
	private LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location loc) {
			float speedKm = loc.getSpeed() * 3.6f;
			
			if(mIsLogging) {
				if(loc.getAccuracy() < SettingActivity.sValidAccuracy) {
					if(loc.hasBearing()) {
						//방향정보 갖고있나?
						if(loc.hasSpeed()) {
							//속도정보 있나?
							if((System.currentTimeMillis() - mLastLoggedTime) > (SettingActivity.sIntervalTime * 1000)) {
								//로깅 인터벌 시간 경과했나?
								insertLog(loc);
								bZeroLogged = false;
							}
						}
					}
				
					if(!bZeroLogged && loc.getSpeed() == 0) {
						//속도0 기록되었나?
						insertLog(loc);
						bZeroLogged = true;
					}
					
					//gps로그가 튀는 현상이 있음. 5km/h이상일 경우에만 지속시킴.
					if(speedKm > 5) {
						mStopTickTimer = 30;
					}
				}
			}else {
				//기록 개시 속도에 도달했는지 확인
				if(speedKm > SettingActivity.sTriggerSpeed) {
					//기록 개시
					startLog();
					insertLog(loc);
				}
			}
			
			mLastLocation = loc;
			sendMessage(Definition.Event.LOCATION_UPDATED, loc);
		}
	};
	
	private OnConnectionFailedListener mLocationFailed = new OnConnectionFailedListener() {

		@Override
		public void onConnectionFailed(ConnectionResult arg0) {
			log("LocationService Connection failed : " + arg0.getErrorCode());			
		}
	};
	
	private IGpsService mService = new IGpsService.Stub() {

		@Override
		public void stopLog() throws RemoteException {
			GpsService.this.stopLog();			
		}

		@Override
		public boolean isLogging() throws RemoteException {
			return GpsService.this.mIsLogging;
		}

		@Override
		public void startLog() throws RemoteException {
			GpsService.this.startLog();
		}

		@Override
		public long getRecordCount(long tKey) throws RemoteException {
			return GpsService.this.getRecordCount(tKey);
		}

		@Override
		public int getCurrentTrackSeq() throws RemoteException {
			return GpsService.this.mTrackSeq;
		}
		
		@Override
		public long getCurrentTimeKey() throws RemoteException {
			return GpsService.this.mTrackTimeKey;
		}

		@Override
		public void requestUpload() throws RemoteException {
			GpsService.this.requestUpload();
		}

		@Override
		public Location getLastPosition() throws RemoteException {
			return GpsService.this.mLastLocation;
		}

	};
}
