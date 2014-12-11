package hu.advancedweb.androidbackupdbtest.app;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.os.ParcelFileDescriptor;
import com.google.common.base.Stopwatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by sashee on 12/8/14.
 */
public class MyBackupAgent extends BackupAgent{

	private static final String DATABASE_KEY="database";


	public static String getLogMessage(String text){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date()) + ":" + text;
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		SharedPreferences sp = getSharedPreferences("pref", Context.MODE_PRIVATE);
		HashSet<String> log = new HashSet<String>(sp.getStringSet(MainActivity.LOGS, new HashSet<String>()));
		SharedPreferences.Editor edit = sp.edit();
		try {
			Stopwatch sw=Stopwatch.createStarted();
			MySqlLiteHelper dbHelper=new MySqlLiteHelper(this);
			log.add(getLogMessage("Backup called"));

			ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
			new BackupHelper(new GZIPOutputStream(bufStream)).writeBackup(dbHelper.getReadableDatabase());

			byte[] buffer = bufStream.toByteArray();
			int len = buffer.length;

			data.writeEntityHeader(DATABASE_KEY, len);
			data.writeEntityData(buffer, len);
			{
				long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
				String hash = new DatabaseHelper(dbHelper.getReadableDatabase()).databaseHash();
				log.add(getLogMessage("Backup finished in: " + elapsed + " ms. The backed up database hash is: " + hash));
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.add(getLogMessage("Error occured when backing up"));
		} finally {
			edit.putStringSet(MainActivity.LOGS,log);
			edit.apply();
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		SharedPreferences sp = getSharedPreferences("pref", Context.MODE_PRIVATE);
		HashSet<String> log = new HashSet<String>(sp.getStringSet(MainActivity.LOGS, new HashSet<String>()));
		SharedPreferences.Editor edit = sp.edit();
		try {
			MySqlLiteHelper dbHelper=new MySqlLiteHelper(this);
			Stopwatch sw=Stopwatch.createStarted();
			while(data.readNextHeader()){
				String key = data.getKey();
				int dataSize = data.getDataSize();

				if (DATABASE_KEY.equals(key)) {
					byte[] dataBuf = new byte[dataSize];
					data.readEntityData(dataBuf, 0, dataSize);
					ByteArrayInputStream baStream = new ByteArrayInputStream(dataBuf);

					dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 0, MySqlLiteHelper.DATABASE_VERSION);
					new RestoreHelper(new GZIPInputStream(baStream)).readBackup(dbHelper.getWritableDatabase());

				}
			}
			{
				long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
				String hash = new DatabaseHelper(dbHelper.getReadableDatabase()).databaseHash();
				log.add(getLogMessage("Restore finished in: " + elapsed + " ms. The restored database hash is: " + hash));
			}
		} finally {
			edit.putStringSet(MainActivity.LOGS, log);
			edit.apply();
		}

	}
}
