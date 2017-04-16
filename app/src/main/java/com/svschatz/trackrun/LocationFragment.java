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
    TextView headingTv, speedTv, gpsDistTv;
/*
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateLocationDisplay();
            timerHandler.postDelayed(this, 500);
        }
    };
*/
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
        Log.d("TrackRunV2", "LocationFragment.onCreate()");
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText("Location: " + getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
        headingTv = (TextView) rootView.findViewById(R.id.heading_tv);
        speedTv = (TextView) rootView.findViewById(R.id.speed_tv);
        gpsDistTv = (TextView) rootView.findViewById(R.id.gps_dist_tv);
        updateLocationDisplay();
        return rootView;
    }

    public void updateLocationDisplay() {
        headingTv.setText(MainActivity.sw.getStringGpsHeading());
        speedTv.setText(MainActivity.sw.getStringGpsSpeed());
        gpsDistTv.setText(MainActivity.sw.getStringGpsDistanceRun());
    }
}
