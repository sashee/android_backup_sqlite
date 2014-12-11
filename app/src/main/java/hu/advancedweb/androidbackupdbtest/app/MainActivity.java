package hu.advancedweb.androidbackupdbtest.app;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.common.hash.Hashing;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class MainActivity extends Activity {

    public static final String LOGS="Logs";

    MySqlLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new MySqlLiteHelper(this);
    }

    private void resetAndPopulateDb(MySqlLiteHelper dbHelper,int numLocations,int numStuffsPerLocation){
        //Resets the db for testing
        dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 0, MySqlLiteHelper.DATABASE_VERSION);


        SQLiteDatabase db = dbHelper.getWritableDatabase();
        for (int i = 0; i < numLocations; i++) {
            db.beginTransaction();
            try {
                long locationId;
                {
                    ContentValues values = new ContentValues();
                    values.put(MySqlLiteHelper.LOCATIONS_COLUMN_NAME, RandomStringUtils.randomAlphabetic(10));
                    locationId = db.insert(MySqlLiteHelper.TABLE_LOCATIONS, null, values);
                }
                for (int j = 0; j < numStuffsPerLocation; j++) {
                    ContentValues values = new ContentValues();
                    values.put(MySqlLiteHelper.STUFF_COLUMN_NAME, RandomStringUtils.randomAlphabetic(10));
                    values.put(MySqlLiteHelper.STUFF_LOCATION_ID, locationId);
                    db.insert(MySqlLiteHelper.TABLE_STUFF, null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        refreshUI(null);
    }

    public void refreshUI(View v){
        SharedPreferences sp=getSharedPreferences("pref", MODE_PRIVATE);
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        long numLocations=DatabaseUtils.queryNumEntries(db,MySqlLiteHelper.TABLE_LOCATIONS);
        long numStuffs=DatabaseUtils.queryNumEntries(db,MySqlLiteHelper.TABLE_STUFF);

        ((TextView)findViewById(R.id.stats_locations_number_text)).setText(numLocations+"");
        ((TextView)findViewById(R.id.stats_stuffs_number_text)).setText(numStuffs+"");

        ((TextView)findViewById(R.id.stats_database_hash)).setText(new DatabaseHelper(db).databaseHash());

        if(numLocations==0){
            numLocations=100;
        }


        long numStuffsPerLocation=numStuffs/numLocations;
        if(numStuffsPerLocation==0){
            numStuffsPerLocation=100;
        }

        ((EditText)findViewById(R.id.locations_amount_text)).setText(numLocations+"");
        ((EditText)findViewById(R.id.stuffs_amount_text)).setText(numStuffsPerLocation+"");

        ((TextView)findViewById(R.id.log_text)).setText(Joiner.on("\n").join(FluentIterable.from(sp.getStringSet(LOGS, new HashSet<String>())).toSortedSet(Ordering.natural())));

        System.out.println(((TextView)findViewById(R.id.log_text)).getText());
    }

    public void clearLog(View v){
        getSharedPreferences("pref", MODE_PRIVATE).edit().putStringSet(LOGS,new HashSet<String>()).commit();
        Toast.makeText(MainActivity.this, "Log cleared", Toast.LENGTH_SHORT).show();
        refreshUI(null);
    }

    public void callDatachanged(View v){
        new BackupManager(this).dataChanged();
        Toast.makeText(MainActivity.this, "dataChanged() called", Toast.LENGTH_SHORT).show();
    }

    public void performTimingTests(View v){
        SharedPreferences sp = getSharedPreferences("pref", Context.MODE_PRIVATE);
        HashSet<String> log = new HashSet<String>(sp.getStringSet(MainActivity.LOGS, new HashSet<String>()));
        SharedPreferences.Editor edit = sp.edit();
        try {
            log.add(MyBackupAgent.getLogMessage("Start timing tests"));
            try {
                {
                    Stopwatch stopwatch = Stopwatch.createStarted();

                    resetAndPopulateDb(dbHelper,Integer.parseInt(((EditText)findViewById(R.id.locations_amount_text)).getText().toString()),Integer.parseInt(((EditText)findViewById(R.id.stuffs_amount_text)).getText().toString()));

                    log.add(MyBackupAgent.getLogMessage("Db population takes: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms"));
                }

                SQLiteDatabase db=dbHelper.getReadableDatabase();

                //Timing tests
                {
                    Stopwatch sw = Stopwatch.createStarted();

                    BackupHelper helper=new BackupHelper(new NullOutputStream());
                    helper.setWriter(new DummyJsonWriter());
                    helper.writeBackup(db);

                    log.add(MyBackupAgent.getLogMessage("DB read takes: " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms"));
                }

                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    Stopwatch sw = Stopwatch.createStarted();

                    new BackupHelper(baos).writeBackup(db);

                    log.add(MyBackupAgent.getLogMessage("Backup size without GZIP:" + baos.size()+" bytes"));

                    log.add(MyBackupAgent.getLogMessage("JSON write without GZIP takes: " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms"));
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                {
                    Stopwatch sw = Stopwatch.createStarted();
                    new BackupHelper(new GZIPOutputStream(baos)).writeBackup(db);

                    log.add(MyBackupAgent.getLogMessage("Backup size with GZIP:" + baos.size()+" bytes"));

                    log.add(MyBackupAgent.getLogMessage("JSON write with GZIP takes: " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms"));
                }

                {
                    SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    dbHelper.onUpgrade(writableDb, 0, MySqlLiteHelper.DATABASE_VERSION);

                    Stopwatch sw = Stopwatch.createStarted();

                    new RestoreHelper(new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()))).readBackup(writableDb);

                    log.add(MyBackupAgent.getLogMessage("Restore took: " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms"));
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
            log.add(MyBackupAgent.getLogMessage("Timing tests finished"));
        } finally {
            edit.putStringSet(MainActivity.LOGS, log);
            edit.apply();
        }

        Toast.makeText(MainActivity.this, "Timing tests finished", Toast.LENGTH_SHORT).show();
        refreshUI(null);
    }

    public void populateDb(View v){
        resetAndPopulateDb(dbHelper,Integer.parseInt(((EditText) findViewById(R.id.locations_amount_text)).getText().toString()),Integer.parseInt(((EditText)findViewById(R.id.stuffs_amount_text)).getText().toString()));
        Toast.makeText(MainActivity.this, "Db populated", Toast.LENGTH_SHORT).show();
        refreshUI(null);
    }

    public void performRestore(View v){
        BackupManager backupManager = new BackupManager(getApplicationContext());
        backupManager.requestRestore(new RestoreObserver() {
            @Override
            public void restoreStarting(int numPackages) {
                super.restoreStarting(numPackages);
            }

            @Override
            public void onUpdate(int nowBeingRestored, String currentPackage) {
                super.onUpdate(nowBeingRestored, currentPackage);
            }

            @Override
            public void restoreFinished(int error) {
                super.restoreFinished(error);
                refreshUI(null);
                Toast.makeText(MainActivity.this, "Update successful", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
