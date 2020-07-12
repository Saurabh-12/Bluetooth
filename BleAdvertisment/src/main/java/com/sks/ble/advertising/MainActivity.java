package com.sks.ble.advertising;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
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
    private AdvertisingSetCallback advertisingSetCallback;
    private AdvertiseSettings advertiseSettings;
    private AdvertisingSetParameters advertisingSetParameters;
    private Handler mHandler = new Handler();
    private  static final byte[]   SERVICE_UUID_BYTE = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int txPowerLevel = -7;
            switch(power) {
                case 0:
                    txPowerLevel = -21;
                    break;
                case 1:
                    txPowerLevel = -15;
                    break;
                case 2:
                    txPowerLevel = -7;
                    break;
                case 3:
                    txPowerLevel = 1;
                    break;
            }
            advertisingSetParameters = new AdvertisingSetParameters.Builder()
                    .setLegacyMode(true)
                    .setConnectable(false)
                    .setScannable(false)
                    .setTxPowerLevel(txPowerLevel)
                    .build();
        } else {
            advertiseSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(mode)
                    .setTxPowerLevel(power)
                    .setTimeout(3 * 60 * 1000)
                    .setConnectable(false)
                    .build();
        }
        byte[] advertisingData = new byte[23];
        advertisingData[0] = 0x02;
        advertisingData[1] = 0x15;

        System.arraycopy(SERVICE_UUID_BYTE, 0, advertisingData, 2, SERVICE_UUID_BYTE.length);
        int major = 0;
        int minor = 1;
        advertisingData[18] = (byte) (major >> 8 & 0xFF);
        advertisingData[19] = (byte) (major & 0xFF);
        advertisingData[20] = (byte) (minor >> 8 & 0xFF);
        advertisingData[21] = (byte) (minor & 0xFF);
        /* Tx Power */
        advertisingData[22] = (byte)0xFF;

        final AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData.Builder();
        // advertiseDataBuilder.setIncludeDeviceName(true);
        advertiseDataBuilder.addManufacturerData(0x004C, advertisingData);
        final AdvertiseData data = advertiseDataBuilder.build();
        //ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );

            /*AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName( true )
                    //.addServiceUuid( pUuid )
                    .addServiceData( pUuid, "SK".getBytes(Charset.forName("UTF-8") ) )
                    .build();*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            advertisingSetCallback = new AdvertisingSetCallback() {
                @Override
                public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                    if (status == ADVERTISE_SUCCESS) {
                        Log.e("saurabh_BLE", "Advertising onAdvertisingSetStarted: getTxPowerLevel "
                                + txPower);
                        mText.setText("Advertising Start: Success \n"+
                                "Advertising PowerLevel: "+txPower+"\n");
                    }
                }

                public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enable, int status) {
                    if (status == ADVERTISE_SUCCESS) {
                        Log.e("saurabh_BLE", "Advertising onAdvertisingEnabled: enable "
                                + enable);
                    }
                }

                public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                    if (status == ADVERTISE_SUCCESS) {
                        Log.e("saurabh_BLE", "Advertising onAdvertisingDataSet ");

                    }
                }

                public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                    Log.e("saurabh_BLE", "Advertising onAdvertisingSetStopped ");
                    mAdvertiseButton.setEnabled(true);
                }
            };
        }
        advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.e( "saurabh_BLE", "Advertising onStartSuccess: getTxPowerLevel "
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
                mAdvertiseButton.setEnabled(true);
                Log.e( "saurabh_BLE", "Advertising onStartFailure: " + errorCode );
                mText.setText("Advertising Start Failure ErrorCode: "+errorCode);
                super.onStartFailure(errorCode);
            }
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBluetoothAdvertiser.startAdvertisingSet(advertisingSetParameters, data, null, null, null, advertisingSetCallback);
        } else {
            mBluetoothAdvertiser.startAdvertising(advertiseSettings, data, advertisingCallback);
        }

/*        AdvertiseSettings settings = new AdvertiseSettings.Builder()
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
                Log.e( "saurabh_BLE", "Advertising onStartSuccess: getTxPowerLevel "
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
                Log.e( "saurabh_BLE", "Advertising onStartFailure: " + errorCode );
                mText.setText("Advertising Start Failure ErrorCode: "+errorCode);
                super.onStartFailure(errorCode);
            }
        };

        mBluetoothAdvertiser.startAdvertising( settings, data, advertisingCallback );*/
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == R.id.discover_btn ) {
            discover();
        } else if( v.getId() == R.id.advertise_btn ) {
            advertise();
        } else if (v.getId() == R.id.advertise_stop_btn) {
            Log.d("Saurabh", "Stop called");
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mBluetoothAdvertiser != null) {
                mBluetoothAdvertiser.stopAdvertisingSet(advertisingSetCallback);
                mText.setText("Advertise Stopped");
            }else {
                stopAdvertising(advertisingCallback);
            }
        }
    }

    private void stopAdvertising(final AdvertiseCallback advertiseCallback) throws IllegalStateException {
        if (mBluetoothAdvertiser != null) {
            mBluetoothAdvertiser.stopAdvertising(advertiseCallback);
            mText.setText("Advertise Stopped");
            Log.e( "saurabh_BLE", "stopAdvertising ");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.spinner_tx_power)
        {
            power = position;
            Log.d("saurabh_BLE", "power position: "+position+" ("+parent.getItemAtPosition(position).toString()+")");
            Toast.makeText(MainActivity.this, "Tx Power: "+position,Toast.LENGTH_SHORT).show();
        }
        else if(parent.getId() == R.id.spinner_tx_mode)
        {
            mode = position;
            Log.d("saurabh_BLE", "Mode position: "+position+" ("+parent.getItemAtPosition(position).toString()+")");
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
