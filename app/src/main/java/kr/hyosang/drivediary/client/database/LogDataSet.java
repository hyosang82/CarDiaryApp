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
	
	public LogDataSet() {
		keyList = new ArrayList<Integer>();
		logData = null;
		timeKey = -1;
		trackSeq = -1;
	}
	
	public int getCount() {
		return (keyList == null ? 0 : keyList.size()); 
	}

}
