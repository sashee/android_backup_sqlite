package hu.advancedweb.androidbackupdbtest.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;

/**
 * Created by sashee on 12/8/14.
 */
public class DatabaseHelper {

	private SQLiteDatabase db;

	public DatabaseHelper(SQLiteDatabase db){
		this.db=db;
	}

	public static void iterateOverCursor(Cursor cursor,Function<Cursor,Void> function){
		if (cursor .moveToFirst()) {
			while (!cursor.isAfterLast()) {
				function.apply(cursor);
				cursor.moveToNext();
			}
		}
		cursor.close();
	}

	public String databaseHash(){
		final StringHolder current=new StringHolder();
		iterateOverCursor(db.query(MySqlLiteHelper.TABLE_LOCATIONS, null, null, null, null, null, MySqlLiteHelper.LOCATIONS_COLUMN_NAME), new Function<Cursor, Void>() {
			@Override
			public Void apply(Cursor cursor) {

				Long locationId=cursor.getLong(cursor.getColumnIndex(MySqlLiteHelper.LOCATIONS_COLUMN_ID));

				String locationName=cursor.getString(cursor.getColumnIndex(MySqlLiteHelper.LOCATIONS_COLUMN_NAME));
				current.val= Hashing.sha1().hashString(current.val+locationName, Charsets.UTF_8).toString();

				iterateOverCursor(db.query(MySqlLiteHelper.TABLE_STUFF, null, MySqlLiteHelper.STUFF_LOCATION_ID + "=?", ImmutableList.of(locationId + "").toArray(new String[]{}), null, null, MySqlLiteHelper.STUFF_COLUMN_NAME), new Function<Cursor, Void>() {
					@Override
					public Void apply(Cursor cursor) {

						String stuffName=cursor.getString(cursor.getColumnIndex(MySqlLiteHelper.STUFF_COLUMN_NAME));
						current.val= Hashing.sha1().hashString(current.val+stuffName, Charsets.UTF_8).toString();

						return null;
					}
				});

				return null;
			}
		});
		return current.val;
	}

	private class StringHolder{
		public String val="";
	}
}
