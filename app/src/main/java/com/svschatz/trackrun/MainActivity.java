package com.svschatz.trackrun;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    //todo verify good gps on start, alert if not good gps (and gps is enabled)
    //todo don't display Infinity or NaN on log/average/location screens
    //todo TrackRun - make step and 30sec logging a setting
    //todo TrackRun - stop should log as pause - there is not really a stop, only reset
    //todo TrackRun - maybe stop menu should be pause and add a stop
    //todo what is average display when counting miles

    protected static final String TAG = "TrackRun";

    // my members
    public static Swe sw = new Swe();
    LapFragment mLapFragment;
    AverageFragment mAverageFragment;
    LocationFragment mLocationFragment;
    boolean mButtonIsGreen = false;
    Button b;
    private SensorManager sensorManager;
    private boolean mHaveStoragePermission = false;

    // Default settings
    public static TrackRunSettings trs = new TrackRunSettings();

    // intent codes
    static final int UPDATE_SETTINGS_REQUEST = 1000;
    static final int SIGNIN_REQUIRED = 1001;

    // permission request codes
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 9090;

    // timer
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long t = System.currentTimeMillis();
            sw.tick(t);
            timerHandler.postDelayed(timerRunnable, 100);
            if (mButtonIsGreen) {
                if (sw.getLapTimeCur() >= 8.0) {
                    b.setBackgroundColor(Color.LTGRAY);
                    mButtonIsGreen = false;
                }
            }
        }
    };

    // logging timer - need a periodic log entry
    Handler logTimerHandler = new Handler();
    Runnable logTimerRunnable = new Runnable() {
        @Override
        public void run() {
            doPeriodicLogging();
            // todo would prefer this logs evey 30 seconds of elapsed time, not system time
            logTimerHandler.postDelayed(logTimerRunnable, 30000);
        }
    };

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restore preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        trs.setCountLaps(settings.getBoolean("CountLaps", true));
        trs.setEnableGps(settings.getBoolean("EnableGps", false));
        trs.setFakeEvents(settings.getBoolean("FakeEvents", false));
        trs.setLapsPerMile(settings.getString("LapsPerMile", "13.0"));
        trs.setStepsPerMile(settings.getString("StepsPerMile", "1462.0"));

        // Create an instance of GoogleAPIClient.
        doInitGoogleLocationApi();

        // Set up cache file for logging
        openLogCache();

        // Check location services permission
        doCheckLocationPermission();

        // Check permissions
        if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            mHaveStoragePermission = true;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // if app was previously running, restore state
        Map<String, String> intState = new HashMap<String, String>();
        if (mHaveStoragePermission) {
            // Verify we have access to public storage area
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                File docDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS).toString());
                File trDir = new File (docDir.getPath(), "TrackRun");
                File stateFile = new File(trDir.getPath(), "state");
                try {
                    BufferedReader br = new BufferedReader(new FileReader(stateFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Log.d(TAG, line);
                        String[] pr = line.split("[,]");
                        if (pr.length != 2) {
                            break;
                        }
                        String k = pr[0].replace("\"", "");
                        String v = pr[1].replace("\"", "");
                        intState.put(k,v);
                    }
                    br.close();
                    boolean rv = sw.setInternalState(intState);
                    if (rv) Toast.makeText(this, "Restored state from file",
                            Toast.LENGTH_LONG).show();
                }
                catch (IOException e) {
                    Log.e("TrackRun", "MainActivity.onCreate " + e.getMessage());
                }
            }
        }

        // set up step sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

        b = (Button) findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MainActivity.onClick()");
                Button b = (Button) v;
                if (sw.getState() == Swe.State.RESET) {
                    // Start pressed
                    sw.start(System.currentTimeMillis());
                    timerHandler.postDelayed(timerRunnable, 0);
                    setButton("Lap", Color.GREEN);
                    appendLogCache(sw.getStringLogRec(Swe.LogRec.START));
                    if (trs.mEnableGps) {
                        appendLogCache(sw.getStringLogRec(Swe.LogRec.GPS));
                    }
                    if (trs.mFakeEvents) {
                        startFakeTimers();
                    }
                } else if (sw.getState() == Swe.State.RUNNING) {
                    // Lap pressed
                    if (sw.getLapTimeCur() >= 8.0) {
                        sw.lap(System.currentTimeMillis());
                        setButton("Lap", Color.GREEN);
                        appendLogCache(sw.getStringLogRec(Swe.LogRec.LAP));
                    }
                } else {
                    // Resume pressed
                    sw.resume(System.currentTimeMillis());
                    timerHandler.postDelayed(timerRunnable, 0);
                    if (sw.getLapTimeCur() < 8) {
                        setButton("Lap", Color.GREEN);
                    }
                    else {
                        setButton("Lap", Color.LTGRAY);
                    }
                    appendLogCache(sw.getStringLogRec(Swe.LogRec.RESUME));
                    if (trs.mEnableGps) {
                        appendLogCache(sw.getStringLogRec(Swe.LogRec.GPS));
                    }
                    if (trs.mFakeEvents) {
                        startFakeTimers();
                    }
                }
                if (mAverageFragment != null){
                    mAverageFragment.updateAverageDisplay();
                }
                if (mLocationFragment != null) {
                    mLocationFragment.updateLocationDisplay();
                }
            }
        });

        // Draw up the display with init or restored data
        switch (sw.getState()) {
            case Swe.State.RUNNING:
                if (sw.getLapTimeCur() < 8) {
                    setButton("Lap", Color.GREEN);
                }
                else {
                    setButton("Lap", Color.LTGRAY);
                }
                timerHandler.postDelayed(timerRunnable, 0);
                break;
            case Swe.State.PAUSE:
                setButton("Resume", Color.LTGRAY);
                break;
            default:
                setButton("Start", Color.LTGRAY);
                break;
        }

        // Start fake event timers
        if (trs.mFakeEvents) {
            startFakeTimers();
        }


        // Start periodic logging
        logTimerHandler.postDelayed(logTimerRunnable, 30000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.d(TAG, "onRequestPermissionsResult() - ext store permission granted");
                    mHaveStoragePermission = true;

                } else {
                    // permission denied
                    Log.d(TAG, "onRequestPermissionsResult() - ext store permission denied");
                    mHaveStoragePermission = false;
                }
                break;
            }
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult() - location permission granted");
                    mHaveLocationPermission = true;
                    if (trs.mEnableGps) {
                        getLocation();
                        createLocationRequest();
                        startLocationUpdates();
                    }
                } else {
                    Log.d(TAG, "onRequestPermissionsResult() - location permission denied");
                    mHaveLocationPermission = false;
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "LapFragment.onSensorChanged()");
        sw.step(System.currentTimeMillis(), event.values[0]);
        //todo should we log steps?
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "MainActivity.onResume()");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "MainActivity.onStop()");
        super.onStop();

        //Save state
        // https://developer.android.com/guide/topics/data/data-storage.html#filesInternal
        Map<String, String> intState = sw.getInternalState();
        try {
            // is there a storage area that is writable?
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                // get document directory
                File docDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS).toString());
                if (!docDir.exists()) {
                    Log.d(TAG,
                            "MainActivity.onStop() docDir does not exist, attempt create");
                    if (!docDir.mkdir()) {
                        Log.d(TAG, "MainActivity.onStop() docDir not created");
                    }
                }
                File trDir = new File (docDir.getPath(), "TrackRun");
                if (!trDir.exists()) {
                    Log.d(TAG, "MainActivity.onStop() trDir does not exist, attempt create");
                    if (!trDir.mkdir()) {
                        Log.d(TAG, "MainActivity.onStop() trDir not created");
                    }
                }
                File stateFile = new File(trDir.getPath(), "state");
                if (!stateFile.exists()) {
                    Log.d(TAG,
                            "MainActivity.onStop() stateFile does not exist, attempt create");
                    if (!stateFile.createNewFile()) {
                        Log.d(TAG, "MainActivity.onStop() stateFile not created");
                    }
                } else {
                    // have a good state file created, save off state
                    Log.d(TAG, "MainActivity.onStop() write state to stateFile");
                    FileWriter fw;
                    BufferedWriter bw;
                    PrintWriter pw = null;
                    try {
                        fw = new FileWriter(stateFile, false);
                        bw = new BufferedWriter(fw);
                        pw = new PrintWriter(bw);

                        Iterator it = intState.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String k = (String) pair.getKey();
                            String v = (String) pair.getValue();
                            String line = "\"" + k + "\"" + "," +  "\"" + v + "\"";
                            Log.d(TAG, line);
                            pw.println(line);
                        }
                    }
                    catch (IOException e) {
                        Log.d(TAG, "MainActivity.onStop() IO exception in write state");
                    }
                    finally {
                        if (pw != null) pw.close();
                    }
                }
            }
        }
        catch (IOException e) {
                Log.e("TrackRun", "MainActivity.onStop() createNewFile IO exception");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (trs.mEnableGps) {
            stopLocationUpdates();
        }
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_stop) {
            onStopMenu();
            return true;
        } else if (id == R.id.action_savelog) {
            saveLogToGoogleDrive();
            return true;
        } else if (id == R.id.action_reset) {
            onResetMenu();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("LAPS_PER_MILE", trs.getLapsPerMileString());
            intent.putExtra("STEPS_PER_MILE", trs.getStepsPerMileString());
            intent.putExtra("COUNT_LAPS", trs.mCountLaps);
            intent.putExtra("ENABLE_GPS", trs.mEnableGps);
            intent.putExtra("FAKE_EVENTS", trs.mFakeEvents);
            startActivityForResult(intent, UPDATE_SETTINGS_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "MainActivity.onActivityResult() - RESULT_OK");
                // Handle laps per mile setting
                trs.setLapsPerMile(data.getStringExtra("LAPS_PER_MILE"));
                // Handle steps per mile setting
                trs.setStepsPerMile(data.getStringExtra("STEPS_PER_MILE"));
                // todo Handle count laps/miles setting
                trs.setCountLaps(data.getBooleanExtra("COUNT_LAPS", true));
                // todo Handle enable/disable GPS
                boolean rv = data.getBooleanExtra("ENABLE_GPS", false);
                if (rv != trs.mEnableGps) {
                    // GPS setting was updated
                    if (rv == false) {
                        // Disable GPS
                        stopLocationUpdates();
                        trs.setEnableGps(false);
                    } else {
                        // Enable GPS
                        if (mHaveLocationPermission) {
                            getLocation();
                            createLocationRequest();
                            startLocationUpdates();
                            trs.setEnableGps(true);
                        } else {
                            Toast.makeText(this, "Can't enable GPS, check permission",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
                // Handle fake events setting
                rv = data.getBooleanExtra("FAKE_EVENTS", false);
                if (rv != trs.mFakeEvents) {
                    if (rv == false) {
                        trs.setFakeEvents(false);
                        stopFakeTimers();
                    } else {
                        trs.setFakeEvents(true);
                        startFakeTimers();
                    }
                }
                // Save preferences
                SharedPreferences settings = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.putString("LapsPerMile", trs.getLapsPerMileString());
                editor.putString("StepsPerMile", trs.getStepsPerMileString());
                editor.putBoolean("EnableGps", trs.getEnableGps());
                editor.putBoolean("CountLaps", trs.getCountLaps());
                editor.putBoolean("FakeEvents", trs.getFakeEvents());
                editor.commit();

                Toast.makeText(this, "Settings Updated", Toast.LENGTH_LONG).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.d("TrackRunV2", "MainActivity.onActivityResult() - RESULT_CANCELLED");
            }
            if (mAverageFragment != null){
                mAverageFragment.updateAverageDisplay();
            }
            if (mLocationFragment != null) {
                mLocationFragment.updateLocationDisplay();
            }
        }

        if (requestCode == SIGNIN_REQUIRED) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "RESULT_OK from signin resolution");
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return mLapFragment = LapFragment.newInstance(position + 1);
                case 1:
                    return mAverageFragment = AverageFragment.newInstance(position + 1);
                case 2:
                    return mLocationFragment = LocationFragment.newInstance(position + 1);
                default:
                    //todo test what happens if reached
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Lap";
                case 1:
                    return "Average";
                case 2:
                    return "Location";
            }
            return null;
        }


    }

    // my member functions

    public void onStopMenu() {
        Log.d(TAG, "MainActivity.onStopMenu()");
        if (sw.getState() == Swe.State.PAUSE) {
            return; //do nothing if already stopped
        }
        sw.pause(System.currentTimeMillis());
        appendLogCache(sw.getStringLogRec(Swe.LogRec.STOP));
        timerHandler.removeCallbacks(timerRunnable);
        setButton("Resume", Color.LTGRAY);
        if (mLapFragment != null) {
            mLapFragment.updateLapDisplay();
        }
        //todo problem here is what should average show, last lap, tenth, or current?
        //if (mAverageFragment != null){
        //    mAverageFragment.updateAverageDisplay();
        //}
        if (mLocationFragment != null) {
            mLocationFragment.updateLocationDisplay();
        }
        if (trs.mFakeEvents) {
            stopFakeTimers();
        }
    }

    public void onResetMenu() {
        Log.d(TAG, "MainActivity.onResetMenu()");
        //if running, kill timer
        if (sw.getState() == Swe.State.RUNNING) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        stopFakeTimers();
        // initialize display and sw values
        sw.reset(System.currentTimeMillis());
        setButton("Start", Color.LTGRAY);
        //lapCountTextView.setText(String.format("%d", 0));
        //mileCountTextView.setText("0.00");
        //lastLapTimeTextView.setText(String.format("%4.1f", 0.0));
        //avgLapTextView.setText(String.format("%5.2f", 0.0));
        //avgMphTextView.setT/ext(String.format("%-5.2f", 0.0));
        //stepCountTextView.setText(String.format("0"));
        //stepsPMTextView.setText(String.format("%5.2f", 0.0));
        //avgSPMTextView.setText(String.format("%5.2f", 0.0));
        if (mLapFragment != null) {
            mLapFragment.updateLapDisplay();
        }
        if (mAverageFragment != null){
            mAverageFragment.updateAverageDisplay();
        }
        if (mLocationFragment != null) {
            mLocationFragment.updateLocationDisplay();
        }
        // clear log cache
        clearLogCache();
        // periodic logging
        periodicLoggingLastDistance = 0.0;
    }

    public void setButton(CharSequence text, int color) {
        b.setText(text);
        b.setBackgroundColor(color);
        if (color == Color.GREEN) {
            mButtonIsGreen = true;
        } else {
            mButtonIsGreen = false;
        }
    }

    /*
    ** GoogleApi related fields, overrides and methods
    */

    private GoogleApiClient mGoogleApiClient;

    protected void doInitGoogleLocationApi() {
        Log.d(TAG, "doInitGoogleLocationApi()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected()");
        if (!mHaveLocationPermission) {
            doGetLocationPermission();
        }

        if (mHaveLocationPermission) {
            if (trs.mEnableGps) {
                getLocation();
                createLocationRequest();
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, SIGNIN_REQUIRED);
            }
            catch (android.content.IntentSender.SendIntentException e) {
                Log.d(TAG, "Exception from connectionFailed resolution");
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /*
    ** Permission related fields, overrides and methods
    */

    static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 51;
    private boolean mHaveLocationPermission = false;

    protected void doCheckLocationPermission() {
        Log.d(TAG, "doCheckLocationPermission()");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "have location permission");
            mHaveLocationPermission = true;
        } else {
            Log.d(TAG, "do not have location permission");
            mHaveLocationPermission = false;
        }
    }

    protected void doGetLocationPermission() {
        Log.d(TAG, "doGetLocationPermission()");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "FINE permission granted in onCreate()");
            mHaveLocationPermission = true;
        } else {
            Log.d(TAG, "FINE permission not granted during onCreate()");
            mHaveLocationPermission = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }

    /*
    ** Location related fields, overrides and methods
    */

    LocationRequest mLocationRequest;

    @Override
    public void onLocationChanged(Location l) {
        if (trs.mFakeEvents) return;
        Log.d(TAG, "onLocationChanged()");
        sw.gps(l.getLatitude(), l.getLongitude(),l.getAltitude(), l.getSpeed(), l.getBearing(),
                l.getAccuracy(), l.getTime());
        if (mLocationFragment != null) {
            mLocationFragment.updateLocationDisplay();
        }
        appendLogCache(sw.getStringLogRec(Swe.LogRec.GPS));
        doTenths();
    }

    protected void getLocation() {
        if (trs.mFakeEvents) return;
        Log.d(TAG, "getLocation()");
        try {
            Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (l == null) return;
            sw.gps(l.getLatitude(), l.getLongitude(),l.getAltitude(), l.getSpeed(), l.getBearing(),
                    l.getAccuracy(), l.getTime());
            appendLogCache(sw.getStringLogRec(Swe.LogRec.GPS));
            doTenths();
        }
        catch (SecurityException e) {
            Log.d(TAG, "getLocation() security exception last location");
        }
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()");
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        catch (SecurityException e) {
            Log.d(TAG, "security exception location update");
        }
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates()");
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        catch (SecurityException e) {
            Log.d(TAG, "security exception location update");
        }
    }

    protected void createLocationRequest() {
        Log.d(TAG, "createLocationRequest()");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
    }



    /*
     * Fields and methods for using google document APIs to log application data to a file.  Google
     * play services must be initialized first.
     */

    final String DIR_NAME = "TrackRun";
    DriveId mLogFolder;
    DriveFile mLogFile;
    DriveContents mLogFileContents;
    ParcelFileDescriptor mLogFilePFD;
    File logToSave;

    String makeLogFileName() {
        String logFileName = new SimpleDateFormat("yyyyMMdd-HH:mm:ss'.csv'").format(new Date());
        return logFileName;
    }

    void saveLog(File f) {
        logToSave = f;
        getGdrFolder();
    }

    // getGdrFolder() - get the folder that contains log file, creates if necessary
    // createLogFile() - called by get folder to create log file
    // writeToLogFile(String) - open file, get and write cache data, commit and close

    void getGdrFolder() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.eq(SearchableField.TITLE, DIR_NAME),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(TAG, "getGdrFolder() query failed");
                            return;
                        }
                        for (Metadata m : result.getMetadataBuffer()) {
                            if (m.getTitle().equals(DIR_NAME) && m.isFolder()) {
                                mLogFolder = m.getDriveId();
                                createLogFile();
                                return;
                            }
                        }
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(DIR_NAME)
                                .build();
                        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                .createFolder(mGoogleApiClient, changeSet)
                                .setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
                                    @Override
                                    public void onResult(@NonNull DriveFolder.DriveFolderResult result) {
                                        if (!result.getStatus().isSuccess()) {
                                            Log.d(TAG, "getFolder() create log folder failed");
                                        } else {
                                            mLogFolder = result.getDriveFolder().getDriveId();
                                            createLogFile();
                                        }
                                    }
                                });
                    }
                });
    }

    void createLogFile() {
        //create empty log file
        String fileName = makeLogFileName();
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .setMimeType("text/csv")
                .build();
        mLogFolder.asDriveFolder()
                .createFile(mGoogleApiClient, changeSet, null)
                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(@NonNull DriveFolder.DriveFileResult driveFileResult) {
                        if (!driveFileResult.getStatus().isSuccess()) {
                            Log.d(TAG, "createLogFile() file create error");
                        } else {
                            mLogFile = driveFileResult.getDriveFile();
                            writeToLogFile();
                        }
                    }
                });
    }

    void writeToLogFile() {
        mLogFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                        if (!driveContentsResult.getStatus().isSuccess()) {
                            Log.d(TAG, "openLogFile() file create error");
                        } else {
                            mLogFileContents = driveContentsResult.getDriveContents();
                            mLogFilePFD = mLogFileContents.getParcelFileDescriptor();
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(mLogFilePFD.getFileDescriptor());
                                Writer writer = new OutputStreamWriter(fileOutputStream);
                                BufferedReader br = new BufferedReader(new FileReader(logToSave));
                                String line;
                                while ((line = br.readLine()) != null) {
                                    writer.write(line + "\n");
                                }
                                writer.close();
                            } catch (IOException e) {
                                Log.d(TAG, "writeToLogFile() IOException");
                            }
                            mLogFileContents.commit(mGoogleApiClient, null);
                        }
                    }
                });
    }

    /*
     * Periodic logging fields and methods
     */

    double periodicLoggingLastDistance;

    void doTenths() {
        // called from GPS location update
        if (sw.state == Swe.State.RUNNING) {
            sw.doTenths();
            if (periodicLoggingLastDistance != sw.tenthCumDistance) {
                periodicLoggingLastDistance = sw.tenthCumDistance;
                appendLogCache(sw.getStringLogRec(Swe.LogRec.TENTH));
            }
        }
    }

    void doPeriodicLogging() {
        appendLogCache(sw.getStringLogRec(Swe.LogRec.SEC30));
    }

    /*
     * Fields and methods associated with internal storage. Internal storage is used to capture
     * data that is eventually saved to a Google Drive file. This implementation uses device cache.
     */

    String CACHE_FILE_NAME = "TrackRunLog";
    File logFile;
    static final class LOG_REC {
        int Start = 1;
    }

    void openLogCache() {
        logFile = new File(getCacheDir(), CACHE_FILE_NAME);
    }

    public void appendLogCache(String l) {
        if (l.isEmpty()) return;
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(l.getBytes());
            fos.close();
        }
        catch (FileNotFoundException e) {
            Log.d(TAG, "appendLogCache() cache file not found");
        }
        catch (IOException e) {
            Log.d(TAG, "appendLogCache() IO exception");
        }
    }

    void saveLogToGoogleDrive() {
        saveLog(logFile);
    }

    void clearLogCache() {
        if (logFile.delete()) {
            logFile = new File(getCacheDir(), CACHE_FILE_NAME);
            appendLogCache(sw.getStringLogRec(Swe.LogRec.HEADER));
        } else {
            Log.d(TAG, "clearLogCache() could not delete cache file");
        }
    }

    /*
     * Fields and methods for the fake timers
     */

    // todo move all the fake time things to methods, start/pause/stop timers based on sw state
    long mFakeStepInc = (1000*(9*60 + 30))/1462;
    long mFakeGpsInc = 2000;
    double mFakeStepCount = 0;
    double mFakeGpsLat = 37.6;
    double mFakeGpsLatInc = 0.000055;
    long mFakeLapInc = 42400;

    // timer for fake gps
    Handler fakeGpsTimerHandler = new Handler();
    Runnable fakeGpsTimerRunnable = new Runnable() {
        @Override
        public void run() {
            fakeGpsTimerHandler.removeCallbacks(fakeGpsTimerRunnable);
            fakeGpsTimerHandler.postDelayed(fakeGpsTimerRunnable, mFakeGpsInc);
            long t = System.currentTimeMillis();
            if (sw.state == Swe.State.RUNNING) {
                // only move if running
                mFakeGpsLat += mFakeGpsLatInc;
            }
            sw.gps(mFakeGpsLat, -122.09, 2.0, (float) 3.062224, (float) 0.0, (float) 5.0, t);
            appendLogCache(sw.getStringLogRec(Swe.LogRec.GPS));
            doTenths();
        }
    };

    // timer for fake laps
    Handler fakeLapTimerHandler = new Handler();
    Runnable fakeLapTimerRunnable = new Runnable() {
        @Override
        public void run() {
            fakeLapTimerHandler.removeCallbacks(fakeLapTimerRunnable);
            fakeLapTimerHandler.postDelayed(fakeLapTimerRunnable, mFakeLapInc);
            long t = System.currentTimeMillis();
            sw.lap(t);
            if (mAverageFragment != null) {
                mAverageFragment.updateAverageDisplay();
            }
            setButton("Lap", Color.GREEN);
            appendLogCache(sw.getStringLogRec(Swe.LogRec.LAP));
        }
    };

    // timer for fake steps
    Handler fakeStepTimerHandler = new Handler();
    Runnable fakeStepTimerRunnable = new Runnable() {
        @Override
        public void run() {
            fakeStepTimerHandler.removeCallbacks(fakeStepTimerRunnable);
            fakeStepTimerHandler.postDelayed(fakeStepTimerRunnable, mFakeStepInc);
            if (sw.state == Swe.State.RUNNING) {
                // only move if running
                long t = System.currentTimeMillis();
                mFakeStepCount += 1;
                sw.step(t, mFakeStepCount);
                //appendLogCache(sw.getStringLogRec(Swe.LogRec.STEP));
            }
        }
    };

    void startFakeTimers() {
        Log.d(TAG, "startFakeTimers()");
        // remove any pending callbacks
        fakeStepTimerHandler.removeCallbacks(fakeStepTimerRunnable);
        fakeGpsTimerHandler.removeCallbacks(fakeGpsTimerRunnable);
        fakeLapTimerHandler.removeCallbacks(fakeLapTimerRunnable);

        // start gps timer if gps is enabled
        if (trs.mEnableGps) {
            fakeGpsTimerHandler.postDelayed(fakeGpsTimerRunnable, mFakeGpsInc);
        }

        // always start step timer
        fakeStepTimerHandler.postDelayed(fakeStepTimerRunnable, mFakeStepInc);

        // start lap timer if counting laps and if running
        if (trs.mCountLaps && sw.state == Swe.State.RUNNING) {
            long curLapTime = sw.et-sw.lapTimeStart;
            long tv = mFakeLapInc-curLapTime;
            if (tv < 0 || tv > mFakeLapInc) {
                tv = mFakeLapInc;
            }
            fakeLapTimerHandler.postDelayed(fakeLapTimerRunnable, tv);
        }
    }

    void stopFakeTimers() {
        Log.d(TAG, "stopFakeTimers()");
        // always stop lap timer
        fakeLapTimerHandler.removeCallbacks(fakeLapTimerRunnable);

        // only stop step and gps timers is fake timers are disabled
        if (trs.mFakeEvents) {
            fakeStepTimerHandler.removeCallbacks(fakeStepTimerRunnable);
            fakeGpsTimerHandler.removeCallbacks(fakeGpsTimerRunnable);
        }
    }

}
