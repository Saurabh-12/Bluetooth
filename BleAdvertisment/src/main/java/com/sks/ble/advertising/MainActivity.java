package com.sks.ble.advertising;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;
    private Button mStopButton;
    private Spinner spinnerTxPower;
    private Spinner spinnerTxMode;

    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothAdvertiser;
    private AdvertiseCallback advertisingCallback;
    private Handler mHandler = new Handler();

    int power = 0;
    int mode = 0;

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) )
                return;

            StringBuilder builder = new StringBuilder( result.getDevice().getName() );

            builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));

            mText.setText(builder.toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = (TextView) findViewById( R.id.text );
        mDiscoverButton = (Button) findViewById( R.id.discover_btn);
        mAdvertiseButton = (Button) findViewById( R.id.advertise_btn);
        mStopButton = (Button) findViewById( R.id.advertise_stop_btn);
        spinnerTxPower = (Spinner)findViewById(R.id.spinner_tx_power);
        spinnerTxMode = (Spinner)findViewById(R.id.spinner_tx_mode);

        mDiscoverButton.setOnClickListener( this );
        mAdvertiseButton.setOnClickListener( this );
        mStopButton.setOnClickListener( this );
        spinnerTxPower.setOnItemSelectedListener(this);
        spinnerTxMode.setOnItemSelectedListener(this);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterPower = ArrayAdapter.createFromResource(this,
                R.array.tx_power_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapterPower.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinnerTxPower.setAdapter(adapterPower);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterMode = ArrayAdapter.createFromResource(this,
                R.array.tx_mode_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapterMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinnerTxMode.setAdapter(adapterMode);

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if(  !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {
            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
/*            mAdvertiseButton.setEnabled( false );
            mDiscoverButton.setEnabled( false );
            mStopButton.setEnabled(false);*/
            mText.setText("Multiple advertisement not supported");
        }
    }

    private void discover() {
        List<ScanFilter> filters = new ArrayList<ScanFilter>();

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid( new ParcelUuid(UUID.fromString( getString(R.string.ble_uuid ) ) ) )
                .build();
        filters.add( filter );

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }, 10000);
    }

    private void advertise() {
        mBluetoothAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(mode)
                .setTxPowerLevel(power)
                .setTimeout(3*60*1000)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                //.addServiceUuid( pUuid )
                .addServiceData( pUuid, "SK".getBytes(Charset.forName("UTF-8") ) )
                .build();

        advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.e( "ECRT_BLE", "Advertising onStartSuccess: getTxPowerLevel "
                        + settingsInEffect.getTxPowerLevel() );
                String modes = "";
                if(2 == settingsInEffect.getMode()){
                    modes = "LOW LATENCY Mode";
                }else if(1 == settingsInEffect.getMode()) {
                    modes = "BALANCED Mode";
                }else if(0 == settingsInEffect.getMode()) {
                    modes = "LOW POWER Mode";
                }

                String power = "";
                if(3 == settingsInEffect.getTxPowerLevel()){
                    power = "TX_POWER_HIGH";
                }else if(2 == settingsInEffect.getTxPowerLevel()) {
                    power = "TX_POWER_MEDIUM";
                }else if(1 == settingsInEffect.getTxPowerLevel()) {
                    power = "TX_POWER_LOW ";
                } else if(0 == settingsInEffect.getTxPowerLevel()) {
                    power = "TX_POWER_ULTRA_LOW";
                }

                mText.setText("Advertising Start: Success \n"+
                        "Advertising PowerLevel: "+power+"\n"+
                        "Advertising Mode: "+modes+" \n"+
                        "Advertising Timeout(ms): "+settingsInEffect.getTimeout()+"\n"+
                        "Advertising Connectable: "+settingsInEffect.isConnectable());
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "ECRT_BLE", "Advertising onStartFailure: " + errorCode );
                mText.setText("Advertising Start Failure ErrorCode: "+errorCode);
                super.onStartFailure(errorCode);
            }
        };

        mBluetoothAdvertiser.startAdvertising( settings, data, advertisingCallback );
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == R.id.discover_btn ) {
            discover();
        } else if( v.getId() == R.id.advertise_btn ) {
            advertise();
        } else if (v.getId() == R.id.advertise_stop_btn) {
            Log.d("ecrt", "Stop called");
            stopAdvertising(advertisingCallback);
        }
    }

    private void stopAdvertising(final AdvertiseCallback advertiseCallback) throws IllegalStateException {
        if (mBluetoothAdvertiser != null) {
            mBluetoothAdvertiser.stopAdvertising(advertiseCallback);
            mText.setText("Advertise Stopped");
            Log.e( "ECRT_BLE", "stopAdvertising ");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.spinner_tx_power)
        {
            power = position;
            Log.d("ECRT_BLE", "power position: "+position+" ("+parent.getItemAtPosition(position).toString()+")");
            Toast.makeText(MainActivity.this, "Tx Power: "+position,Toast.LENGTH_SHORT).show();
        }
        else if(parent.getId() == R.id.spinner_tx_mode)
        {
            mode = position;
            Log.d("ECRT_BLE", "Mode position: "+position+" ("+parent.getItemAtPosition(position).toString()+")");
            Toast.makeText(MainActivity.this, "Tx Mode: "+position,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if(parent.getId() == R.id.spinner_tx_power)
        {
            power = 0;
        }
        else if(parent.getId() == R.id.spinner_tx_mode)
        {
            mode = 0;
        }
    }
}
