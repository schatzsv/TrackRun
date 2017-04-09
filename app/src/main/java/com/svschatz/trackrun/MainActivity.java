package com.svschatz.trackrun;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // my members
    public static Swe sw = new Swe();
    LapFragment mLapFragment;
    AverageFragment mAverageFragment;
    boolean mButtonIsGreen = false;
    Button b;
    private SensorManager sensorManager;

    // my settings
    // todo add these to the saved/restored data
    public static String mLapsPerMileString = "13.0";
    public static double mLapsPerMileDouble = 13.0;
    private boolean mEnableGps = false;
    private boolean mCountLaps = true;

    // intent codes
    static final int UPDATE_SETTINGS_REQUEST = 1000;

    // timer
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            sw.tick(System.currentTimeMillis());
            if (mButtonIsGreen) {
                if (sw.getLapTimeCur() >= 8.0) {
                    b.setBackgroundColor(Color.LTGRAY);
                    mButtonIsGreen = false;
                }
            }
            timerHandler.postDelayed(this, 100);
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
        Log.d("TrackRunV2", "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // if app was previously running, restore state
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        Map<String, String> intState = (Map<String, String>) settings.getAll();
        boolean rv = sw.setInternalState(intState);
        if (rv) Toast.makeText(this, "Restored state data", Toast.LENGTH_LONG).show();

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
                Log.d("TrackRunV2", "MainActivity.onClick()");
                Button b = (Button) v;
                if (sw.getState() == Swe.State.RESET) {
                    // Start pressed
                    sw.start(System.currentTimeMillis());
                    timerHandler.postDelayed(timerRunnable, 0);
                    setButton("Lap", Color.GREEN);
                } else if (sw.getState() == Swe.State.RUNNING) {
                    // Lap pressed
                    if (sw.getLapTimeCur() >= 8.0) {sw.lap(System.currentTimeMillis());
                        setButton("Lap", Color.GREEN);
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
                }
                mAverageFragment.updateAverageDisplay();
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

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("LAPFRAG", "LapFragment.onSensorChanged()");
        sw.step(System.currentTimeMillis(), event.values[0]);
    }

    @Override
    public void onResume() {
        Log.d("TrackRunV2", "MainActivity.onResume()");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d("TrackRunV2", "MainActivity.onStop()");
        super.onStop();

        //Save state
        // todo should use internal storage
        // https://developer.android.com/guide/topics/data/data-storage.html#filesInternal
        Map<String, String> intState = sw.getInternalState();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        Iterator it = intState.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            editor.putString((String) pair.getKey(), (String) pair.getValue());
        }
        editor.apply();
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
        } else if (id == R.id.action_reset) {
            onResetMenu();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("LAPS_PER_MILE", mLapsPerMileString);
            intent.putExtra("COUNT_LAPS", mCountLaps);
            intent.putExtra("ENABLE_GPS", mEnableGps);
            startActivityForResult(intent, UPDATE_SETTINGS_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d("TrackRunV2", "MainActivity.onActivityResult() - RESULT_OK");
                mLapsPerMileString = data.getStringExtra("LAPS_PER_MILE");
                mLapsPerMileDouble = Double.parseDouble(mLapsPerMileString);
                mCountLaps = data.getBooleanExtra("COUNT_LAPS", true);
                mEnableGps = data.getBooleanExtra("ENABLE_GPS", false);
                Toast.makeText(this, "Settings Updated", Toast.LENGTH_LONG).show();
                // todo settings changed should trigger some actions to update?
                sw.setLapsPerMile(mLapsPerMileDouble);
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.d("TrackRunV2", "MainActivity.onActivityResult() - RESULT_CANCELLED");
            }
            mAverageFragment.updateAverageDisplay();
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
                    return LocationFragment.newInstance(position + 1);
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
        Log.d("TrackRunV2", "MainActivity.onStopMenu()");
        if (sw.getState() == Swe.State.PAUSE) {
            return; //do nothing if already stopped
        }
        sw.pause(System.currentTimeMillis());
        timerHandler.removeCallbacks(timerRunnable);
        setButton("Resume", Color.LTGRAY);
        mLapFragment.updateLapDisplay();
        mAverageFragment.updateAverageDisplay();
        return;
    }

    public void onResetMenu() {
        Log.d("TrackRunV2", "MainActivity.onResetMenu()");
        //if running, kill timer
        if (sw.getState() == Swe.State.RUNNING) {
            timerHandler.removeCallbacks(timerRunnable);
        }
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
        mLapFragment.updateLapDisplay();
        mAverageFragment.updateAverageDisplay();
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
}
