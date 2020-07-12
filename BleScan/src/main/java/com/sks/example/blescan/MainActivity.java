package com.sks.example.blescan;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.sks.example.util.BleUtil;
import com.sks.example.util.ScannedDevice;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback{
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;
    ListView deviceListView;
    /** A bluetooth scan callback listener for build Lollipop or above.       */
    private ScanCallback          mScanCallback;

    /** A bluetooth scan callback listener for build Jelly Bean MR2 or above
     *  to Lollipop
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private List<ScanFilter> SCAN_FILTER_LIST = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        init();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Permission already Granted
            //Do your work here
            //Perform operations here only which requires permission
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission Granted
                //Do your work here
                //Perform operations here only which requires permission
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi,
                         final byte[] newScanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceAdapter.update(newDeivce, newRssi, newScanRecord);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // ignore
            return true;
        } else if (itemId == R.id.action_scan) {
            startScan();
            return true;
        } else if (itemId == R.id.action_stop) {
            stopScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init listview
        deviceListView = (ListView) findViewById(R.id.list);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                ScannedDevice item = mDeviceAdapter.getItem(position);
                if (item != null) {
                    Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                    BluetoothDevice selectedDevice = item.getDevice();
                    intent.putExtra(DeviceActivity.EXTRA_BLUETOOTH_DEVICE, selectedDevice);
                    //startActivity(intent);

                    // stop before change Activity
                    //stopScan();
                }
            }
        });

        stopScan();
    }


    void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            //mBTAdapter.startLeScan(this);
            startBleScan();
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            //mBTAdapter.stopLeScan(this);
            stopBleScan();
            if(mDeviceAdapter != null){
                mDeviceAdapter.clear();
                mDeviceAdapter.notifyDataSetChanged();
            }
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

    /**
     * Start the bluetooth low energy scan.
     */
    private boolean startBleScan() {
        boolean isSuccess = true;

        // Scan filter for iBeacon.
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(0x004c, new byte[] {});
        SCAN_FILTER_LIST.add(builder.build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mScanCallback = new ScanCallback() {
                @Override
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                public void onScanResult(final int callbackType,
                                         final ScanResult result) {
                    // Print out the RSSI value from result
                    Log.d("ECRT_Tx", "onScanResult RSSI: "+result.getRssi());
                    try {
                        // Print out the rssi value
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDeviceAdapter.update(result.getDevice(), result.getRssi(),
                                        result.getScanRecord());
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onScanFailed(final int errorCode) {
                    Log.d("ECRT_ScanFailed", "errorCode: "+errorCode);
                }
            };

            // There is no immediate value telling if scan is failed or not
            if (null != mBTAdapter.getBluetoothLeScanner()) {
                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scanSettings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                            .build();
                }
                mBTAdapter.getBluetoothLeScanner().startScan(SCAN_FILTER_LIST,
                        scanSettings, mScanCallback);
            } else {
                isSuccess = false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // On some Android device, Bluetooth LE (Android Version < 4.1)
            // scan is unstable. Turning off scan every 3 seconds as
            // a workaround for this issue
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device,
                                     final int rssi,
                                     final byte[] scanRecord) {
                    try {
                        // Print out the rssi value
                        Log.d("ECRT_Tx", "onScanResult RSSI: "+rssi);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDeviceAdapter.update(device, rssi, scanRecord);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            isSuccess = mBTAdapter.startLeScan(mLeScanCallback);
        }

        return isSuccess;
    }

    /**
     * Stop the bluetooth low energy scan.
     */
    private boolean stopBleScan() {
        boolean isSuccess = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (null != mBTAdapter.getBluetoothLeScanner() && null != mScanCallback) {
                mBTAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
            } else {
                isSuccess = false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
            if (null != mLeScanCallback) {
                mBTAdapter.stopLeScan(mLeScanCallback);
            } else {
                isSuccess = false;
            }
        }

        return isSuccess;
    }
}

