package com.example.bipinmanandhar.androcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.IOException;
import java.util.UUID;
import android.os.Handler;

import static android.graphics.Color.RED;

/**
 * This class is for the interface for controlling the android car.
 * implementing SensorEventListener in this class is for listening the event from accelerometer sensor
 */

/**
 * 1 for forward
 * 2 for backward
 * 3 for right turn
 * 4 for left turn
 * 5 for right reverse
 * 6 for left reverse
 * 7 for head light
 * 8 for tail light
 * 9 for horn
 * 0 to stop the car
 */

public class CarControlActivity extends AppCompatActivity implements SensorEventListener {
    // imageView for buttons used in the interface for good apperance
    ImageView forwardBtnn, backwardBtnn,hornBtnn,headlightBtnn,rearlightBtnn;
    // this variable is for storing the address of bluetooth model we are connected to.
    String address = null;

    // BluetoothAdapter for using the bluetooth device from the android device.
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    // boolean value for storing the state of head light and tail light
    boolean head_on = false;
    boolean rear_on = false;

    // for checking whether the button is being hold or not
    boolean forwardBtnnHold = false;
    boolean backwardBtnnHold = false;

    // SPP UUID uuid for bluetooth model. only work for bluetooth model HC-05 and HC-06.
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Text view for displaying the error message on obstacle detection
    private TextView myTV;

    // Sensor components
    private Sensor mySensor;
    private SensorManager manager;

    // store the last data that is sent to arduino through bluetooth model
    private int latestSentData = 0;

    // for debugging
    private static final String TAG = "CarControlActivity";

    // the data from arduino is received in byte for each character in the message.
    // storing each byte to readBuffer character by character
    byte[] readBuffer;
    volatile boolean stopWorker;

    // variable for representing the index in readBuffer
    int readBufferPosition;
    // Thread for converting the received stream of data to string in background
    Thread workerThread;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // creating Intent for receiving the data passed from previous activity.
        // in this case the bluetooth model address
        Intent newint = getIntent();

        // receive the address of the bluetooth device using newint Intent
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);

        setContentView(R.layout.activity_carcontrol);

        // getting the bluetooth adapter build inside the android phone
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        // initializing the components for buttons
        backwardBtnn = (ImageView)findViewById(R.id.Left_Btn);
        forwardBtnn = (ImageView)findViewById(R.id.Right_Btn);
        hornBtnn =(ImageView)findViewById(R.id.horn_btn);
        headlightBtnn = (ImageView)findViewById(R.id.headLight_btnn);
        rearlightBtnn = (ImageView)findViewById(R.id.rearLight_btnn);

        // creating sensor manager
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Accelerometer Sensor
        mySensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // register sensor listener
        manager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_FASTEST);

        // init accelerometer textview
        myTV = (TextView) findViewById(R.id.textView);

        // executing the AsyncTask for checking the bluetooth connection status in background
        new ConnectBT().execute();

        // What to do if forward button is being hold
        forwardBtnn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    // case: button is being hold
                    case MotionEvent.ACTION_DOWN:
                        forwardBtnn.setImageResource(R.drawable.forward_btnn);
                        forwardBtnnHold = true;
                        break;
                    // case: button is not being pressed now
                    case  MotionEvent.ACTION_UP:
                        forwardBtnn.setImageResource((R.drawable.forward_btn_idle));
                        forwardBtnnHold = false;
                        // send data to stop the andro car
                        // 0 is for stopping the andro car
                        sendData("0");
                        // store the latest value sent to arduino
                        latestSentData = 0;
                        break;
                }
                return true;
            }
        });

        // what to do if backward button is being hold
        backwardBtnn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        backwardBtnn.setImageResource(R.drawable.backward_btn);
                        backwardBtnnHold = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        backwardBtnn.setImageResource(R.drawable.backward_btn_idle);
                        backwardBtnnHold = false;
                        sendData("0");
                        latestSentData = 0;
                        break;
                }
                return true;
            }
        });

        // what to do if headlight is pressed
        headlightBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                head_on = !head_on;
                if (btSocket != null){
                    try {
                        // 7 is for turning headlight on and off
                        btSocket.getOutputStream().write("7".toString().getBytes());
                        // if head_on i.e. headlight is on change the image of button according to status of headlight
                        if(head_on)
                            headlightBtnn.setImageResource(R.drawable.head_light_on);
                        else
                            headlightBtnn.setImageResource(R.drawable.head_light_off);
                    }
                    catch (IOException e){
                        Log.v(TAG, "headlight");
                    }
                }
            }
        });

        // what to do if taillight is pressed
        rearlightBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rear_on = !rear_on;
                if (btSocket != null){
                    try {
                        // 8 is for turning tail light on and off
                        btSocket.getOutputStream().write("8".toString().getBytes());
                        if(rear_on)
                            rearlightBtnn.setImageResource(R.drawable.rear_light_on);
                        else
                            rearlightBtnn.setImageResource(R.drawable.rear_light_off);
                    }
                    catch (IOException e){
                        Log.v(TAG, "rearlight");
                    }
                }
            }
        });

        // what to do if horn is pressed
        // 9 is for horn
        hornBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData("9");
            }
        });
    }

    // function for listening to change in the sensor value and performing some action
    // note: only change on the sensor value is responsible for changing the motor state of andro-car
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // sensor data is returned in the form of following axis: [x, y, z]
        // x passes from left to right of the screen: [-]
        // y passes from top to bottom of the screen: [|]
        // z passes form back cover to screen of the phone: -|-

        // checking the value of y axis measured by sensor
        // if mobile phone is tilted to right, right turn the andro-car or right reversed according to button hold by user
        if (java.lang.Math.floor(sensorEvent.values[1]) >= 5){
            // check the value of latestSentData for latest sent data so that it does not get sent again
            // if forward button is being pressed
            if (forwardBtnnHold && latestSentData != 3){
                // 3 is for right turn
                sendData("3");
                latestSentData = 3;
            }
            // if backward button is being pressed
            else if (backwardBtnnHold && latestSentData != 5){
                // 5 is for right reversed turn
                sendData("5");
                latestSentData = 5;
            }
        }
        // if mobile phone is tilted to left, left turn the andro-car or left reversed according to button hold by user
        else if (java.lang.Math.floor(sensorEvent.values[1]) <= -5){
            // if forward button is being pressed
            if (forwardBtnnHold && latestSentData != 4){
                // 4 is for left turn
                sendData("4");
                latestSentData = 4;
            }
            // if backward button is being pressed
            else if (backwardBtnnHold && latestSentData != 6){
                // 6 is for left reversed turn
                sendData("6");
                latestSentData = 6;
            }
        }
        // if mobile is not tilted sufficiently in right or left direction
        else {
            // if forward button is being pressed and the latest sent data is not 1, move car in forward direction
            if (forwardBtnnHold && latestSentData != 1){
                // 1 is for moving the andro-car forward
                sendData("1");
                latestSentData = 1;
            }
            // if backward button is being pressed and the latest sent data is not 2, move car in backward direction
            else if (backwardBtnnHold && latestSentData != 2){
                // 2 is for moving the andro-car backward
                sendData("2");
                latestSentData = 2;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not necessary for now
    }

    // method for handling the Serial data from arduino to android i.e. converting the byte data from arduino to string
    void listenForData(){
        final Handler handler = new Handler();
        final byte delimiter = 10; // ASCII code for newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker){
                    try {
                        // get total number of bytes of available data from arduino
                        int bytesAvailable = btSocket.getInputStream().available();
                        // if byte available is greater than 0 i.e. data is being sent from arduino
                        if (bytesAvailable > 0){
                            // creating another byte array equal to number of bytes of data sent from arduino
                            byte[] packetBytes = new byte[bytesAvailable];
                            // using bt connection store the byte of information sent from arduino to packetBytes variable
                            btSocket.getInputStream().read(packetBytes);
                            // populate the content of new byte array readBuffer by content of packetBytes
                            for(int i=0; i<bytesAvailable; i++){
                                byte b = packetBytes[i];
                                // if the end of character is reached( delimiter is found ) i.e. end of the message
                                if(b == delimiter){
                                    // create a new byte array encodedBytes with the size equal to the position where delimiter is found
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    // copy the array element from readBuffer to encodedBytes now in array of characters
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // convert the array of characters to a single string
                                    final String data = new String(encodedBytes);
                                    readBufferPosition = 0;

                                    // this is executed when the thread is completed sucessfully
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // display the message received from arduino in android
                                            myTV.setText(data);
                                            myTV.setTextColor(RED);
                                        }
                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    // if ioexception is received stop running the thread
                    catch (IOException ex){
                        stopWorker = true;
                    }
                }
            }
        });
        // starting the thread created above
        workerThread.start();
    }

    /**
     * class for checking the connection of bluetooth in background
     */
    private class ConnectBT extends AsyncTask<Void, Void, Void>
    // UI threads
    {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute(){

        }

        @Override
        protected Void doInBackground(Void... devices){
            try{
                // if bluetooth socket is not established and bluetooth is not connected with bluetooth module
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();

                    // get the bluetooth device this given address
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);

                    // create the connection with the bluetooth device with the given address
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);

                    // disable the discovery of android bluetooth so that no one is sending data to android except andro-car
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            // if some exception is being raised
            catch ( IOException e){
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);

            // if connection is not success, may be bluetooth of targeted device is not enabled or the bluetooth device is not HC-05, HC-06 bluetooth module
            if (!ConnectSuccess){
                msg("Connection Failed. Is it a SSP Bluetooth? Try again.");
                // close this activity if connection is not established successfully
                finish();
            }
            // if connection is established successfully with the target device
            else {
                msg("Connected.");
                isBtConnected = true;
                // start listening for data from arduino
                listenForData();
            }
        }
    }

    // function for creating toast message
    private void msg(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    // function for send data from android to arduino
    private void sendData(String i){
        if (btSocket != null){
            try {
                btSocket.getOutputStream().write(i.toString().getBytes());
            }
            catch (IOException e){
                Log.v(TAG, "Error: " + i);
            }
        }
    }

    // default sensor function for what to do in case of activity in stop, resume or in pause mode
    @Override
    protected void onStop() {
        manager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }
}
