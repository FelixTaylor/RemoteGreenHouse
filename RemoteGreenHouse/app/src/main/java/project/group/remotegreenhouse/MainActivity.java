package project.group.remotegreenhouse;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Resources res;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothSocket bluetoothSocket;
    private volatile boolean stopWorker;
    private Button btnConnect;
    private byte[] readBuffer;
    private int readBufferPosition, iLimiterPosition;
    private static final int REQUEST_ENABLE_BT = 1;
    private InputStream inputStream;
    private long pastMillis;
    private OutputStream outputStream;
    private SeekBar sbLEDlevel;
    private Set<BluetoothDevice> pairedDevices;
    private Switch swBluetooth;
    private String TAG = "MainActivity";
    private TextView twLEDlevel, twValTemperatur, twValDruck, twValHelligkeit, twValLuftfeuchte, twValBodenfeuchte;
    private Thread workerThread;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = getResources();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(mReceiver, filter);

        sbLEDlevel          = findViewById(R.id.sb_LEDlevel);
        twLEDlevel          = findViewById(R.id.tw_LEDlevel);
        twValTemperatur     = findViewById(R.id.val_Temperatur);
        twValDruck          = findViewById(R.id.val_Druck);
        twValHelligkeit     = findViewById(R.id.val_Helligkeit);
        twValLuftfeuchte    = findViewById(R.id.val_Luftfeuchte);
        twValBodenfeuchte   = findViewById(R.id.val_Bodenfeuchte);
        swBluetooth         = findViewById(R.id.sw_BluetoothONOFF);
        btnConnect          = findViewById(R.id.btn_Connect);

        //Initial Conditions
        sbLEDlevel.setMax(100);
        sbLEDlevel.setProgress(0);
        twLEDlevel.setText(Integer.toString(0));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = bluetoothAdapter.getBondedDevices();

        if(bluetoothAdapter.isEnabled()){
            swBluetooth.setChecked(true);
        }
        else{
            swBluetooth.setChecked(false);
        }

        //Listener
        swBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    activateBluetooth();
                }
                else {
                    deactivateBluetooth();
                }
            }
        });
        sbLEDlevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                twLEDlevel.setText(Integer.toString(sbLEDlevel.getProgress()));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String sendString = "a" + Integer.toString(sbLEDlevel.getProgress());
                serialWrite(sendString);
            }
        });
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_connecting),Toast.LENGTH_SHORT).show();
                connectToPairedDevice();
            }
        });
    }



    /*--------------------------
      -----Zusatzfunktionen-----
      --------------------------*/
    private void activateBluetooth(){
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        while(!bluetoothAdapter.isEnabled()){
            //wait until bt is enabled
        }
        Toast.makeText(getApplicationContext(), "Bluetooth ON", Toast.LENGTH_SHORT).show();
    }

    private void deactivateBluetooth(){
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
        while(bluetoothAdapter.isEnabled()){
            //wait until bt is disabled
        }
        Toast.makeText(getApplicationContext(), "Bluetooth OFF", Toast.LENGTH_SHORT).show();
    }

    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.i("Log", "Discoverable ");
    }

    private void connectToPairedDevice(){
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

                if (bondedDevices.size() > 0) {
                    for (BluetoothDevice mDevice : pairedDevices) {
                        if (mDevice.getName().equals("HC-05")) {
                            device = mDevice;
                        }
                        ParcelUuid[] uuids = device.getUuids();
                        try {
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                        } catch (IOException e) { Log.d(TAG,"Could not create Socket");
                        }
                        try {
                            bluetoothSocket.connect();
                        } catch (IOException e) {Log.d(TAG,"Could not connect");
                        }
                        try {
                            outputStream = bluetoothSocket.getOutputStream();
                        } catch (IOException e) {Log.d(TAG,"Could not create Outputstream");
                        }
                        try {
                            inputStream = bluetoothSocket.getInputStream();
                        } catch (IOException e) {Log.d(TAG,"Could not create Inputstream");
                        }
                        beginListenForData();
                        serialWrite("a000");
                    }
                } else {
                    Log.e("error", "Bluetooth is disabled.");
                }
            }
        }
    }

    public void serialWrite(String s) {
        try {
            outputStream.write(s.getBytes());
        }catch(IOException e){Log.i(TAG,"could not send String");}
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_connected),Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_disconnected),Toast.LENGTH_SHORT).show();
            }
        }
    };

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        pastMillis = System.currentTimeMillis();

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable(){
            public void run(){
                while(!Thread.currentThread().isInterrupted() && !stopWorker){
                    if(System.currentTimeMillis() - pastMillis > 5000){
                        serialWrite("h");
                        pastMillis = System.currentTimeMillis();
                    }
                    try{
                        int bytesAvailable = inputStream.available();
                        if(bytesAvailable > 0){
                            byte[] packetBytes = new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++){
                                byte b = packetBytes[i];
                                if(b == delimiter){
                                    final byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable(){
                                        public void run(){
                                            for(int i = 0; i<data.length(); i++){
                                                if(data.substring(i,i+1).equals("h")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("l")){
                                                            iLimiterPosition = j;
                                                        }
                                                    }
                                                    twValHelligkeit.setText(
                                                            res.getString(R.string.dim_helligkeit,
                                                            data.substring(i+1,iLimiterPosition))
                                                    );
                                                }
                                                if(data.substring(i,i+1).equals("l")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("p")){
                                                            iLimiterPosition = j;
                                                        }
                                                    }
                                                    twValLuftfeuchte.setText(
                                                            res.getString(R.string.dim_feuchtigkeit,
                                                            data.substring(i+1,iLimiterPosition))
                                                    );
                                                }
                                                if(data.substring(i,i+1).equals("p")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("t")){
                                                            iLimiterPosition = j;
                                                        }
                                                    }
                                                    twValDruck.setText(
                                                            res.getString(R.string.dim_druck,
                                                            data.substring(i+1,iLimiterPosition))
                                                    );
                                                }
                                                if(data.substring(i,i+1).equals("t")){
                                                    twValTemperatur.setText(
                                                            res.getString(R.string.dim_temperatur,
                                                            data.substring(i+1,data.length()))
                                                    );
                                                }
                                            }
                                        }
                                    });
                                }
                                else{
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex){
                        Log.d(TAG,"beginListeningForData: catch");
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}