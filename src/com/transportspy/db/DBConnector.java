package com.transportspy.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBConnector {
	// Data
	private static final String DATABASE_NAME = "transport_spy.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "Options";

	// Column names
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_ZOOM = "Zoom";
	private static final String COLUMN_TRANSPORT_LIST = "TransportList";

	// Column number
	//private static final int NUM_COLUMN_ID = 0;
	private static final int NUM_COLUMN_ZOOM = 1;
	private static final int NUM_COLUMN_TRANSPORT_LIST = 2;

	private SQLiteDatabase mDataBase;

	public DBConnector(Context context) {
		OpenHelper mOpenHelper = new OpenHelper(context);
		mDataBase = mOpenHelper.getWritableDatabase();
	}

	public Options getOptions() {
		Cursor mCursor = mDataBase.query(TABLE_NAME, null, COLUMN_ID
				+ " = 1", null, null, null, null);
		if (!mCursor.moveToFirst()) {
			mCursor.close();
			return null;
		}
		
		Options ret = new Options(
				mCursor.getInt(NUM_COLUMN_ZOOM),
				mCursor.getString(NUM_COLUMN_TRANSPORT_LIST)
				);
		mCursor.close();
		return ret;
	}

	public void delete() {
		mDataBase.delete(TABLE_NAME, COLUMN_ID + " = 1", null);
	}

	public boolean setOptions(Options options) {
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_ID, 1);
		cv.put(COLUMN_ZOOM, options.getZoom());
		cv.put(COLUMN_TRANSPORT_LIST, options.getTransportList());

		if (getOptions() != null) {
			return updateOptions(cv) != -1;
		} else {
			return insertOptions(cv) != -1;
		}
	}

	private int insertOptions(ContentValues options) {
		return (int) mDataBase.insert(TABLE_NAME, null, options);
	}

	private int updateOptions(ContentValues options) {
		return mDataBase.update(TABLE_NAME, options, COLUMN_ID + " = 1",
				null);
	}
	
	private class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String query = "CREATE TABLE " + TABLE_NAME + " (" 
					+ COLUMN_ID + " INTEGER PRIMARY KEY, " 
					+ COLUMN_ZOOM + " INTEGER, "
					+ COLUMN_TRANSPORT_LIST + " TEXT)";
			db.execSQL(query);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
}
