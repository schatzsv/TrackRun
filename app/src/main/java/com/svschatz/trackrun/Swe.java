package com.svschatz.trackrun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 3/18/17.
 */

public class Swe {
    private double mLapsPerMile = 13.0;
    int state; //0 - none, 1 - reset, 2 - running, 3 - pause
    public class State {
        static final int NONE = 0;
        static final int RESET = 1;
        static final int RUNNING = 2;
        static final int PAUSE = 3;
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

    public Swe() {
        //System.out.println("Swe()");
        reset(System.currentTimeMillis());
    }

    public void setLapsPerMile(double lpm) {
        mLapsPerMile = lpm;
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

        //lap related
        lapCount = 0;
        lapTimeStart = 0;
        lapTimeLast = 0;
        lapTimeAvg = 0;
        lapMphLast = 0;
        lapMphAvg = 0;
        laps.clear();
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
                lapMphLast = 1.0 / (double) lapTimeLast * 3600000.0 / mLapsPerMile;
                lapMphAvg = 1.0 / lapTimeAvg * 3600.0 / mLapsPerMile;
                stepsLastLap = stepsCount - stepsLapStart;
                stepsLapStart = stepsCount;
                stepsSpmLastLap = stepsLastLap / ((double) lapTimeLast / 1000.0) * 60.0;
                laps.add(new Lap());
                break;
            default:
                break;
        }
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
        return String.format("Mi %.2f", lapCount / mLapsPerMile);
    }

    public String getStringCurrentLapTime() {
        return String.format("Cl %.1f", (et-lapTimeStart) / 1000.0);
    }

    public String getStringLastLapTime() {
        return String.format("Ll %.1f", (double) lapTimeLast / 1000.0);
    }

    public String getStringLastLapPace() {
        double pace = getLapTimeLast() * mLapsPerMile / 60f; //minutes per mile
        int paceMin = (int) Math.floor(pace);
        int paceSec = (int) Math.round(60f * (pace - paceMin));
        if (paceSec == 60) {
            paceMin++;
            paceSec = 0;
        }
        return String.format("Lp %d:%02d", paceMin, paceSec);
    }

    public String getStringAvgPace() {
        double pace = (et / 60000.0) / (lapCount / mLapsPerMile); //minutes per mile
        int paceMin = (int) Math.floor(pace);
        int paceSec = (int) Math.round(60f * (pace - paceMin));
        if (paceSec == 60) {
            paceMin++;
            paceSec = 0;
        }
        return String.format("P %d:%02d", paceMin, paceSec);
    }

    public String getStringStepCount() {
        return String.format("C %,d", (int) Math.round(stepsCount));
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
        sl = 63360.0*getLaps()/mLapsPerMile/stepsLapStart;
        if (sl > 1000.0) {
            sl = 0.0;
        }
        return String.format("Sl %.1f", sl);
    }

    public String getStringAvgStepsPerMile() {
        return String.format("pM %,d", Math.round(stepsLapStart/(getLaps()/mLapsPerMile)));
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
        m.put("ver", "0.2");
        m.put("state", Integer.toString(state));
        m.put("et", Long.toString(et));
        m.put("ctLast", Long.toString(ctLast));
        m.put("stepsCount", Double.toString(stepsCount));
        m.put("stepsCountLast", Double.toString(stepsCountLast));
        m.put("stepsTimeLastUpdate", Long.toString(stepsTimeLastUpdate));
        m.put("stepsUpdated", Boolean.toString(stepsUpdated));
        m.put("stepsLapStart", Double.toString(stepsLapStart));
        m.put("stepsLastLap", Double.toString(stepsLastLap));
        m.put("stepsSpmCur", Double.toString(stepsSpmCur));
        m.put("stepsSpmLastLap", Double.toString(stepsSpmLastLap));
        m.put("stepsSpmAvg", Double.toString(stepsSpmAvg));
        m.put("lapCount", Integer.toString(lapCount));
        m.put("lapTimeStart", Long.toString(lapTimeStart));
        m.put("lapTimeLast", Long.toString(lapTimeLast));
        m.put("lapTimeAvg", Double.toString(lapTimeAvg));
        m.put("lapMphLast", Double.toString(lapMphLast));
        m.put("lapMphAvg", Double.toString(lapMphAvg));
        // todo have to add laps ArrayList here
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
        if (!m.get("ver").contentEquals("0.2")) return false;
        state = Integer.valueOf(m.get("state"));
        et = Long.valueOf(m.get("et"));
        ctLast = Long.valueOf(m.get("ctLast"));
        stepsCount = Double.valueOf(m.get("stepsCount"));
        stepsCountLast = Double.valueOf(m.get("stepsCountLast"));
        stepsTimeLastUpdate = Long.valueOf(m.get("stepsTimeLastUpdate"));
        stepsUpdated = Boolean.valueOf(m.get("stepsUpdated"));
        stepsLapStart = Double.valueOf(m.get("stepsLapStart"));
        stepsSpmCur = Double.valueOf(m.get("stepsSpmCur"));
        stepsLastLap = Double.valueOf(m.get("stepsLastLap"));
        stepsSpmLastLap = Double.valueOf(m.get("stepsSpmLastLap"));
        stepsSpmAvg = Double.valueOf(m.get("stepsSpmAvg"));
        lapCount = Integer.valueOf(m.get("lapCount"));
        lapTimeStart = Long.valueOf(m.get("lapTimeStart"));
        lapTimeLast = Long.valueOf(m.get("lapTimeLast"));
        lapTimeAvg = Double.valueOf(m.get("lapTimeAvg"));
        lapMphLast = Double.valueOf(m.get("lapMphLast"));
        lapMphAvg = Double.valueOf(m.get("lapMphAvg"));
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

}
