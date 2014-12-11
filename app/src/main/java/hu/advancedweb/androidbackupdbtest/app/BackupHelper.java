package hu.advancedweb.androidbackupdbtest.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by sashee on 12/7/14.
 */
public class BackupHelper {

	private JsonWriter writer;

	public BackupHelper(OutputStream out) throws UnsupportedEncodingException{
		this.writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
	}

	public void setWriter(JsonWriter writer){
		this.writer=writer;
	}



	private void writeStuffs(final JsonWriter writer,SQLiteDatabase db,Long locationId) throws IOException {
		writer.beginArray();

		DatabaseHelper.iterateOverCursor(db.query(MySqlLiteHelper.TABLE_STUFF, null, MySqlLiteHelper.STUFF_LOCATION_ID + "=?", ImmutableList.of(locationId + "").toArray(new String[]{}), null, null, null), new Function<Cursor, Void>() {
			@Override
			public Void apply(Cursor cursor) {
				try {
					writer.beginObject();
					writer.name("name").value(cursor.getString(cursor.getColumnIndex(MySqlLiteHelper.STUFF_COLUMN_NAME)));
					writer.endObject();
					return null;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});


		writer.endArray();
	}

	private void writeLocations(final JsonWriter writer,final SQLiteDatabase db) throws IOException{
		writer.beginArray();

		DatabaseHelper.iterateOverCursor(db.query(MySqlLiteHelper.TABLE_LOCATIONS, null, null, null, null, null, null), new Function<Cursor, Void>() {
			@Override
			public Void apply(Cursor cursor) {
				try {
					writer.beginObject();

					writer.name("name").value(cursor.getString(cursor.getColumnIndex(MySqlLiteHelper.LOCATIONS_COLUMN_NAME)));
					writer.name("stuffs");
					writeStuffs(writer, db, cursor.getLong(cursor.getColumnIndex(MySqlLiteHelper.LOCATIONS_COLUMN_ID)));

					writer.endObject();
					return null;
				}catch(IOException e){
					throw new RuntimeException(e);
				}
			}
		});

		writer.endArray();
	}

	private void writeBackup(final JsonWriter writer,final SQLiteDatabase db) throws IOException{
		writer.beginObject();
		writer.name("locations");

		writeLocations(writer,db);

		writer.endObject();
		writer.close();
	}

	public void writeBackup(final SQLiteDatabase db) throws IOException{
		writeBackup(writer,db);
	}
}
