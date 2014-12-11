package hu.advancedweb.androidbackupdbtest.app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.google.gson.stream.JsonReader;

import java.io.*;

/**
 * Created by sashee on 12/7/14.
 */
public class RestoreHelper {

	private final JsonReader reader;

	public RestoreHelper(InputStream in) throws UnsupportedEncodingException{
		this.reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
	}

	public void readBackup(SQLiteDatabase writableDb) throws IOException{
		readBackup(reader,writableDb);
		reader.close();
	}

	private void readBackup(JsonReader reader,SQLiteDatabase writableDb) throws IOException {
		reader.beginObject();
		reader.nextName();
		readLocationArray(reader,writableDb);
		reader.endObject();
	}

	private void readLocationArray(JsonReader reader,SQLiteDatabase writableDb) throws IOException{
		reader.beginArray();

		while(reader.hasNext()){
			readLocation(reader,writableDb);
		}

		reader.endArray();
	}

	private void readLocation(JsonReader reader,SQLiteDatabase writableDb) throws IOException{
		writableDb.beginTransaction();
		try {
			reader.beginObject();

			reader.nextName();
			String locationName=reader.nextString();

			long locationId;
			{
				ContentValues values = new ContentValues();
				values.put(MySqlLiteHelper.LOCATIONS_COLUMN_NAME, locationName);
				locationId = writableDb.insert(MySqlLiteHelper.TABLE_LOCATIONS, null, values);
			}

			reader.nextName();
			readStuffArray(reader,writableDb,locationId);

			writableDb.setTransactionSuccessful();

			reader.endObject();
		}finally{
			writableDb.endTransaction();
		}
	}

	private void readStuffArray(JsonReader reader,SQLiteDatabase writableDb,long locationId) throws IOException{
		reader.beginArray();

		while (reader.hasNext()) {
			readStuff(reader,writableDb,locationId);
		}

		reader.endArray();
	}

	private void readStuff(JsonReader reader,SQLiteDatabase writableDb,long locationId) throws IOException{
		reader.beginObject();

		reader.nextName();
		String stuffName = reader.nextString();

		{
			ContentValues values = new ContentValues();
			values.put(MySqlLiteHelper.STUFF_COLUMN_NAME, stuffName);
			values.put(MySqlLiteHelper.STUFF_LOCATION_ID, locationId);
			writableDb.insert(MySqlLiteHelper.TABLE_STUFF, null, values);
		}

		reader.endObject();
	}
}
