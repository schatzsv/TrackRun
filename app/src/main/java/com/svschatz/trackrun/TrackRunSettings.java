package com.svschatz.trackrun;

/**
 * Created by svschatz on 4/18/2017.
 */

public class TrackRunSettings {
    boolean mEnableGps;
    boolean mCountLaps;
    double mLapsPerMile;
    double mStepsPerMile;
    boolean mFakeEvents;

    public TrackRunSettings() {
        mEnableGps = false;
        mCountLaps = true;
        mLapsPerMile = 13.0;
        mStepsPerMile = 1462.0;
        mFakeEvents = false;
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

    public void setFakeEvents(boolean s) {
        mFakeEvents = s;
    }

    public boolean getFakeEvents() {
        return mFakeEvents;
    }
}
