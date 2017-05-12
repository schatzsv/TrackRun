package com.svschatz.trackrun;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.svschatz.trackrun.MainActivity.sw;
import static com.svschatz.trackrun.MainActivity.trs;

/**
 * Created by steve on 3/18/17.
 */

public class Swe {
    String TAG = "TrackRunSwe";
    int state; //0 - none, 1 - reset, 2 - running, 3 - pause
    public class State {
        static final int NONE = 0;
        static final int RESET = 1;
        static final int RUNNING = 2;
        static final int PAUSE = 3;
    }
    public class LogRec {
        static final int START = 1;
        static final int RESUME = 2;
        static final int STOP = 3;
        static final int LAP = 4;
        static final int TENTH = 5;
        static final int SEC30 = 6;
        static final int GPS = 7;
        static final int HEADER = 8;
        static final int STEP = 9;
    }
    long et; //elapsed time in ms
    long ctLast; //last ct value received, ct in ms
    double stepsCount;
    double stepsCountLast;
    long stepsTimeLastUpdate; //ct time (mS) for last step update
    boolean stepsUpdated; //indicates that an update was received
    double stepsLapStart; //steps at start of lap
    double stepsLastLap; //steps for last lap
    double stepsSpmCur; //instant spm
    double stepsSpmLastLap;
    double stepsSpmAvg;
    double stepsEstimatedMiles;
    int lapCount;
    long lapTimeStart; //et for start of lap
    long lapTimeLast; //mS of last lap
    double lapTimeAvg; //avg lap time in sec
    double lapMphLast; //last lap Mph
    double lapMphAvg; //avg Mph
    public class Lap {
        int number;
        long timeMilli, elapsedTimeMilli;
        double steps;
        public Lap() {
            number = lapCount;
            timeMilli = lapTimeLast;
            elapsedTimeMilli = lapTimeStart;
            steps = stepsLastLap;
        }
    }
    ArrayList<Lap> laps = new ArrayList<Lap>();
    double gpsLatLast, gpsLonLast, gpsAltLast; //last gps data, alt is meters
    float gpsSpdLast, gpsHdgLast, gpsAccLast; //last gps data, meters/s, deg(0-360), meters
    long gpsTimeLast; //gps time mS UTC
    float gpsDistanceRun; //calculated run distance in miles

    double tenthCurDistance, tenthCumDistance; //distance at last tenth
    double tenthNextDistance; //trigger for next calculation
    long tenthCurTime, tenthCumTime; //et at last tenth
    double tenthCurSteps, tenthCumSteps; //step count
    double tenthCurPace, tenthCumPace; //seconds per mile
    double tenthCurStepRate, tenthCumStepRate; //steps per minute


    public Swe() {
        //System.out.println("Swe()");
        reset(System.currentTimeMillis());
    }

    public void reset(long ct) {
        //System.out.println("Swe.reset()");
        //state related
        state = 1;

        //time related
        et = 0;
        ctLast = ct;

        //step related
        stepsCount = 0;
        stepsCountLast = 0;
        stepsTimeLastUpdate = 0;
        stepsUpdated = false;
        stepsLapStart = 0;
        stepsLastLap = 0;
        stepsSpmCur = 0;
        stepsSpmLastLap = 0;
        stepsSpmAvg = 0;
        stepsSpmCur = 0;
        stepsEstimatedMiles = 0;

        //lap related
        lapCount = 0;
        lapTimeStart = 0;
        lapTimeLast = 0;
        lapTimeAvg = 0;
        lapMphLast = 0;
        lapMphAvg = 0;
        laps.clear();

        //gps related
        gpsDistanceRun = (float) 0.0;
        // todo - should lastGps fields be cleared on reset?
        //gpsLatLast = 0.0;
        //gpsLonLast = 0.0;
        //gpsAltLast = 0.0;
        //gpsSpdLast = (float) 0.0;
        //gpsHdgLast = (float) 0.0;
        //gpsAccLast = (float) 0.0;
        //gpsTimeLast = 0;

        //tenth mile related
        tenthCurDistance = 0.0;
        tenthCumDistance = 0.0;
        tenthNextDistance = 0.1;
        tenthCurTime = 0;
        tenthCumTime = 0;
        tenthCurSteps = 0.0;
        tenthCumSteps = 0.0;
        tenthCurPace = 0.0;
        tenthCumPace = 0.0;
        tenthCurStepRate = 0.0;
        tenthCumStepRate = 0.0;

    }

    public void start(long ct) {
        reset(ct);
        state = 2;
    }

    public void pause(long ct) {
        switch (state) {
            case 2:
                state = 3;
                et += ct - ctLast;
                ctLast = ct;
                break;
            default:
                break;
        }
    }

    public void resume(long ct) {
        switch (state) {
            case 3:
                state = 2;
                ctLast = ct;
                break;
            default:
                break;
        }
    }

    public void tick(long ct) {
        if (state == 2) {
            et += ct - ctLast;
            ctLast = ct;
        }
    }

    public void step(long ct, double steps) {
        if (!stepsUpdated) {
            stepsUpdated = true;
            stepsCountLast = steps;
        }
        switch (state) {
            case 2:
                stepsCount += steps - stepsCountLast;
                long stepTime = ct - stepsTimeLastUpdate;
                // todo current steps per minute should really average over the last few steps
                stepsSpmCur = (steps - stepsCountLast) / (stepTime / 60000f);
                stepsTimeLastUpdate = ct;
                stepsCountLast = steps;
                stepsSpmAvg = stepsCount / ((double) et / 1000.0) * 60.0;
                stepsEstimatedMiles = stepsCount / trs.mStepsPerMile;
                break;
            default:
                stepsTimeLastUpdate = ct;
                stepsCountLast = steps;
                break;
        }
    }

    public void lap(long ct) {
        switch (state) {
            case 2:
                et += ct-ctLast;
                ctLast = ct;
                lapCount += 1;
                lapTimeLast = et-lapTimeStart;
                lapTimeStart = et;
                lapTimeAvg = (double) et / (double) lapCount / 1000.0;
                lapMphLast = 1.0 / (double) lapTimeLast * 3600000.0 / trs.mLapsPerMile;
                lapMphAvg = 1.0 / lapTimeAvg * 3600.0 / trs.mLapsPerMile;
                stepsLastLap = stepsCount - stepsLapStart;
                stepsLapStart = stepsCount;
                stepsSpmLastLap = stepsLastLap / ((double) lapTimeLast / 1000.0) * 60.0;
                laps.add(new Lap());
                break;
            default:
                break;
        }
    }

    public void gps(double lat, double lon, double alt,
                    float spd, float hdg, float acc, long time) {
        Log.d(TAG, "gps()");
        switch (state) {
            case State.RUNNING:
                if (gpsTimeLast != 0) {
                    // call tick() so that et lines up with gps distance
                    long t = System.currentTimeMillis();
                    tick(t);
                    double lat1, lat2, lon1, lon2;
                    lat1 = gpsLatLast * Math.PI / 180.0;
                    lat2 = lat * Math.PI / 180.0;
                    lon1 = gpsLonLast * Math.PI / 180.0;
                    lon2 = lon * Math.PI / 180.0;

                    double dLat = lat2 - lat1;
                    double dLon = lon2 - lon1;
                    double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                            Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0) * Math.cos(lat1) * Math.cos(lat2);
                    double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
                    double earthRadiusKm = 6371.0;
                    double dKm = earthRadiusKm * c;
                    double dMi = dKm * 0.621371;
                    //double dFt = dKm * 3280.84;
                    gpsDistanceRun += dMi;
                }
                break;
            default:
                break;
        }
        gpsLatLast = lat;
        gpsLonLast = lon;
        gpsAltLast = alt;
        gpsSpdLast = spd;
        gpsHdgLast = hdg;
        gpsAccLast = acc;
        gpsTimeLast = time;
    }

    public void doTenths() {
        double d;

        if (trs.mEnableGps)
            d = gpsDistanceRun;
        else
            d = stepsEstimatedMiles;
        if (d >= tenthNextDistance) {

            tenthCurDistance = d - tenthCumDistance;
            tenthCumDistance = d;

            tenthNextDistance += 0.1;

            tenthCurTime = et - tenthCumTime;
            tenthCumTime = et;

            tenthCurSteps = stepsCount - tenthCumSteps;
            tenthCumSteps = stepsCount;

            tenthCurPace = tenthCurTime/1000/tenthCurDistance;
            tenthCumPace = tenthCumTime/1000/tenthCumDistance;

            tenthCurStepRate = tenthCurSteps/((double)tenthCurTime/1000/60);
            tenthCumStepRate = tenthCumSteps/((double)tenthCumTime/1000/60);
        }
    }

    public float getDistance() {
        if (trs.mEnableGps) {
            return gpsDistanceRun;
        }
        if (trs.getCountLaps()) {
            return (float) (lapCount / trs.mLapsPerMile);
        }
        return (float) stepsEstimatedMiles;
    }

    public String getStringTenthPace() {
        long t = Math.round(tenthCurPace);
        long s = t % 60;
        long m = t / 60;
        return String.format("%d:%02d", m,s);
    }

    public String getStringTenthStepRate() {
        return String.format("%.1f", tenthCurStepRate);
    }

    public String getStringGpsHeading() {
        return String.format("%03d", Math.round(gpsHdgLast));
    }

    public String getStringGpsSpeed() {
        return String.format("%5.2f", gpsSpdLast * 2.23693629);
    }

    public String getStringAvgGpsSpeed() {
        if (et == 0 || gpsDistanceRun == 0.0) {
            return "0.00";
        }
        double ags = gpsDistanceRun / et * 3600000.0;
        return String.format("%5.2f", ags);
    }

    public String getStringGpsDistanceRun() {
        return String.format("%5.2f Mi", gpsDistanceRun);
    }

    public String getStringLapCount() {
        //return String.format("La %d", lapCount);
        return String.format("La %d", laps.size());
    }

    public String getStringET(boolean show_ms) {
        long millis = et % 1000;
        long seconds = et / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;
        hours = hours % 24;
        if (show_ms) {
            return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        } else {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    public String getStringLapEt(boolean show_ms) {
        // et at start of last lap
        long millis = lapTimeStart % 1000;
        long seconds = lapTimeStart / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;
        hours = hours % 24;
        if (show_ms) {
            return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        } else {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    public String getStringMiles() {
        return String.format("Mi %.2f", lapCount / trs.mLapsPerMile);
    }

    public String getStringStepEstimatedMiles() {
        return String.format("Mi %.2f", Math.floor(stepsEstimatedMiles*100)/100);
    }

    public String getStringCurrentLapTime() {
        double lapTimeSecs = (et-lapTimeStart) / 1000.0;
        if (lapTimeSecs < 59.5) {
            return String.format("Cl %.1f", lapTimeSecs);
        } else {
            int lapHour = (int) Math.floor(lapTimeSecs/3600.0);
            int lapMin = (int) Math.floor((lapTimeSecs - lapHour*3600.0)/60.0);
            int lapSec = (int) Math.floor(lapTimeSecs - lapHour*3600.0 - lapMin*60.0);
            if (lapHour > 0) {
                return String.format("Cl %d:%02d:%02d", lapHour, lapMin, lapSec);
            } else {
                return String.format("Cl %d:%02d", lapMin, lapSec);
            }
        }
    }

    public String getStringLastLapTime() {
        return String.format("Ll %.1f", (double) lapTimeLast / 1000.0);
    }

    public String getStringLastLapPace() {
        double pace = getLapTimeLast() * trs.mLapsPerMile / 60f; //minutes per mile
        int paceMin = (int) Math.floor(pace);
        int paceSec = (int) Math.round(60f * (pace - paceMin));
        if (paceSec == 60) {
            paceMin++;
            paceSec = 0;
        }
        return String.format("Lp %d:%02d", paceMin, paceSec);
    }

    public String getStringAvgPace() {
        if (lapCount == 0) {
            return "P 0:00";
        }
        double pace = (et / 60000.0) / (lapCount / trs.mLapsPerMile); //minutes per mile
        int paceMin = (int) Math.floor(pace);
        int paceSec = (int) Math.round(60f * (pace - paceMin));
        if (paceSec == 60) {
            paceMin++;
            paceSec = 0;
        }
        return String.format("P %d:%02d", paceMin, paceSec);
    }

    public String getStringStepCount() {
        return String.format("%,d", (int) Math.round(stepsCount));
    }

    public String getStringStepRate() {
        return String.format("R %.1f", stepsSpmLastLap);
    }

    public String getStringLastMph() {
        return String.format("Mph %.2f", lapMphAvg);
    }

    public String getStringAvgSpm() {
        return String.format("pm %.1f", stepsSpmAvg);
    }

    public String getStringAvgStepLength() {
        double sl;
        if (getLaps() == 0 || stepsLapStart < 1.0) {
            return "Sl 0.0";
        }
        sl = 63360.0*getLaps()/trs.mLapsPerMile/stepsLapStart;
        if (sl > 1000.0) {
            sl = 0.0;
        }
        return String.format("Sl %.1f", sl);
    }

    public String getStringAvgStepsPerMile() {
        if (getLaps() == 0) {
            return "pM 0";
        }
        return String.format("pM %,d", Math.round(stepsLapStart/(getLaps()/trs.mLapsPerMile)));
    }

    public int getState() {
        return state;
    }

    public long getEt() {
        return et;
    }

    public int getSteps() {
        return (int) Math.round(stepsCount);
    }

    public int getLaps() {
        //return lapCount;
        return laps.size();
    }

    public double getAvgMph() {
        return lapMphAvg;
    }

    public double getLastMph() {
        return lapMphLast;
    }

    public double getLapTimeAvg() {
        return lapTimeAvg;
    }

    public double getLapTimeLast() {
        return (double) lapTimeLast / 1000.0;
    }

    public double getLapTimeCur() {
        return (et-lapTimeStart) / 1000.0;
    }

    public double getAvgSpm() {
        return stepsSpmAvg;
    }

    public double getLastLapSpm() {
        return stepsSpmLastLap;
    }

    public double getCurSpm() {
        return stepsSpmCur;
    }

    public Map<String, String> getInternalState() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("ver", "0.9");
        m.put("state", Integer.toString(state));
        m.put("et", Long.toString(et));
        m.put("ctLast", Long.toString(ctLast));
        m.put("stepsCount", Double.toString(stepsCount));
        m.put("stepsCountLast", Double.toString(stepsCountLast));
        m.put("stepsTimeLastUpdate", Long.toString(stepsTimeLastUpdate));
        //removed because on restart the step sensor may have different number
        //m.put("stepsUpdated", Boolean.toString(stepsUpdated));
        m.put("stepsLapStart", Double.toString(stepsLapStart));
        m.put("stepsLastLap", Double.toString(stepsLastLap));
        m.put("stepsSpmCur", Double.toString(stepsSpmCur));
        m.put("stepsSpmLastLap", Double.toString(stepsSpmLastLap));
        m.put("stepsSpmAvg", Double.toString(stepsSpmAvg));
        m.put("stepsEstimatedMiles", Double.toString(stepsEstimatedMiles));
        m.put("lapCount", Integer.toString(lapCount));
        m.put("lapTimeStart", Long.toString(lapTimeStart));
        m.put("lapTimeLast", Long.toString(lapTimeLast));
        m.put("lapTimeAvg", Double.toString(lapTimeAvg));
        m.put("lapMphLast", Double.toString(lapMphLast));
        m.put("lapMphAvg", Double.toString(lapMphAvg));
        m.put("gpsLatLast", Double.toString(gpsLatLast));
        m.put("gpsLonLast", Double.toString(gpsLonLast));
        m.put("gpsAltLast", Double.toString(gpsAltLast));
        m.put("gpsSpdLast", Float.toString(gpsSpdLast));
        m.put("gpsHdgLast", Float.toString(gpsHdgLast));
        m.put("gpsAccLast", Float.toString(gpsAccLast));
        m.put("gpsTimeLast", Long.toString(gpsTimeLast));
        m.put("gpsDistanceRun", Float.toString(gpsDistanceRun));
        m.put("tenthCurDistance", Double.toString(tenthCurDistance));
        m.put("tenthCumDistance", Double.toString(tenthCumDistance));
        m.put("tenthNextDistance", Double.toString(tenthNextDistance));
        m.put("tenthCurTime", Long.toString(tenthCurTime));
        m.put("tenthCumTime", Long.toString(tenthCumTime));
        m.put("tenthCurSteps", Double.toString(tenthCurSteps));
        m.put("tenthCumSteps", Double.toString(tenthCumSteps));
        m.put("tenthCurPace", Double.toString(tenthCurPace));
        m.put("tenthCumPace", Double.toString(tenthCumPace));
        m.put("tenthCurStepRate", Double.toString(tenthCurStepRate));
        m.put("tenthCumStepRate", Double.toString(tenthCumStepRate));
        for (Lap l : laps) {
            String v = "";
            v += Integer.toString(l.number) + ":";
            v += Double.toString(l.steps) + ":";
            v += Long.toString(l.timeMilli) + ":";
            v += Long.toString(l.elapsedTimeMilli);
            m.put("lap-" + Integer.toString(l.number), v);
        }
        return m;
    }

    public boolean setInternalState(Map<String, String> m) {
        if (!m.containsKey("ver")) return false;
        if (!m.get("ver").contentEquals("0.9")) return false;
        state = Integer.valueOf(m.get("state"));
        et = Long.valueOf(m.get("et"));
        ctLast = Long.valueOf(m.get("ctLast"));
        stepsCount = Double.valueOf(m.get("stepsCount"));
        stepsCountLast = Double.valueOf(m.get("stepsCountLast"));
        stepsTimeLastUpdate = Long.valueOf(m.get("stepsTimeLastUpdate"));
        //removed because on restart the step sensor may have different number
        //stepsUpdated = Boolean.valueOf(m.get("stepsUpdated"));
        stepsLapStart = Double.valueOf(m.get("stepsLapStart"));
        stepsSpmCur = Double.valueOf(m.get("stepsSpmCur"));
        stepsLastLap = Double.valueOf(m.get("stepsLastLap"));
        stepsSpmLastLap = Double.valueOf(m.get("stepsSpmLastLap"));
        stepsSpmAvg = Double.valueOf(m.get("stepsSpmAvg"));
        stepsEstimatedMiles = Double.valueOf(m.get("stepsEstimatedMiles"));
        lapCount = Integer.valueOf(m.get("lapCount"));
        lapTimeStart = Long.valueOf(m.get("lapTimeStart"));
        lapTimeLast = Long.valueOf(m.get("lapTimeLast"));
        lapTimeAvg = Double.valueOf(m.get("lapTimeAvg"));
        lapMphLast = Double.valueOf(m.get("lapMphLast"));
        lapMphAvg = Double.valueOf(m.get("lapMphAvg"));
        gpsLatLast = Double.valueOf(m.get("gpsLatLast"));
        gpsLonLast = Double.valueOf(m.get("gpsLonLast"));
        gpsAltLast = Double.valueOf(m.get("gpsAltLast"));
        gpsSpdLast = Float.valueOf(m.get("gpsSpdLast"));
        gpsHdgLast = Float.valueOf(m.get("gpsHdgLast"));
        gpsAccLast = Float.valueOf(m.get("gpsAccLast"));
        gpsTimeLast = Long.valueOf(m.get("gpsTimeLast"));
        gpsDistanceRun = Float.valueOf(m.get("gpsDistanceRun"));
        tenthCurDistance = Double.valueOf(m.get("tenthCurDistance"));
        tenthCumDistance = Double.valueOf(m.get("tenthCumDistance"));
        tenthNextDistance = Double.valueOf(m.get("tenthNextDistance"));
        tenthCurTime = Long.valueOf(m.get("tenthCurTime"));
        tenthCumTime = Long.valueOf(m.get("tenthCumTime"));
        tenthCurSteps = Double.valueOf(m.get("tenthCurSteps"));
        tenthCumSteps = Double.valueOf(m.get("tenthCumSteps"));
        tenthCurPace = Double.valueOf(m.get("tenthCurPace"));
        tenthCumPace = Double.valueOf(m.get("tenthCumPace"));
        tenthCurStepRate = Double.valueOf(m.get("tenthCurStepRate"));
        tenthCumStepRate = Double.valueOf(m.get("tenthCumStepRate"));

        laps.clear();
        int i = 1;
        int lc = lapCount;
        String delims = "[:]";
        while (i <= lc) {
            String v = m.get("lap-" + Integer.toString(i));
            String[] tokens = v.split(delims);
            lapCount = Integer.valueOf(tokens[0]);
            stepsLastLap = Double.valueOf(tokens[1]);
            lapTimeLast = Long.valueOf(tokens[2]);
            lapTimeStart= Long.valueOf(tokens[3]);
            laps.add(new Lap());
            i++;
        }
        return true;
    }

    String getStringLogRec(int type) {
        switch (type) {
            case LogRec.STEP:
                if (state == State.RUNNING) {
                    return "Step,,,," + stepsTimeLastUpdate + "," + stepsCount + "\n";
                } else {
                    return "";
                }
            case LogRec.GPS:
                if (state == State.RUNNING) {
                    return "GPS,,,,,,,,," + gpsLatLast + "," + gpsLonLast + ","
                            + gpsAltLast + "," + gpsSpdLast + "," + gpsHdgLast + ","
                            + gpsAccLast + "," + gpsTimeLast + "\n";
                } else {
                    return "";
                }
            case LogRec.SEC30:
                return "30Sec,,," + getDistance() + "," + et + "," + stepsCount + "\n";
            case LogRec.TENTH:
                return "Tenth,,,"
                        + tenthCumDistance + ","
                        + tenthCumTime + ","
                        + tenthCumSteps + ","
                        + tenthCurDistance + ","
                        + tenthCurTime + ","
                        + tenthCurSteps + "\n";
            case LogRec.LAP:
                return "Lap,,"
                        + new SimpleDateFormat("HH:mm:ss,", Locale.US).format(new Date())
                        + ",,,"
                        + lapCount + "," + getLapTimeLast() + "," + stepsLastLap + "\n";
            case LogRec.START:
                if (MainActivity.trs.mEnableGps) {
                    return "Start,"
                            + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                            + getDistance() + "," + et + "," + stepsCount + "," +
                            ",,," + gpsLatLast + "," + gpsLonLast + ","
                            + gpsAltLast + "," + gpsSpdLast + "," + gpsHdgLast + ","
                            + gpsAccLast + "," + gpsTimeLast + "\n";
                }
                return "Start,"
                        + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                        + getDistance() + "," + et + "," + stepsCount + "\n";
            case LogRec.RESUME:
                if (MainActivity.trs.mEnableGps) {
                    return "Resume,"
                            + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                            + getDistance() + "," + et + "," + stepsCount + "," +
                            ",,," + gpsLatLast + "," + gpsLonLast + ","
                            + gpsAltLast + "," + gpsSpdLast + "," + gpsHdgLast + ","
                            + gpsAccLast + "," + gpsTimeLast + "\n";
                }
                return "Resume,"
                        + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                        + getDistance() + "," + et + "," + stepsCount + "\n";
            case LogRec.STOP:
                if (MainActivity.trs.mEnableGps) {
                    return "Stop,"
                            + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                            + getDistance() + "," + et + "," + stepsCount + "," +
                            ",,," + gpsLatLast + "," + gpsLonLast + ","
                            + gpsAltLast + "," + gpsSpdLast + "," + gpsHdgLast + ","
                            + gpsAccLast + "," + gpsTimeLast + "\n";
                }
                return "Stop,"
                        + new SimpleDateFormat("yyyyMMdd,HH:mm:ss,", Locale.US).format(new Date())
                        + getDistance() + "," + et + "," + stepsCount + "\n";
            case LogRec.HEADER:
                return "Type,Date,Time,Dist,ET,Steps,Laps,LapTime,LapSteps,Lat,Lon,Alt,Spd,Hdg"
                        + ",Accur,GPStime\n";
            default:
                return "Log Error Invalid Type\n";
        }
    }

}
