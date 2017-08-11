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
 * Created by Bipin Manandhar on 6/26/2017.
 */

public class CarControlActivity extends AppCompatActivity implements SensorEventListener {

    Button forwardBtn, backwardBtn, rightBtn, leftBtn, headlight, rearlight;
    ImageView forwardBtnn, backwardBtnn,hornBtnn,headlightBtnn,rearlightBtnn;
    String address = null;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    boolean head_on = false;
    boolean rear_on = false;

    boolean forwardBtnnHold = false;
    boolean backwardBtnnHold = false;

    boolean readyForRightTurn = false;
    boolean readyForLeftTurn = false;

    // SPP UUID.
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Sensor compontents
    private TextView myTV;
    private Sensor mySensor;
    private SensorManager manager;

    private int latestSentData = 0;

    // for debugging
    private static final String TAG = "CarControlActivity";
    private int obstacleMessage = 0;

    byte[] readBuffer;
    volatile boolean stopWorker;
    int counter;
    int readBufferPosition;
    Thread workerThread;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); // receive the address of the bluetooth device

        setContentView(R.layout.activity_carcontrol);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        // initializing the UI components
/*        forwardBtn = (Button) findViewById(R.id.forwardBtn);
        backwardBtn = (Button) findViewById(R.id.backwardBtn);
        leftBtn = (Button) findViewById(R.id.leftBtn);
        rightBtn = (Button) findViewById(R.id.rightBtn);
        headlight = (Button) findViewById(R.id.headlight);
        rearlight = (Button) findViewById(R.id.rearlight);*/
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

        new ConnectBT().execute();

//        forwardBtnn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (btSocket != null){
//                    try {
//                        btSocket.getOutputStream().write("1".toString().getBytes());
//                    }
//                    catch (IOException e){
//                        Log.v(TAG, "forwardbtn");
//                        msg("ERROR");
//                    }
//                }
//            }
//        });

        forwardBtnn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        forwardBtnnHold = true;
//                        if (readyForRightTurn){
//                            sendData("3");
//                        }
//                        else if (readyForLeftTurn){
//                            sendData("4");
//                        }
//                        else{
//                            sendData("1");
//                        }
                        break;
                    case  MotionEvent.ACTION_UP:
                        forwardBtnnHold = false;
                        sendData("0");
                        latestSentData = 0;
                        break;
                }

                return true;
            }
        });

//        backwardBtnn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (btSocket != null){
//                    try {
//                        btSocket.getOutputStream().write("2".toString().getBytes());
//                    }
//                    catch (IOException e){
//                        Log.v(TAG, "forwardbtn");
//                        msg("ERROR");
//                    }
//                }
//            }
//        });

        backwardBtnn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        backwardBtnnHold = true;
//                        if (readyForRightTurn){
//                            sendData("5");
//                        }
//                        else if (readyForLeftTurn){
//                            sendData("6");
//                        }
//                        else{
//                            sendData("2");
//                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        backwardBtnnHold = false;
                        sendData("0");
                        latestSentData = 0;
                        break;
                }
                return true;
            }
        });

        /*rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btSocket != null){
                    try {
                        btSocket.getOutputStream().write("3".toString().getBytes());
                    }
                    catch (IOException e){
                        Log.v(TAG, "rightbtn");
                        msg("ERROR");
                    }
                }
            }
        });

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btSocket != null){
                    try {
                        btSocket.getOutputStream().write("4".toString().getBytes());
                    }
                    catch (IOException e){
                        Log.v(TAG, "leftbtn");
                        msg("ERROR");
                    }
                }
            }
        });*/

        headlightBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                head_on = !head_on;
                if (btSocket != null){
                    try {
                        btSocket.getOutputStream().write("7".toString().getBytes());
                        if(head_on)
                            headlightBtnn.setImageResource(R.drawable.head_light_on);
                        else
                            headlightBtnn.setImageResource(R.drawable.head_light_off);
                    }
                    catch (IOException e){
                        Log.v(TAG, "headlight");
//                        msg("ERROR");
                    }
                }
            }
        });

        rearlightBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rear_on = !rear_on;
                if (btSocket != null){
                    try {
                        btSocket.getOutputStream().write("8".toString().getBytes());
                        if(rear_on)
                            rearlightBtnn.setImageResource(R.drawable.rear_light_on);
                        else
                            rearlightBtnn.setImageResource(R.drawable.rear_light_off);
                    }
                    catch (IOException e){
                        Log.v(TAG, "rearlight");
//                        msg("ERROR");
                    }
                }
            }
        });

        hornBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData("9");
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
//        myTV.setText("X: " + java.lang.Math.floor(sensorEvent.values[0])
//                    + "\nY: " + java.lang.Math.floor(sensorEvent.values[1])
//                    + "\nZ: " + java.lang.Math.floor(sensorEvent.values[2]));

//        receiveData();

        if (java.lang.Math.floor(sensorEvent.values[1]) >= 5){
            readyForRightTurn = true;
            if (forwardBtnnHold && latestSentData != 3){
                sendData("3");
                latestSentData = 3;
//                Toast.makeText(getApplicationContext(), "right", Toast.LENGTH_SHORT).show();
            }
            else if (backwardBtnnHold && latestSentData != 5){
                sendData("5");
                latestSentData = 5;
//                Toast.makeText(getApplicationContext(), " back right", Toast.LENGTH_SHORT).show();
            }
        }
        else if (java.lang.Math.floor(sensorEvent.values[1]) <= -5){
            readyForLeftTurn = true;
            if (forwardBtnnHold && latestSentData != 4){
                sendData("4");
                latestSentData = 4;
//                Toast.makeText(getApplicationContext(), "left", Toast.LENGTH_SHORT).show();
            }

            else if (backwardBtnnHold && latestSentData != 6){
                sendData("6");
                latestSentData = 6;
//                Toast.makeText(getApplicationContext(), "left back", Toast.LENGTH_SHORT).show();
            }
        }
        else {

            if (forwardBtnnHold && latestSentData != 1){
                sendData("1");
                latestSentData = 1;
//                Toast.makeText(getApplicationContext(), "forward", Toast.LENGTH_SHORT).show();
            }
            else if (backwardBtnnHold && latestSentData != 2){
                sendData("2");
                latestSentData = 2;
//                Toast.makeText(getApplicationContext(), "backward", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not necessory for now
    }

    // method for handling the Serial data from arduino to android
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
                        int bytesAvailable = btSocket.getInputStream().available();
                        if (bytesAvailable > 0){
                            byte[] packetBytes = new byte[bytesAvailable];
                            btSocket.getInputStream().read(packetBytes);
                            for(int i=0; i<bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == delimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
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
                    catch (IOException ex){
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

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
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();

                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);

                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);

                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            catch ( IOException e){
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);

            if (!ConnectSuccess){
                msg("Connection Failed. Is it a SSP Bluetooth? Try again.");
                finish();
            }
            else {
                msg("Connected.");
                isBtConnected = true;
                listenForData();
            }
        }
    }

    private void msg(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void sendData(String i){
        if (btSocket != null){
            try {
                btSocket.getOutputStream().write(i.toString().getBytes());
            }
            catch (IOException e){
                Log.v(TAG, "Error: " + i);
//                        msg("ERROR");
            }
        }
    }

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
