package kr.hyosang.drivediary.client.network;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import kr.hyosang.drivediary.client.Definition;
import kr.hyosang.drivediary.client.SettingActivity;
import kr.hyosang.drivediary.client.database.DbHelper;
import kr.hyosang.drivediary.client.database.FuelRecord;
import kr.hyosang.drivediary.client.database.LogDataSet;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

public class UploadThread extends Thread {
    private static final int MSG_SHOW_TOAST = 0x01;
    
    private static final int MINIMALUPLOAD_INTERVAL = 20 * 60 * 1000;
    
    private Context mContext = null;
    
    private static boolean mbRunning = false;
    
    private static long mLastUploadTime = 0;
    
    
    public UploadThread(Context context) {
        mContext = context;
    }
    
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_SHOW_TOAST:
                Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    @Override
    public void run() {
        synchronized(this) {
            if(mbRunning) return;
            
            mbRunning = true;
        }
        
        DbHelper mDb = new DbHelper(mContext);
        
        //주유기록 먼저 업로드함
        while(true) {
            FuelRecord fuel = mDb.getUploadFuelRecord();
            if(fuel == null) break;
            
            showToast("주유기록 " + fuel.seq + " 업로드");
            
            HashMap<String, String> uploadData = new HashMap<String, String>();
            uploadData.put("inputVehicle", SettingActivity.sVin);
            uploadData.put("inputOdo", String.valueOf(fuel.odo));
            uploadData.put("inputPrice", String.valueOf(fuel.priceUnit));
            uploadData.put("inputTotalPrice", String.valueOf(fuel.priceTotal));
            uploadData.put("inputVolume", String.valueOf(fuel.liter));
            uploadData.put("inputIsFull", fuel.isFull ? "Y" : "N");
            uploadData.put("inputDate", String.valueOf(fuel.timestamp));
            uploadData.put("inputStation", fuel.location);
            
            int res = NetworkManager.getInstance().sendData(Definition.UPLOAD_FUEL, uploadData);
            
            showToast("주유기록 업로드 Result = " + res);
            
            if(res == 200) {
                //성공
                mDb.removeFuelRecord(fuel.seq);
            }else {
                //실패. 중지함.
                break;
            }
        }
        
        
        //로그기록 업로드
        if((System.currentTimeMillis() - mLastUploadTime) > MINIMALUPLOAD_INTERVAL) {
            //로그 조각화 방지
            while(true) {
                LogDataSet dataset = mDb.getUploadData();
                if(dataset != null && dataset.getCount() > 0) {
                    showToast("로그 업로드 : " + dataset.getCount() + "건");
                    
                    int res = NetworkManager.getInstance().uploadLog(dataset);
                    
                    if(res == 200) {
                        //성공
                        showToast("업로드 성공, 데이터 삭제");
                        
                        mDb.deleteRows(dataset.keyList);
                        
                        mLastUploadTime = System.currentTimeMillis();
                    }else {
                        //실패
                        showToast("업로드 실패 = " + res);
                        break;
                    }
                }else {
                    showToast("업로드할 데이터 없음");
                    break;
                }
            }
        }
        
        mbRunning = false;
        
        
    }
    
    private void showToast(String msg) {
        Message.obtain(mHandler, MSG_SHOW_TOAST, msg).sendToTarget();
    }

}
