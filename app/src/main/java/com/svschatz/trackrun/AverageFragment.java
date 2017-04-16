package com.svschatz.trackrun;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by svschatz on 3/27/2017.
 */

public class AverageFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    // my member variables
    View rootView;
    TextView lapsTv, milesTv;
    TextView paceTv, mphTv;
    TextView stepsTv, stepspermileTv;
    TextView stepsperminTv, steplengthTv;
    TextView etTv;

    public AverageFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AverageFragment newInstance(int sectionNumber) {
        AverageFragment fragment = new AverageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.average_fragment, container, false);
        Log.d("TrackRunV2", "AverageFragment.onCreate()");

        lapsTv = (TextView) rootView.findViewById(R.id.lap_count_aftv);
        milesTv = (TextView) rootView.findViewById(R.id.miles_aftv);
        paceTv = (TextView) rootView.findViewById(R.id.pace_aftv);
        mphTv = (TextView) rootView.findViewById(R.id.mph_aftv);
        stepsTv = (TextView) rootView.findViewById(R.id.steps_aftv);
        stepspermileTv = (TextView) rootView.findViewById(R.id.stepspermile_aftv);
        stepsperminTv = (TextView) rootView.findViewById(R.id.stepspermin_aftv);
        steplengthTv = (TextView) rootView.findViewById(R.id.steplen_aftv);
        etTv = (TextView) rootView.findViewById(R.id.et_aftv);

        updateAverageDisplay();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TrackRunV2", "AverageFragment.onResume()");
        updateAverageDisplay();

    }

    public void updateAverageDisplay() {
        lapsTv.setText(MainActivity.sw.getStringLapCount());
        milesTv.setText(MainActivity.sw.getStringMiles());
        paceTv.setText(MainActivity.sw.getStringAvgPace());
        mphTv.setText(MainActivity.sw.getStringLastMph());
        stepsTv.setText("C " + MainActivity.sw.getStringStepCount());
        stepspermileTv.setText(MainActivity.sw.getStringAvgStepsPerMile());
        stepsperminTv.setText(MainActivity.sw.getStringAvgSpm());
        steplengthTv.setText(MainActivity.sw.getStringAvgStepLength());
        etTv.setText(MainActivity.sw.getStringLapEt(false));
    }
}
