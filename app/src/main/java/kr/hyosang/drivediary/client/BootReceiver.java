package kr.hyosang.drivediary.client;

import kr.hyosang.drivediary.client.service.GpsService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, GpsService.class));
            }else {
                context.startService(new Intent(context, GpsService.class));
            }
        }
    }

}
