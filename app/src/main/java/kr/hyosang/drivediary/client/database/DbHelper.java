package kr.hyosang.drivediary.client.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import kr.hyosang.drivediary.client.BaseUtil;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper implements BaseUtil {
	private static final String TABLE_TRACK = "tracklist";
	private static final String TABLE_LOG = "tracklog";
	private static final String COL_SEQ = "seq";
	private static final String COL_TSEQ = "trackseq";
	private static final String COL_TKEY = "time_key";
	private static final String COL_LATI = "latitude";
	private static final String COL_LONGI = "longitude";
	private static final String COL_ALTI = "altitude";
	private static final String COL_SPEED = "speed";
	private static final String COL_TIME = "timestamp";
	private static final String COL_TITLE = "title";
	
	private static Object mLock = null;
	
	public DbHelper(Context context) {
		super(context, "database.db", null, 1);
		
		if(mLock == null) {
			mLock = new Object();
		}
	}

	public DbHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		log("onCreate");
		
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE IF NOT EXISTS tracklog ")
		.append(" ( ")
		.append(" seq INTEGER PRIMARY KEY AUTOINCREMENT, ")
		.append(" trackseq INTEGER NOT NULL, ")
		.append(" time_key NUMERIC, ")
		.append(" latitude REAL(2,8), ")
		.append(" longitude REAL(3,7), ")
		.append(" altitude REAL, ")
		.append(" speed REAL, ")
		.append(" timestamp NUMERIC ")
		.append(" ) ");
		
		db.execSQL(sb.toString());

		sb = new StringBuffer();
		sb.append("CREATE TABLE IF NOT EXISTS tracklist ")
		.append(" ( ")
		.append(" seq INTEGER PRIMARY KEY AUTOINCREMENT, ")
		.append(" title TEXT, ")
		.append(" timestamp NUMERIC ")
		.append(" ) ");
		db.execSQL(sb.toString());
		
		sb = new StringBuffer();
		sb.append("CREATE TABLE IF NOT EXISTS fuellog ")
		.append(" ( ")
		.append(" seq INTEGER PRIMARY KEY AUTOINCREMENT, ")
		.append(" location TEXT, ")
		.append(" odo INTEGER, ")
		.append(" price_unit INTEGER, ")
		.append(" price_total INTEGER, ")
		.append(" liter REAL(3,2), ")
		.append(" is_full CHAR(1) DEFAULT 'N', ")
		.append(" timestamp NUMERIC ")
		.append(" ) ");
		db.execSQL(sb.toString());
		
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

	}
	
	public FuelRecord getUploadFuelRecord() {
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query("fuellog",
	            new String[]{"seq", "location", "odo", "price_unit", "price_total", "liter", "is_full", "timestamp"},
	            null, null,
	            null, null,
	            "seq ASC"
	           );
	    
	    FuelRecord record = null;
	    
	    if(c.getCount() > 0) {
	        c.moveToNext();
	        
	        record = new FuelRecord();
	        record.seq = c.getInt(c.getColumnIndex("seq"));
	        record.location = c.getString(c.getColumnIndex("location"));
	        record.odo = c.getInt(c.getColumnIndex("odo"));
	        record.priceUnit = c.getInt(c.getColumnIndex("price_unit"));
	        record.priceTotal = c.getInt(c.getColumnIndex("price_total"));
	        record.liter = c.getDouble(c.getColumnIndex("liter"));
	        record.isFull = "Y".equals(c.getString(c.getColumnIndex("is_full")));
	        record.timestamp = c.getLong(c.getColumnIndex("timestamp"));
	    }else {
	    }
	    
	    c.close();
	    
	    db.releaseReference();
	    
	    return record;
	}
	
	public void removeFuelRecord(int seq) {
	    SQLiteDatabase db = getWritableDatabase();
	    db.execSQL("DELETE FROM fuellog WHERE seq = " + seq);
	    db.releaseReference();
	}
	
	
	public long addFuelRecord(String location, int odo, int price_unit, int price_total, double liter, boolean isFull) {
        ContentValues cols = new ContentValues();
        cols.put("location", location);
        cols.put("odo", odo);
        cols.put("price_unit", price_unit);
        cols.put("price_total", price_total);
        cols.put("liter", liter);
        cols.put("is_full", isFull ? "Y" : "N");
        cols.put("timestamp", System.currentTimeMillis());
        
	    synchronized(mLock) {
	        SQLiteDatabase db = getWritableDatabase();
	        
	        long res = db.insert("fuellog", null, cols);
	        
	        db.releaseReference();
	        
	        return res;
	    }
	}
	
	public long insertNewTrack(String title, long timestamp) {
		synchronized(mLock) {
			ContentValues cols = new ContentValues();
			cols.put(COL_TITLE, title);
			cols.put(COL_TIME, timestamp);
			
			SQLiteDatabase db = getWritableDatabase();
			long ins = db.insert(TABLE_TRACK, null, cols);
			
			db.releaseReference();
			
			return ins;
		}
	}
	
	public void insertLocation(int trackSeq, long keyTime, Location loc) {
		synchronized(mLock) {
			ContentValues cols = new ContentValues();
			cols.put(COL_TSEQ, trackSeq);
			cols.put(COL_TKEY, keyTime);
			cols.put(COL_LATI, loc.getLatitude());
			cols.put(COL_LONGI, loc.getLongitude());
			cols.put(COL_ALTI, loc.getAltitude());
			cols.put(COL_SPEED, loc.getSpeed() * 3.6f);
			cols.put(COL_TIME, loc.getTime());
			
			SQLiteDatabase db = getWritableDatabase();
			db.insert(TABLE_LOG, null, cols);
			db.close();
		}
	}

	public long getLastInsertedLogTime() {
		long lasttime = 0;
		synchronized(mLock) {
			String query = "SELECT MAX(" + COL_TIME + ") FROM " + TABLE_LOG;
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.rawQuery(query, null);
			if(c.moveToNext()) {
				lasttime = c.getLong(0);
			}

			db.releaseReference();
		}

		return lasttime;
	}

    public int getLastestSeq() {
        int seq = 0;
        synchronized(mLock) {
            String query = "SELECT MAX(" + COL_SEQ + ") FROM " + TABLE_TRACK;
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(query, null);
            if (c.moveToNext()) {
                seq = c.getInt(0);
            }

            db.releaseReference();
        }

        return seq;
    }

    public long getLastTimeKey() {
        long lasttime = 0;
        synchronized(mLock) {
            String query = "SELECT MAX(" + COL_TIME + ") FROM " + TABLE_TRACK;
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(query, null);
            if(c.moveToNext()) {
                lasttime = c.getLong(0);
            }

            db.releaseReference();
        }

        return lasttime;
    }
	
	public long getRecordCount(long tkey) {
		synchronized(mLock) {
			String query = "SELECT COUNT(*) FROM " + TABLE_LOG;
			
			if(tkey > 0) {
				query += " WHERE time_key=" + tkey;
			}
			
			SQLiteDatabase db = getReadableDatabase();
			SQLiteStatement pstmt = db.compileStatement(query);
			long count = pstmt.simpleQueryForLong();
			
			pstmt.close();
			db.releaseReference();
			
			return count;
		}
	}
	
	public int deleteRows(List<Integer> list) {
		if(list != null && list.size() > 0) {
			synchronized(mLock) {
				SQLiteDatabase db = getWritableDatabase();
				SQLiteStatement stmt = db.compileStatement("DELETE FROM " + TABLE_LOG + " WHERE " + COL_SEQ + " BETWEEN ? AND ?");
				
				int count = list.size();
				int rangeMin, rangeMax;
				
				rangeMin = rangeMax = list.get(0);
				
				for(Integer val : list) {
					if(val < rangeMin) rangeMin = val;
					else if(val > rangeMax) rangeMax = val;
				}
				
				stmt.bindLong(1, rangeMin);
				stmt.bindLong(2, rangeMax);
				stmt.execute();
				
				stmt.close();
				db.releaseReference();
				
				return count;
			}
		}
		return 0;
	}
	
	public LogDataSet getUploadData() {
		synchronized(mLock) {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TABLE_LOG,
					new String[]{COL_SEQ, COL_TKEY, COL_LATI, COL_LONGI, COL_SPEED, COL_ALTI, COL_TIME},
					null, null,
					null, null,
					COL_TKEY + " ASC, " + COL_TIME + " ASC");
			
			if(c.getCount() == 0) {
				return null;
			}
			
			int iSeq = c.getColumnIndex(COL_SEQ);
			int iKTime = c.getColumnIndex(COL_TKEY);
			int iLat = c.getColumnIndex(COL_LATI);
			int iLng = c.getColumnIndex(COL_LONGI);
			int iSpd = c.getColumnIndex(COL_SPEED);
			int iAlt = c.getColumnIndex(COL_ALTI);
			int iTime = c.getColumnIndex(COL_TIME);
			
			StringBuffer sb = new StringBuffer(1024 * 1024);		// 1 Mb
			long lastKey = -1;
			LogDataSet dataset = new LogDataSet();
			while(c.moveToNext()) {
				int seq = c.getInt(iSeq);
				long tkey = c.getLong(iKTime);
				
				if(lastKey == -1) {
					//first record
					dataset.timestamp = c.getLong(iTime);
				}else {
					if(lastKey != tkey) {
						break;
					}
				}
				lastKey = tkey;
				
				sb.append(String.format("%f|%f|%s|%s|%s",
						c.getDouble(iLat),
						c.getDouble(iLng),
						c.getString(iAlt),
						c.getString(iSpd),
						c.getString(iTime))
				);
				sb.append("$");
			
				dataset.timeKey = tkey;
				dataset.keyList.add(seq);
				
				if(sb.length() > (900 * 1024)) {	//900 Kb
					break;
				}
			}
			
			dataset.logData = sb.toString();
			
			c.close();
			db.releaseReference();
			
			return dataset;
		}
		
	}
	
	
	@SuppressLint("SimpleDateFormat")
	public void selectAll() {
		synchronized(mLock) {
			SQLiteDatabase db = getWritableDatabase();
			Cursor c = db.query(TABLE_LOG,
					new String[]{COL_SEQ, COL_LATI, COL_LONGI, COL_ALTI, COL_TIME},
					null,
					null,
					null,
					null,
					COL_SEQ);
			
			while(c.moveToNext()) {
				long dt = c.getLong(c.getColumnIndex("timestamp"));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				
				log("Timestamp : " + sdf.format(new Date(dt)));
			}
			
			c.close();
			
			db.releaseReference();
		}
	}

	public void release() {
        close();
    }

	@Override
	public String getTag() {
		return "DbHelper";
	}

	@Override
	public void log(String log) {
		Log.d(getTag(), log);
	}

}
