package com.example.bipinmanandhar.androcar;

import com.example.bipinmanandhar.androcar.R;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button pairedBtn;
    ListView deviceList;

    // Bluetooth components
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;

    public static String EXTRA_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initializing the UI components
        pairedBtn = (Button) findViewById(R.id.pairedBtn);
        deviceList = (ListView) findViewById(R.id.pairedList);

        // getting bluetooth adapter
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null){
            Toast.makeText(getApplicationContext(), "Bluetooth Device not available", Toast.LENGTH_LONG).show();

            // finish apk
            finish();
        }
        else if (!myBluetooth.isEnabled()){
            // ask user to turn on the bluetooth
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }

        pairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedDevicesList();
            }
        });

    }

    private void pairedDevicesList(){
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0){
            for (BluetoothDevice bt : pairedDevices){
                // get device's name and address
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No paired Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        deviceList.setAdapter(adapter);

        // calling method myListClickListener on click of device
        deviceList.setOnItemClickListener(myListClickListener);
    }


    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
//            System.out.println(info);
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity.
            Intent i = new Intent(MainActivity.this, CarControlActivity.class);

            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };


}
