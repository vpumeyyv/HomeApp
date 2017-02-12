package com.venera.homeapp;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //
    private GoogleMap mMap;
    //Defining the required component for any and all location activities
    private LocationManager locationManager;
    //Defining the required component for online location monitoring
    private LocationListener listener;
    //Defining the required component for any and all Bluetooth activities
    private BluetoothAdapter mBluetoothAdapter;
    //Defining a popup window to turn on Bluetooth
    private Intent enableBtIntent;



    @Override
    //What hapens when I open the application is executed in 'onCreate'
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Finding the device's bluetooth ID, if any.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        //If Bluetooth isn't on, request user to allow app turn it on using a popup.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        
        //Get all of the allready paired bluetooth devices.
        //This is usefull if it's not the first time using the app and the user's phone has connected previously to the Arduino.
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        //If there's atleast one paired bluetooth device, it may be the arduino we seek.
        if (pairedDevices.size() > 0) {
        // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                //Get's all of the names and MAC addresses of the paired devices. (MAC=Media Access Control)
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                
                if (deviceName.contains(bluetooth_component)) {
                       
                }
            }
        }
        

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);


    }
}

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-
        00805f9b34fb");
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) { }
            mmSocket = tmp;
        }
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } 
            catch (IOException connectException) {
                try {
                    mSocket.close();
                } catch (IOException closeException) { }
                return;
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

