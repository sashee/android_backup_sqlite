package hu.advancedweb.androidbackupdbtest.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by sashee on 12/5/14.
 */
public class MySqlLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_LOCATIONS = "location";
	public static final String LOCATIONS_COLUMN_ID = "_id";
	public static final String LOCATIONS_COLUMN_NAME = "name";

	public static final String TABLE_STUFF = "stuff";
	public static final String STUFF_COLUMN_ID = "_id";
	public static final String STUFF_COLUMN_NAME = "name";
	public static final String STUFF_LOCATION_ID = "location";

	private static final String DATABASE_NAME = "test.db";
	public static final int DATABASE_VERSION = 22;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_LOCATIONS + "(" + LOCATIONS_COLUMN_ID
			+ " integer primary key autoincrement, " + LOCATIONS_COLUMN_NAME
			+ " text not null);";

	private static final String DATABASE_CREATE2 =
			"create table " + TABLE_STUFF + "(" + STUFF_COLUMN_ID + ""
					+ " integer primary key autoincrement, " + STUFF_COLUMN_NAME + ""
					+ " text not null, " + STUFF_LOCATION_ID + ""
					+ " integer not null, " + ""
					+ "foreign key(" + STUFF_LOCATION_ID + ") references " + TABLE_LOCATIONS + ""
					+ "(" + LOCATIONS_COLUMN_ID + "))";

	public MySqlLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(DATABASE_CREATE2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySqlLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUFF);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
		onCreate(db);
	}

}