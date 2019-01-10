package de.waldau_webdesign.app.hoehenmesser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    @BindView(R.id.btnSetReference)
    Button btnSetReference;
    @BindView(R.id.tvFloor)
    TextView tvFloor;
    @BindView(R.id.tvPressure)
    TextView tvPressure;
    @BindView(R.id.tvReferencePressure)
    TextView tvReferencePressure;
    @BindView(R.id.tvHeight)
    TextView tvHeight;
    private float mPressure;
    private float mReferencePressure;
    private float mHeight;
    private float mFloorHeight;
    private float mOffset;
    private DatabaseReference mDatabase;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SharedPreferences mPrefs;
    private boolean manualReference;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_right, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }


        mReferencePressure = Float.valueOf(getString(R.string.pref_default_manual_reference));
        mFloorHeight = Float.valueOf(getString(R.string.pref_default_floor_height));
        mOffset = Float.valueOf(getString(R.string.pref_default_reference_offset));

        btnSetReference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrefs.edit().putString("reference_value", String.valueOf(mPressure)).apply();
                loadSettings();
                updateUi();
            }
        });


    }

    @Override
    protected void onResume() {
        // Register a listener for the mSensor.
        super.onResume();
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            loadSettings();
            updateUi();
        }

        mDatabase.child("htw").child("sensor").child("pressure").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot == null)
                            return;

                        Float value = dataSnapshot.getValue(Float.class);
                        if (!manualReference) {
                            mReferencePressure = value / 100;
                            updateUi();
                        }

                        Log.d(TAG, "pressure: " + value);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    private void loadSettings() {

        manualReference = mPrefs.getBoolean("reference_switch", false);

        if (manualReference) {
            mReferencePressure = Float.valueOf(mPrefs.getString("reference_value", getString(R.string.pref_default_manual_reference)));
        }



        mFloorHeight = Float.valueOf(mPrefs.getString("floor_height", getString(R.string.pref_default_floor_height)));
        mOffset = Float.valueOf(mPrefs.getString("reference_offset", getString(R.string.pref_default_reference_offset)));
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the mSensor when the activity pauses.
        super.onPause();

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        mPressure = event.values[0];

        mPressure += mOffset;

        updateUi();

    }

    private void updateUi() {
        mHeight = SensorManager.getAltitude(mReferencePressure, mPressure);

        tvPressure.setText(String.format(Locale.getDefault(), "%.2f", mPressure));
        tvReferencePressure.setText(String.format(Locale.getDefault(), "%.2f", mReferencePressure));
        tvHeight.setText(String.format(Locale.getDefault(), "%.2f", mHeight));


        tvFloor.setText(getFloor(mHeight, mFloorHeight) + ". EG");
    }


    private int getFloor(float height, float floorHeight) {
        return (int) (height / floorHeight);
    }

}
