package kr.hyosang.drivediary.client.database;

import java.util.ArrayList;
import java.util.List;

public class LogDataSet {
	public List<Integer> keyList;
	public String logData;
	@Deprecated
	public int trackSeq;
	public long timeKey;
	public long timestamp;
	public String uuid;

	public List<Item> logs = new ArrayList<>();

	
	public LogDataSet() {
		keyList = new ArrayList<Integer>();
		logData = null;
		timeKey = -1;
		trackSeq = -1;
	}
	
	public int getCount() {
		return (keyList == null ? 0 : keyList.size()); 
	}

	public static class Item {
		public double latitude;
		public double longitude;
		public float altitude;
		public float speed;
		public long time;
	}

}
