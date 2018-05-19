package myexample.sks.com.mybt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVER_BT = 2;
    private static final int VISIBLE_TIME = 300;
    private static final String TAG = "HomeActivity";
    private static final Boolean DEBUG = true;
    private Boolean mFirstTime = true;
    private Button scanButton, PairedDeviceButton;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    ListView mPairedDevice;
    ArrayList<String>mDiscoveredDeviceList;
    ArrayList<String>mDiscoveredDeviceMACList;
    ArrayAdapter<String> deviceDiscoverableAdapter;
    ArrayAdapter<String> mPairedDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initRes();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if(mBluetoothAdapter == null)
        {
            // Device doesn't support Bluetooth
            showDialog(this, "BT is not supported by this device");

        }else{
            pairedDevices  = mBluetoothAdapter.getBondedDevices();

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

        //Listening for BT state change
        registerReceiver(mBTChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        //Registering receiver for device scanning.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(mBTDiscoverReceiver, filter);

    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBTChangeReceiver);
        unregisterReceiver(mBTDiscoverReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(DEBUG)
            Log.d(TAG, "RequestCode "+requestCode+" resultCode "+resultCode+" data "+data);
        if(REQUEST_ENABLE_BT == requestCode && resultCode == RESULT_OK){
            Toast.makeText(this, "BT turned on successfully", Toast.LENGTH_SHORT).show();
        }else if(REQUEST_ENABLE_BT == requestCode && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "BT turn on decline by user", Toast.LENGTH_SHORT).show();
        }else if(REQUEST_DISCOVER_BT == requestCode && VISIBLE_TIME == RESULT_OK){
            Toast.makeText(this, "Device become visible", Toast.LENGTH_SHORT).show();
            //scanning device and show in list
            mFirstTime = false;
            showDiscoveredDeviceList(mDiscoveredDeviceList);
        }else if(REQUEST_DISCOVER_BT == requestCode && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "decline by user", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver mBTChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DEBUG)
                Log.d(TAG, "BT State chane Action: "+action);

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                //Possible values are: STATE_OFF, STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF,
                int current_state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                int prev_state = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                if(DEBUG) {
                    Log.d(TAG, "Current BT State " + current_state);
                    Log.d(TAG, "Prev BT State "+prev_state);
                }

                /**
                 * STATE_OFF = 10;
                 * STATE_TURNING_ON = 11;
                 * STATE_ON = 12;
                 * STATE_TURNING_OFF = 13
                 *
                 * **/
            }
        }
    };

    private BroadcastReceiver mBTDiscoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DEBUG)
                Log.d(TAG,"BTDiscoverReceiver "+action);
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG,"BTDiscoverReceiver Scanning started");
                Toast.makeText(context, "Scanning Started...", Toast.LENGTH_SHORT).show();
            }
            //When discovery finds a device
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                mDiscoveredDeviceList.add(deviceName);
                mDiscoveredDeviceMACList.add(deviceHardwareAddress);
                Log.d(TAG,"BTDiscoverReceiver deviceName: "+deviceName+" deviceHardwareAddress: "+deviceHardwareAddress);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.d(TAG,"discovery Finished Size: "+mDiscoveredDeviceList.size());
                if(mDiscoveredDeviceList.size() != 0)
                {
                    mPairedDevice.invalidateViews();
                    showDiscoveredDeviceList(mDiscoveredDeviceList);
                    deviceDiscoverableAdapter.notifyDataSetChanged();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "No New Devices Found", Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    private void initRes() {
        scanButton = (Button)findViewById(R.id.button);
        PairedDeviceButton = (Button)findViewById(R.id.show_paired_device_btn);
        mPairedDevice = (ListView)findViewById(R.id.list_item);

        scanButton.setOnClickListener(MainActivity.this);
        PairedDeviceButton.setOnClickListener(MainActivity.this);

        mDiscoveredDeviceList = new ArrayList<>();
        mDiscoveredDeviceMACList = new ArrayList<>();
    }

    private void showPairedDeviceList(Set<BluetoothDevice> pairedList)
    {
        ArrayList<String> deviceNameList = new ArrayList<>();
        ArrayList<String> deviceMACList = new ArrayList<>();

        if (pairedList.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedList) {
                String deviceName = device.getName();
                deviceNameList.add(deviceName);
                String deviceHardwareAddress = device.getAddress(); // MAC address
                deviceMACList.add(deviceHardwareAddress);
            }

            mPairedDeviceAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, deviceNameList);


            // Assign adapter to ListView
            mPairedDevice.setAdapter(mPairedDeviceAdapter);
        }


    }

    private void showDiscoveredDeviceList(ArrayList<String>mDeviceList){
        Log.d(TAG, "Discovery Phone List size"+mDeviceList.size());
        if(mDeviceList != null){
            deviceDiscoverableAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, mDeviceList);
            // Assign adapter to ListView
            mPairedDevice.setAdapter(deviceDiscoverableAdapter);
            //deviceDiscoverableAdapter.notifyDataSetChanged();
        }

    }


    @Override
    public void onClick(View view) {

        if(view == scanButton)
        {
            mPairedDevice.setVisibility(View.VISIBLE);
            mBluetoothAdapter.startDiscovery();
            showDiscoveredDeviceList(mDiscoveredDeviceList);

        }else if(view == PairedDeviceButton)
        {
            mPairedDevice.setVisibility(View.VISIBLE);
            showPairedDeviceList(pairedDevices);
        }

    }

    private void showDialog(Context context, String msg)
    {
        AlertDialog.Builder mDialog = new AlertDialog.Builder(context);
        mDialog.create();
        mDialog.setTitle("Bluetooth");
        mDialog.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        mDialog.show();
    }


}
