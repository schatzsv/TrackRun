package com.svschatz.trackrun;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by svschatz on 3/27/2017.
 */

public class LapFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    // my member variables
    View rootView;
    TextView lapCountTextView;
    TextView elapsedTimeTextView;
    TextView mileCountTextView;
    TextView currentLapTimeTextView;
    TextView lastLapTimeTextView;
    TextView lastLapPaceTextView;
    TextView stepCountTextView;
    TextView stepRateTextView;


    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateLapDisplay();
            timerHandler.postDelayed(this, 250);
        }
    };


    public LapFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LapFragment newInstance(int sectionNumber) {
        LapFragment fragment = new LapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.lap_fragment, container, false);
        Log.d("TrackRun", "LapFragment.onCreateView()");
        lapCountTextView = (TextView) rootView.findViewById(R.id.lapCountTextView);
        elapsedTimeTextView = (TextView) rootView.findViewById(R.id.elapsedTimeTextView);
        mileCountTextView = (TextView) rootView.findViewById(R.id.mileCountTextView);
        currentLapTimeTextView = (TextView) rootView.findViewById(R.id.currentLapTimeTextView);
        lastLapTimeTextView = (TextView) rootView.findViewById(R.id.lastLapTimeTextView);
        lastLapPaceTextView = (TextView) rootView.findViewById(R.id.lastLapPaceTextView);
        stepCountTextView = (TextView) rootView.findViewById(R.id.stepCountTextView);
        stepRateTextView = (TextView) rootView.findViewById(R.id.stepRateTextView);

        updateLapDisplay();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TrackRun", "LapFragment.onResume()");
        timerHandler.postDelayed(timerRunnable, 0);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("TrackRun", "LapFragment.onPause()");
        timerHandler.removeCallbacks(timerRunnable);
    }

    public void updateLapDisplay() {
        //Log.d("TrackRun", "LapFragment.updateLapDisplay()");

        lapCountTextView.setText(MainActivity.sw.getStringLapCount());
        if (MainActivity.sw.getState() == Swe.State.RUNNING) {
            elapsedTimeTextView.setText(MainActivity.sw.getStringET(false));
        } else {
            elapsedTimeTextView.setText(MainActivity.sw.getStringET(true));
        }
        mileCountTextView.setText(MainActivity.sw.getStringMiles());
        currentLapTimeTextView.setText(MainActivity.sw.getStringCurrentLapTime());
        lastLapTimeTextView.setText(MainActivity.sw.getStringLastLapTime());
        lastLapPaceTextView.setText(MainActivity.sw.getStringLastLapPace());
        stepCountTextView.setText("C " + MainActivity.sw.getStringStepCount());
        stepRateTextView.setText(MainActivity.sw.getStringStepRate());
    }
}
