package com.venera.homeapp;

//All the components the app will ues are imported at the beginning.
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


import static com.venera.homeapp.R.id.map;

//the main class, in which everything happens.
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.InfoWindowAdapter, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    //Defining the required component for receiving location data.
    GoogleApiClient mGoogleApiClient;
    //Defining the required component for any and all location activities.
    LocationManager locationManager;
    //Defining the required component for online location monitoring.
    LocationListener listener;
    //Defining the required component for any and all Bluetooth activities.
    BluetoothAdapter mBluetoothAdapter;
    //The bluetooth device the android will connect to.
    BluetoothDevice mDevice;
    //A string for debugging the bluetooth connection.
    static final String TAG = "MY_APP_DEBUG_TAG";
    //A boolean for telling if the client is ready
    boolean clientReady =false;
    List DBList = new ArrayList<>();
    Location mLastLocation = null;
    LatLng myPosition = null;
    String testString = "{\n" +
            "  \"CO\": 2,\n" +
            "  \"LPG\": 4,\n" +
            "  \"CO2\": 8,\n" +
            "  \"SMOKE\": 16,\n" +
            "  \"N_HEXANE\": 32\n" +
            "}";
    JSONObject testJson =  new JSONObject(testString);
    public static int COv;
    public static int LPGv;
    public static int CO2v;
    public static int SMOKEv;
    public static int N_HEXANEv;
    public MapsActivity() throws JSONException {
    }

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

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
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

    //when connected to Google API client.
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        clientReady = true;
    }
    //when connection to Google API client is suspended.
    @Override
    public void onConnectionSuspended(int i) {
        clientReady = false;
    }


    //Open new thread because when the operation finishes it blocks the thread.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        ConnectThread(BluetoothDevice device) {
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
        //private final OutputStream mmOutStream;
        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            //OutputStream tmpOut = null;
            // Get the input stream; Using temp objects because member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            mmInStream = tmpIn;
            //mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes;
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
                    }
                } catch (IOException|NullPointerException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        /*
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        */
    }

     //Indicate that the connection attempt failed, notify the user and try again.
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
            int begin = msg.arg1;
            int end = msg.arg2;
            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    try {
                        JSONObject json = (JSONObject) new JSONTokener(writeMessage)
                                .nextValue();
                    } catch (JSONException e) {
                        Log.e("JSONObject", "Input was not in JSON format");
                    }
                    break;
            }
        }
    };
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

     //Manipulates the map once available.
     //This callback is triggered when the map is ready to be used.
     //This is where we can add markers for showing the air pollution.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        googleMap.setInfoWindowAdapter(this);
        //Creating a test circle on our school bus station.
        LatLng TestLocation = new LatLng(32.25419, 34.9220);
        googleMap.addCircle(new CircleOptions()
                .center(TestLocation)
                .clickable(true)
                .radius(20)
                .strokeWidth(0)
                //Blue circle a quarter visible.
                .fillColor(Color.argb(64, 0, 0, 255)));

        try {
            COv = testJson.getInt("CO");
            LPGv = testJson.getInt("LPG");
            CO2v = testJson.getInt("CO2");
            SMOKEv = testJson.getInt("SMOKE");
            N_HEXANEv = testJson.getInt("N_HEXANE");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Creating an invisible marker in order to create the info window
        //on the test cir4cle.
        final Marker markerTest = googleMap.addMarker(new MarkerOptions()
                .position(TestLocation)
                .infoWindowAnchor(0,0)
                .alpha(0));
        //
        myCurrentPosition();

        //When the test circle is clicked show his info window.
        googleMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle Test) {
                markerTest.showInfoWindow();
            }
        });
        //When the map is moved hide the test circle's info window.
        googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                markerTest.hideInfoWindow();
            }
        });
        //When the map is clicked hide the test circle's info window.
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                markerTest.hideInfoWindow();
            }
        });



    }
    public void myCurrentPosition (){
        while (true) {
            if (clientReady){
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    myPosition = new LatLng(mLastLocation.getLatitude()
                            ,mLastLocation.getLongitude());
                    /*
                    try {
                        COv = testJson.getInt("CO");
                        LPGv = testJson.getInt("LPG");
                        CO2v = testJson.getInt("CO2");
                        SMOKEv = testJson.getInt("SMOKE");
                        N_HEXANEv = testJson.getInt("N_HEXANE");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    */
                }
            }
        }
    }
    public List getInfo(List list){
        list.clear();
        //Get all the JSON strings from the data base
        //In the meantime-
        Random rnd = new Random();
        String input;
        for (int i =0;i<rnd.nextInt(10);i++){
            input = "{\n" +
                    "  \"phoneid\": \"012345678AA\",\n" +
                    "  \"data\": [\n" +
                    "    {\n" +
                    "      \"gas\": \"CO\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"SMOKE\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"N-HEXANE\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"BENZENE\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"PROPANE\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"O3\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"gas\": \"NH3\",\n" +
                    "      \"concentration\": "+rnd.nextInt(1000)+"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            list.add(input);
        }
        return list;
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
    }
    @Override
    public View getInfoWindow(Marker marker) {
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new MyAdapter(this));

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.info_window,null,false);
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

}


