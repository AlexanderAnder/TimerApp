package com.example.timer;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 10000;
private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 42;
private static final int MY_PERMISSIONS_REQUEST_ACCESS_BLUETOOTH =25;
private Timer time;
private BluetoothAdapter bluetoothAdapter;
private BluetoothLeScanner bluetoothLeScanner;
private Handler handler = new Handler();
private boolean mScanning;
private boolean mConnected;
private String deviceMac = "E8:F8:F1:7D:FC:5B";
private UUID deviceUUID = UUID.fromString("713d0000-503e-4c75-ba94-3148f18d941e") ;
private UUID characteristicUUID = UUID.fromString("713d0003-503e-4c75-ba94-3148f18d941e") ;
private BluetoothManager bluetoothManager;
private BluetoothGatt myGatt;
private Set<BluetoothDevice> bluetoothDevices = new HashSet<>();
private  BluetoothGattCharacteristic characteristic;
private boolean characteristicReady = false;
private BluetoothGattService service;
private CountDownTimer bleTiming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        final TextView timeText = findViewById(R.id.timetext);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                createDialog();


            }
        });
        View view = findViewById(R.id.main_content_view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               if(getSupportFragmentManager().findFragmentByTag("Settings")!=null){
                   onBackPressed();
               }


            }
        });
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            if(areLocationServicesEnabled(this)) {
                Handler permissionHandler = new Handler();

                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(this, R.string.ble_not_supported, LENGTH_SHORT).show();
                    finish();
                }
            }}
        if(!hasBlePermissions()){
           requestBlePermissions(this,MY_PERMISSIONS_REQUEST_ACCESS_BLUETOOTH);

        }
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner= bluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if(getSupportFragmentManager().findFragmentByTag("Settings")==null) {
                Fragment newFragment = new SettingsFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.settingsFrame, newFragment, "Settings");
                transaction.addToBackStack(null);
                transaction.commit();
            }else{
                onBackPressed();

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.alert_input, null);
        builder.setView(dialogView);
        final EditText hourText =  dialogView.findViewById(R.id.hours);
        final EditText minuteText =  dialogView.findViewById(R.id.minutes);
        final EditText secondsText =  dialogView.findViewById(R.id.seconds);
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String notificationTimeString = mSharedPreferences.getString(getString(R.string.notification_time)+"Timer","10");
        final int notificationTime = Integer.parseInt(notificationTimeString);
        final String notificationStrengthString = mSharedPreferences.getString(getString(R.string.seekPow)+"Timer","10");
        final int notificationStrength = Integer.parseInt(notificationStrengthString);
        Log.d("devilTrigger", String.valueOf(notificationStrength));

        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(hourText.getText().toString().equals("")||
                        minuteText.getText().toString().equals("")||
                        secondsText.getText().toString().equals("")){

                }else{
                long hours = (Long.parseLong(hourText.getText().toString()));
                long minutes = (Long.parseLong(minuteText.getText().toString()));
                long seconds = (Long.parseLong(secondsText.getText().toString()));
                long actualTime = timeInMilliseconds(hours,minutes,seconds);
                if(time!= null){
                    time.clearTimer(false);
                    if (bleTiming != null) {
                        bleTiming.cancel();
                    }
                }
                time = new Timer (actualTime,1000, (TextView) findViewById(R.id.timetext));
                    Log.d("coldBlows", String.valueOf(bluetoothDevices.size()));
                    Log.d("Nero","Watch this"+characteristicReady);
                    if (characteristicReady) {
                        bleTimer(actualTime, notificationTime, notificationStrength);
                    }

            }}
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        if(!characteristicReady){
            Toast.makeText(this,"No Ble connection established, try to connect to a device from the settings",Toast.LENGTH_LONG).show();
        }
        builder.show();
    }

    public long timeInMilliseconds(long hour,long minute, long second){
        long hourMin = hour*60;
        long minSec = (hourMin+minute) * 60;
        return (minSec+second) *1000;
    }


    public void scanLeDevice(final boolean enable){

        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(MainActivity.this,"Bluetooth is not enabled",Toast.LENGTH_LONG).show();
            return;
        }
        if(!areLocationServicesEnabled(this)){
            Toast.makeText(MainActivity.this,"Gps is not enabled",Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(deviceMac).build();
        filters.add(filter);
        final ScanCallback scanCallback = new ToothScanCallback();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothLeScanner.startScan(filters,settings,scanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }

    }

    private void connectDevice(BluetoothDevice device) {
        GattClientCallback gattClientCallback = new GattClientCallback();
        myGatt = device.connectGatt(this, false, gattClientCallback);
        if(myGatt.connect()) {
            Toast.makeText(MainActivity.this, "Device found", LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this, "No Ble Device found, please try again", LENGTH_SHORT).show();
        }
        Log.d("GattOn", String.valueOf(myGatt!=null));
    }

    public void disconnectGattServer() {
        Log.d("gattClose","disconnected");
        mConnected = false;
        characteristicReady = false;
        if (myGatt != null) {
            myGatt.disconnect();
            myGatt.close();
        }
       // Toast.makeText(MainActivity.this,"Disconnected from server",LENGTH_SHORT).show();
    }



    private void writeCharacteristic(String message){
            sendMessage(message);
            Log.d("charaPoll", characteristic.getStringValue(0));
            Handler zero = new Handler();
            zero.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMessage("00000000");
                    Log.d("charaPoll", characteristic.getStringValue(0));
                }
            },2000);



        }
        //  Toast.makeText(this, "Service not ready yet", LENGTH_SHORT).show();


    private void bleTimer(final long finishTime, long intervall, final int power){
            bleTiming= new CountDownTimer(finishTime,intervall*60000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("timeRatio", String.valueOf(millisUntilFinished/finishTime));
                Log.d("timeMillis", String.valueOf(millisUntilFinished/finishTime));
                Log.d("timefinsh", String.valueOf(finishTime));
                int remainingPower = 1;
                if ((double)millisUntilFinished /(double) finishTime >= 0.8) {
                    remainingPower = 1;
                } else if ((double)millisUntilFinished /(double) finishTime >= 0.5){
                    remainingPower = 2;
            }else if((double)millisUntilFinished/(double)finishTime >= 0.2) {
                    remainingPower = 3;
                }else if((double)millisUntilFinished/(double)finishTime < 0.2){
                    remainingPower = 4;

                }

                writeCharacteristic(engines(remainingPower,blePower(power)));

            }

            @Override
            public void onFinish() {
                writeCharacteristic("FFFFFFFF");

            }
        }.start();


    }
    public void connectToDevice(){
        scanLeDevice(true);
        countDownToConnection();
        characteristicReady = true;

    }
    private void countDownToConnection(){
        Handler delay = new Handler();
        delay.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(bluetoothDevices.size()==0){
                    return;
                }else {
                    Iterator<BluetoothDevice> iter = bluetoothDevices.iterator();
                    connectDevice(iter.next());
            }}
        },4000);
    }

    private String blePower(int power){
        String zeroPower = "0";
        Log.d("devilTrigger", String.valueOf(power));
    if(power > 0 && power <25){
        return "9";
    }else if (power <50){
       return "11";
    }else if (power < 75){
       return "C";
    }else if(power >75){
        return "F";
    }
        return zeroPower;
    }

    private String engines(int status, String power){
        switch (status){
            case 1 :
                Log.d("blePower",power+power+"000000");
                return "000000" + power +power ;
            case 2:
                return power + power +"0000"+power+power;
            case 3:
                return power+power+power+power+"00"+power+power;
            case 4:
                Log.d("blePower",power +power +power +power +power +power +power +power);
                return power +power +power +power +power +power +power +power;
             default: return "00000000";
        }
    }

    private void sendMessage(String message) {
        if(!characteristicReady){
            time.clearTimer(true);
            return;
        }
        if (!mConnected ) {
            return;
        }
        characteristic = service.getCharacteristic(characteristicUUID);
        if (characteristic == null) {
            Log.d("noChara","fuck you");
            disconnectGattServer();
            return;
        }

        byte[] messageBytes = hexStringToByteArray(message);

        characteristic.setValue(messageBytes);
        boolean success = myGatt.writeCharacteristic(characteristic);
        if (success) {
            Log.d("shouldWrite","Write?");
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d("gattFail","fail");
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("gattNoSuccess","fail");
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                myGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("gattDisconnected","fail");
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            service = gatt.getService(deviceUUID);
            Log.d("servicePoll", String.valueOf(service!=null) +" "+ (service.getCharacteristic(characteristicUUID)!=null));


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("writeSuccess","Characteristic written successfully");
            } else {
                Log.d("writeSuccess","Characteristic write unsuccessful, status: " + status);
                disconnectGattServer();
            }
        }

    }


    private class ToothScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e("hui","BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            bluetoothDevices.add(device);
        }

    }



    public boolean hasBlePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }
    public void requestBlePermissions(final Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                requestCode);
    }
    public boolean areLocationServicesEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}



