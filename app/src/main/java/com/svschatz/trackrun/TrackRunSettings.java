package com.svschatz.trackrun;

/**
 * Created by svschatz on 4/18/2017.
 */

public class TrackRunSettings {
    boolean mEnableGps;
    boolean mCountLaps;
    double mLapsPerMile;
    double mStepsPerMile;

    public TrackRunSettings() {
        mEnableGps = false;
        mCountLaps = true;
        mLapsPerMile = 13.0;
        mStepsPerMile = 1462.0;
    }

    public void setEnableGps(boolean s) {
        mEnableGps = s;
    }

    public boolean getEnableGps() {
        return mEnableGps;
    }

    public void setCountLaps(boolean s) {
        mCountLaps = s;
    }

    public boolean getCountLaps() {
        return mCountLaps;
    }

    public void setLapsPerMile(double lpm) {
        mLapsPerMile = lpm;
    }

    public void setLapsPerMile(String lpm) {
        mLapsPerMile = Double.valueOf(lpm);
    }

    public double getLapsPerMileDouble() {
        return mLapsPerMile;
    }

    public String getLapsPerMileString() {
        return String.valueOf(mLapsPerMile);
    }

    public double getStepsPerMileDouble() {
        return mStepsPerMile;
    }

    public String getStepsPerMileString() {
        return String.valueOf(mStepsPerMile);
    }

    public void setStepsPerMile(double spm) {
        mStepsPerMile = spm;
    }

    public void setStepsPerMile(String spm) {
        mStepsPerMile = Double.valueOf(spm);
    }
}
