package com.svschatz.trackrun;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

public class SettingsActivity extends AppCompatActivity {

    //Button b;
    EditText lpm, spm;
    CheckBox egps, fe;
    RadioButton cl, cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Log.d("TrackRunV2", "SettingsActivity.onCreate()");

        lpm = (EditText) findViewById(R.id.editText_lapsPerMile);
        String lapsPerMile = getIntent().getStringExtra("LAPS_PER_MILE");
        lpm.setText(lapsPerMile);
        spm = (EditText) findViewById(R.id.editText_stepsPerMile);
        String stepsPerMile = getIntent().getStringExtra("STEPS_PER_MILE");
        spm.setText(stepsPerMile);
        egps = (CheckBox) findViewById(R.id.checkBox_enableGps);
        egps.setChecked(getIntent().getBooleanExtra("ENABLE_GPS", true));
        cl = (RadioButton) findViewById(R.id.radioButton_countLaps);
        cl.setChecked(getIntent().getBooleanExtra("COUNT_LAPS", true));
        cm = (RadioButton) findViewById(R.id.radioButton_countMiles);
        cm.setChecked(!getIntent().getBooleanExtra("COUNT_LAPS", true));
        fe = (CheckBox) findViewById(R.id.checkBox_fakeEvents);
        fe.setChecked(getIntent().getBooleanExtra("FAKE_EVENTS", true));

        Button button = (Button) findViewById(R.id.button_saveSettings);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("TrackRunV2", "SettingsActivity.onClick()");
                saveAndReturn();
            }
        });

    }

    void saveAndReturn() {
        Log.d("TrackRunV2", "SettingsActivity.saveAndReturn()");
        Intent intent = new Intent();
        intent.putExtra("LAPS_PER_MILE", lpm.getText().toString());
        intent.putExtra("STEPS_PER_MILE", spm.getText().toString());
        intent.putExtra("ENABLE_GPS", egps.isChecked());
        intent.putExtra("COUNT_LAPS", cl.isChecked());
        intent.putExtra("FAKE_EVENTS", fe.isChecked());
        setResult(RESULT_OK, intent);
        finish();
    }



    //setResult() RESULT_CANCELED, RESULT_OK


}
