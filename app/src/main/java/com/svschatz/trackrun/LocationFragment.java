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

public class LocationFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    //my member variables
    TextView headingTv, speedTv, avgSpeedTv, gpsDistTv;
    TextView textView5, textView6;

    private final Handler timerLocationFragmentHandler = new Handler();
    private final Runnable timerLocationFragmentRunnable = new Runnable() {
        @Override
        public void run() {
            updateLocationDisplay();
            timerLocationFragmentHandler.postDelayed(timerLocationFragmentRunnable, 250);
        }
    };

    public LocationFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LocationFragment newInstance(int sectionNumber) {
        LocationFragment fragment = new LocationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.location_fragment, container, false);
        Log.d("TrackRun", "LocationFragment.onCreateView()");
        headingTv = (TextView) rootView.findViewById(R.id.heading_tv);
        speedTv = (TextView) rootView.findViewById(R.id.speed_tv);
        avgSpeedTv = (TextView) rootView.findViewById(R.id.avgspeed_tv);
        gpsDistTv = (TextView) rootView.findViewById(R.id.gps_dist_tv);
        textView5 = (TextView) rootView.findViewById(R.id.textView5);
        textView6 = (TextView) rootView.findViewById(R.id.textView6);
        updateLocationDisplay();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TrackRun", "LocationFragment.onResume()");
        timerLocationFragmentHandler.postDelayed(timerLocationFragmentRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("TrackRun", "LocationFragment.onPause()");
        timerLocationFragmentHandler.removeCallbacks(timerLocationFragmentRunnable);
    }

    public void updateLocationDisplay() {
        headingTv.setText(MainActivity.sw.getStringGpsHeading() + (char) 0x00B0);
        speedTv.setText(MainActivity.sw.getStringGpsSpeed());
        avgSpeedTv.setText(MainActivity.sw.getStringAvgGpsSpeed());
        gpsDistTv.setText(MainActivity.sw.getStringGpsDistanceRun());
        textView5.setText(MainActivity.sw.getStringStepCount());
        switch (MainActivity.sw.getState()) {
            case Swe.State.RUNNING:
                textView6.setText(MainActivity.sw.getStringET(false));
                break;
            default:
                textView6.setText(MainActivity.sw.getStringET(true));
        }
    }
}
