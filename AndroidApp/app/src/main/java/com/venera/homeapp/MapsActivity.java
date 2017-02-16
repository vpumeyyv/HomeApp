package com.venera.homeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;

import static com.venera.homeapp.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    //Defining the required component for any and all location activities
    private LocationManager locationManager;
    //Defining the required component for online location monitoring
    private LocationListener listener;
    //Defining the required component for any and all Bluetooth activities
    private BluetoothAdapter mBluetoothAdapter;
    //Defining a popup window to turn on Bluetooth
    private Intent enableBtIntent;
    //The bluetooth device the android will connect to
    private BluetoothDevice mDevice;
    //The thread in which the connection will occur.
    private ConnectThread mConnectThread;
    //The thread in which the communication will occur.
    private ConnectedThread mConnectedThread;
    // Get a BluetoothSocket to connect with the given BluetoothDevice.
    // This code below show how to do it and handle the case that the UUID from the device is not found and trying a default UUID.
    // Default UUID
    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //A string for debugging the bluetooth connection
    private static final String TAG = "MY_APP_DEBUG_TAG";
    //Circle popup related

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };


    @Override
    //What happens when I open the application is executed in 'onCreate'
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        //Finding the device's bluetooth ID, if any.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        //If Bluetooth isn't on, request user to allow app turn it on using a popup.
        if (!mBluetoothAdapter.isEnabled()) {
            enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
            }
        }
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();


    }
    //Open new thread because when the operation finishes it blocks the thread.
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            BluetoothSocket tmp = null;
            mmDevice = device;

            //When the app deals with outside components, the app needs to check that the components work.
            //The app 'tries' the components, and if they'e not working (null) it returns errors.
            try {
                // Use the UUID of the device that got discovered
                if (mmDevice != null) {
                    Log.i(TAG, "Device Name: " + mmDevice.getName());
                    Log.i(TAG, "Device UUID: " + mmDevice.getUuids()[0].getUuid());
                    tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());

                } else Log.d(TAG, "Device is null.");
            } catch (NullPointerException e) {
                Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + device.getName());
                try {
                    tmp = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
            }
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                //If closing fails, returns error
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        // Closes the client socket and causes the thread to finish.
        //If closing fails, returns error
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer; // mmBuffer store for the stream
        private Handler mHandler; // handler that gets info from Bluetooth service

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(0, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        //Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

     //Manipulates the map once available.
     //This callback is triggered when the map is ready to be used.
     //This is where we can add markers for showing the air pollution.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        //Creating a test circle on our lab.
        Circle Test;
        LatLng test = new LatLng(32.2550, 34.9216);
        CircleOptions circleOptions = new CircleOptions()
                .center(test)
                .clickable(true)
                .radius(400)
                .strokeWidth(0)
                .fillColor(Color.argb(64,0,0,255));

        //Creating an invisible marker in order to create the info window
        final Marker perth = mMap.addMarker(new MarkerOptions()
                .position(test)
                .alpha(0)
                .title("Testing...")
                .snippet("It seems to work!"));

        Test =mMap.addCircle(circleOptions);

        //when the test circle is clicked-
        final Random rand = new Random();

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                perth.hideInfoWindow();
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                perth.hideInfoWindow();
            }
        });

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle Test) {
                Test.setFillColor(Color.argb(64,rand.nextInt(255),rand.nextInt(255),rand.nextInt(255)));
                perth.showInfoWindow();
            }
        });

    }

}



