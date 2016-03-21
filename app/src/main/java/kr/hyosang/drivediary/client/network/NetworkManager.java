package kr.hyosang.drivediary.client.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import kr.hyosang.drivediary.client.BaseUtil;
import kr.hyosang.drivediary.client.Definition;
import kr.hyosang.drivediary.client.SettingActivity;
import kr.hyosang.drivediary.client.database.LogDataSet;
import android.util.Log;

public class NetworkManager implements BaseUtil {
	private static NetworkManager mInstance = null;
	
	public static NetworkManager getInstance() {
		if(mInstance == null) {
			mInstance = new NetworkManager();
		}
		
		return mInstance;
	}
	
	public int sendData(String uri, Map<String, String> postData) {
		try {
			String host = Definition.getServerHost();
			URL url = new URL(String.format("%s%s", host, uri));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setDoOutput(true);
			
			conn.setRequestMethod("POST");
			conn.addRequestProperty("VIN", SettingActivity.sVin);
			
			//make post body
			StringBuffer postString = new StringBuffer();
			Set<Entry<String, String>> entries = postData.entrySet();
			for(Entry<String, String> entry : entries) {
				postString.append(
					String.format("%s=%s&", entry.getKey(), entry.getValue()));
			}
			
			OutputStream os = conn.getOutputStream();
			os.write(postString.toString().getBytes("UTF-8"));
			os.flush();
			
			int resCode = conn.getResponseCode();
			
			return resCode;
		}catch(MalformedURLException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return -1;	
	}
	
	public synchronized int uploadLog(LogDataSet dataset) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("time_key", String.valueOf(dataset.timeKey));
		data.put("log_data", dataset.logData);
		data.put("timestamp", String.valueOf(dataset.timestamp));
		
		return sendData(Definition.UPLOAD_LOG, data);
	}

	@Override
	public String getTag() {
		return "Network";
	}

	@Override
	public void log(String log) {
		Log.d(getTag(), log);
	}

}
