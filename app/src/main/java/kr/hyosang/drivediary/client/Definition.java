package kr.hyosang.drivediary.client;

public class Definition {
	public static class Event {
		public static final int LOCATION_UPDATED = 1;
		public static final int LOG_STATE_CHANGED = 2;
		public static final int LAST_LOCATION = 3;
		public static final int TERMINATE_APP = 4;
	}
	
	public static final String EXTRA_MESSENGER = "extra_messenger";
	
	public static final boolean IS_LOCAL = true;
	
	public static final String DAUM_API_KEY = "6cd27c06ff0036a8d8dc8428b194494f";
	
	public static final String SERVER_ROOT_LOCAL = "http://192.168.123.1:8888";
	public static final String SERVER_ROOT_REAL = "http://car-diary.appspot.com"; 
	//public static final String UPLOAD_LOG = "/UploadLog";			//v1
	//public static final String UPLOAD_FUEL = "/SaveFuelLog";		//v1
	public static final String UPLOAD_FUEL = "/Fuel/AddRecord";		//v2
	public static final String UPLOAD_LOG = "/DriveLog/UploadLog";	//v2
	
	public static String getServerHost() {
		return (IS_LOCAL ? SERVER_ROOT_REAL : SERVER_ROOT_REAL);
	}

}
