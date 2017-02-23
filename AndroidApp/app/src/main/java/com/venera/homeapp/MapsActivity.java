package com.venera.homeapp;

//All the components the app will ues are imported at the beginning.
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import static com.venera.homeapp.R.id.map;

//the main clss, in which everything happens.
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.InfoWindowAdapter {

    //Defining the required component for any and all location activities.
    private LocationManager locationManager;
    //Defining the required component for online location monitoring.
    private LocationListener listener;
    //Defining the required component for any and all Bluetooth activities.
    private BluetoothAdapter mBluetoothAdapter;
    //The bluetooth device the android will connect to.
    private BluetoothDevice mDevice;
    //A string for debugging the bluetooth connection.
    private static final String TAG = "MY_APP_DEBUG_TAG";


    @Override
    //What happens when I open the application is executed in 'onCreate'
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //This screen will use the activity_maps.xml layout.
        setContentView(R.layout.activity_maps);
        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        //Finding the device's bluetooth ID, if any.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG,"Device does not support Bluetooth");
        }
        else {
            //Device supports bluetooth.
            //If Bluetooth isn't on, request user to allow app turn it on using a popup.
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mDevice = device;
                }
            }
            ConnectThread mConnectThread = new ConnectThread(mDevice);
            mConnectThread.start();
        }
    }



    //Open new thread because when the operation finishes it blocks the thread.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            //When the app deals with outside components, the app needs to check that the
            //components work. The app tries a code in 'try', and if it catches an error,
            //the app executes a code in 'catch' which helps in debugging.
            try {
                // Use a temporary object that is later assigned to mmSocket
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, "Unable to connect", connectException);
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    return;
                }
                connectionFailed();
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            ConnectedThread mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }
        // Closes the client socket and causes the thread to finish.
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
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input stream; Using temp objects because member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            int count = 0;
            while (true) {
                try {
                    if (this.mmInStream.available() != 0){
                        bytes = this.mmInStream.read(buffer);
                        for(int i = begin; i < bytes; i++) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                        count++;
                    }
                } catch (IOException|NullPointerException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

     //Indicate that the connection attempt failed and notify the user.
    private void connectionFailed() {
        Toast.makeText(getApplicationContext(), "Unable to connect device",
                Toast.LENGTH_SHORT).show();
        ConnectThread mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();
    }

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
                    try {
                        JSONObject json = (JSONObject) new JSONTokener(writeMessage).nextValue();
                    } catch (JSONException e) {
                        Log.e("JSONObject", "Input was not in JSON format");
                    }
                    break;
            }
        }
    };

     //Manipulates the map once available.
     //This callback is triggered when the map is ready to be used.
     //This is where we can add markers for showing the air pollution.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        GoogleMap mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setInfoWindowAdapter(this);
        //Creating a test circle on our lab.
        Circle Test;
        LatLng test = new LatLng(32.25419, 34.9220);
        CircleOptions circleOptions = new CircleOptions()
                .center(test)
                .clickable(true)
                .radius(20)
                .strokeWidth(0)
                .fillColor(Color.argb(64,0,0,255));

        //Creating an invisible marker in order to create the info window
        final Marker markerTest = mMap.addMarker(new MarkerOptions()
                .position(test)
                .infoWindowAnchor(0,0)
                .alpha(0));


        Test = mMap.addCircle(circleOptions);

        //when the test circle is clicked-
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                markerTest.hideInfoWindow();
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                markerTest.hideInfoWindow();
            }
        });

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle Test) {
                markerTest.showInfoWindow();
            }
        });

    }
    @Override
    public View getInfoWindow(Marker marker) {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.info_window,null,false);
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

}



