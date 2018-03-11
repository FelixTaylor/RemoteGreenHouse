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
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName(); // TAG for logging data
    private static final int REQUEST_ENABLE_BT = 1;         // Code for Enable_Request
    private Resources res;                                  // Using resources directory
    private BluetoothAdapter bluetoothAdapter;              // Local bluetooth adapter
    private BluetoothDevice device;                         // Bluetooth device
    private BluetoothSocket bluetoothSocket;                // Bluetooth communication Socket
    private volatile boolean stopWorker;                    // Variable to stop the communication thread
    private boolean b_isInitialized;                        // Set to true, if state got initialized data from arduino
    private Button btn_bt_connect;                          // Button to connect to coupled bluetooth device
    private byte[] readBuffer;                              // Serial Buffer
    private int readBufferPosition;                         // Current pointer position for data reading
    private int readLimiterPosition;                        // Current pointer position for searching limiter characters

    private InputStream inputStream;                        // Bluetooth communication Inputsream
    private long thread_pastMillis;                         // last send data time
    private OutputStream outputStream;                      // Bluetooth communication Outputstream
    private SeekBar sb_LEDLightControl;                     // SeekBar to control the LED Stripes
    private Set<BluetoothDevice> pairedDevices;             // Set of paired bluetooth devices
    private Switch sw_BluetoothState;                       // Switch to turn ON/OFF bluetooth
    private String s_ValuePressure, s_ValueTemperature, s_ValueBrightness, s_ValueAirHumidity, s_ValueTerraHumidity, s_ValueLEDState;
    private TextView tv_ValuePressure, tv_ValueTemperature, tv_ValueBrightness, tv_ValueAirHumidity, tv_ValueTerraHumidity, tv_ValueLEDState;
    private Thread workerThread;                            // Thread for bluetooth data stream

    private LayoutInflater inflater;
    private TableLayout table;

    // 0 temperature, 1 pressure, 2 brightness, 3 humidity, 4 moisture,
    private String tableIdentifiers[];
    private double tableValues[];


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = getResources();

        // Prepare and set receiver for bluetooth actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(mReceiver, filter);

        sb_LEDLightControl = findViewById(R.id.sb_LEDState);
        tv_ValueLEDState   = findViewById(R.id.tw_LEDState);
        sw_BluetoothState  = findViewById(R.id.sw_BluetoothONOFF);
        btn_bt_connect     = findViewById(R.id.btn_Connect);
        table              = findViewById(R.id.table);

        // TODO: Remove these textViews and ID's
        // If we use the updateTable method to show the tableValues
        // we don't need these variables.

       /* tv_ValueTemperature     = findViewById(R.id.val_temperature);
        tv_ValuePressure        = findViewById(R.id.val_pressure);
        tv_ValueBrightness      = findViewById(R.id.val_brightness);
        tv_ValueAirHumidity     = findViewById(R.id.val_airHumidity);
        tv_ValueTerraHumidity   = findViewById(R.id.val_terraHumidity);
*/
        // ----------------------------------------------------------------

        // Initialize Values

        tableValues = new double[5];
        tableIdentifiers = res.getStringArray(R.array.array_label_identifiers);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        sb_LEDLightControl.setMax(100);
        sb_LEDLightControl.setProgress(0);
        tv_ValueLEDState.setText(Integer.toString(0));
        s_ValueLEDState = "";
        b_isInitialized = false;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check bluetooth adapters state for enable switch
        if(bluetoothAdapter.isEnabled()){
            sw_BluetoothState.setChecked(true);
        } else {
            sw_BluetoothState.setChecked(false);
        }


        /*------------------------
        -------Set Listener-------
        --------------------------*/
        sw_BluetoothState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                /*if(b){
                    enableBluetooth();
                } else {
                    disableBluetooth();
                }*/
                toggleBluetooth(b);
            }
        });
        sb_LEDLightControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tv_ValueLEDState.setText(Integer.toString(sb_LEDLightControl.getProgress()));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String sendString = "a" + Integer.toString(sb_LEDLightControl.getProgress());
                serialWrite(sendString);
            }
        });
        btn_bt_connect.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_connecting),Toast.LENGTH_SHORT).show();
                connectToPairedDevice();
            }
        });

        // I will change the table layout therefor we will need
        // this method to reload the table and show the user the
        // correct tableValues.
        updateTable();
    }

    /*---------------------------
      -----Additional Methods-----
      ---------------------------*/
    /*private void enableBluetooth(){
        // enables bluetooth adapter if it is disabled
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        while(!bluetoothAdapter.isEnabled()){
            //wait until bt is enabled
        }
        Toast.makeText(getApplicationContext(), "Bluetooth ON", Toast.LENGTH_SHORT).show();
    }*/

    /*private void disableBluetooth(){
        // disables bluetooth adapter if it is enabled
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
        while(bluetoothAdapter.isEnabled()){
            //wait until bt is disabled
        }
        Toast.makeText(getApplicationContext(), "Bluetooth OFF", Toast.LENGTH_SHORT).show();
    }*/
    private void toggleBluetooth(boolean b) {
        String msg;
        if(b){

            if(!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            while(!bluetoothAdapter.isEnabled()){
                //wait until bt is enabled
            }
            msg= "Bluetooth ON";

        } else {

            if(bluetoothAdapter.isEnabled()){
                bluetoothAdapter.disable();
            }
            while(bluetoothAdapter.isEnabled()){
                //wait until bt is disabled
            }
            msg= "Bluetooth OFF";

        }

        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void connectToPairedDevice(){
        // connects bluetooth adapter to a paired bluetooth device
        // I think we can write:
        // if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
        // at this position
        if (bluetoothAdapter != null) {
            pairedDevices = bluetoothAdapter.getBondedDevices();
            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();                       // get bonded devices
                if (bondedDevices.size() > 0) {
                    for (BluetoothDevice mDevice : pairedDevices) {
                        if (mDevice.getName().equals("HC-05")) {                                                // Looking for HC-05 device
                            device = mDevice;
                        }
                        ParcelUuid[] uuids = device.getUuids();
                        try {
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());     // Create communication socket
                        } catch (IOException e) { Log.d(TAG,"Could not create Socket");
                        }
                        try {
                            bluetoothSocket.connect();                                                          // Connect socket
                        } catch (IOException e) {Log.d(TAG,"Could not connect");
                        }
                        try {
                            outputStream = bluetoothSocket.getOutputStream();                                   // Create an output stream
                        } catch (IOException e) {Log.d(TAG,"Could not create Outputstream");
                        }
                        try {
                            inputStream = bluetoothSocket.getInputStream();                                     // Create an input stream
                        } catch (IOException e) {Log.d(TAG,"Could not create Inputstream");
                        }
                        beginListenForData();                                                                   // Create a communication thread
                        serialWrite("h");                                                                    // send initial data
                    }
                } else {
                    Log.e("error", "Bluetooth is disabled.");
                }
            }
        }
    }

    public void serialWrite(String s) {
        // writes the string s to the output stream
        try {
            outputStream.write(s.getBytes());
        }catch(IOException e){Log.i(TAG,"could not send String");}
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        // receiver for bluetooth actions (connected, disconnected)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_connected),Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_disconnected),Toast.LENGTH_SHORT).show();
            }
        }
    };

    void beginListenForData() {
        // Thread for receiving bluetooth data from inputstream and data synchronization
        final Handler handler = new Handler();
        final byte delimiter = 10;                                  // ASCII code for a newline character
        thread_pastMillis = System.currentTimeMillis();             // sets current timer

        // initialize thread variables
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        // create thread
        workerThread = new Thread(new Runnable(){
            public void run(){
                while(!Thread.currentThread().isInterrupted() && !stopWorker){
                    // synchronize sensor tableValues every 5 seconds
                    if(System.currentTimeMillis() - thread_pastMillis > 5000){
                        serialWrite("h");
                        thread_pastMillis = System.currentTimeMillis();
                    }
                    // read from input stream
                    try{
                        int bytesAvailable = inputStream.available();               // if there is something to read
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
                                            // evaluate the incoming data string
                                            for(int i = 0; i<data.length(); i++){
                                                if(data.substring(i,i+1).equals("h")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("l")){
                                                            readLimiterPosition = j;
                                                        }
                                                    }
                                                    try{
                                                        tableValues[2] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    } catch(NumberFormatException ex){
                                                        tableValues[2] = Double.NaN;
                                                    }
                                                    Log.d(TAG,"brightness: '" + tableValues[2] + "'");
                                                    /*
                                                    tableValues[2] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    tv_ValueBrightness.setText(
                                                            res.getString(R.string.dim_brightness,
                                                            data.substring(i+1, readLimiterPosition))
                                                    );*/
                                                }
                                                if(data.substring(i,i+1).equals("l")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("p")){
                                                            readLimiterPosition = j;
                                                        }
                                                    }
                                                    try{
                                                        tableValues[3] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    } catch(NumberFormatException ex){
                                                        tableValues[3] = Double.NaN;
                                                    }
                                                    Log.d(TAG,"humidity: '" + tableValues[3] + "'");
                                                    /*
                                                    try {
                                                        tableValues[3] = Double.valueOf(data.substring(i + 1, readLimiterPosition));
                                                    }catch(NumberFormatException ex) {
                                                        tableValues[3] = 0.0;
                                                    }
                                                    tv_ValueAirHumidity.setText(
                                                            res.getString(R.string.dim_humidity,
                                                            data.substring(i+1, readLimiterPosition))
                                                    );*/
                                                }
                                                if(data.substring(i,i+1).equals("p")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("t")){
                                                            readLimiterPosition = j;
                                                        }
                                                    }
                                                    try{
                                                        tableValues[1] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    } catch(NumberFormatException ex){
                                                        tableValues[1] = Double.NaN;
                                                    }
                                                    Log.d(TAG,"pressure: '" + tableValues[1] + "'");
                                                    /*

                                                    tableValues[0] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    tv_ValuePressure.setText(
                                                            res.getString(R.string.dim_pressure,
                                                            data.substring(i+1, readLimiterPosition))
                                                    );*/
                                                }
                                                if(data.substring(i,i+1).equals("t")){
                                                    for(int j = i; j < data.length(); j++){
                                                        if(data.substring(j,j+1).equals("g")){
                                                            readLimiterPosition = j;
                                                        }
                                                    }
                                                    try{
                                                        tableValues[0] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    } catch(NumberFormatException ex){
                                                        tableValues[0] = Double.NaN;
                                                    }
                                                    Log.d(TAG,"temperature: '" + tableValues[0] + "'");
                                                    /*
                                                    tableValues[1] = Double.valueOf(data.substring(i+1, readLimiterPosition));
                                                    /*tv_ValueTemperature.setText(
                                                            res.getString(R.string.dim_temperature,
                                                            data.substring(i+1, readLimiterPosition))
                                                    );*/
                                                }
                                                if(data.substring(i,i+1).equals("g")){
                                                    s_ValueLEDState = data.substring(i+1,data.length()-1);
                                                    Log.d(TAG,"LED_Level: '" + s_ValueLEDState + "'");
                                                    if(!b_isInitialized){           // initialize the seekbar if it is the first call
                                                        sb_LEDLightControl.setProgress(Integer.parseInt(s_ValueLEDState));
                                                        b_isInitialized = true;
                                                    }
                                                }
                                            }
                                            updateTable();
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

        // I will change the table layout therefor we will need
        // this method to reload the table and show the user the
        // correct tableValues.
        workerThread.start();
    }

    private void updateTable() {
        // We will generate and update
        // the table with te correct tableValues
        // here.

        table.removeAllViews();
        String valueInputs[] = new String[]{
                res.getString(R.string.dim_temperature, String.valueOf(tableValues[0])),
                res.getString(R.string.dim_pressure, String.valueOf(tableValues[1])),
                res.getString(R.string.dim_brightness, String.valueOf(tableValues[2])),
                res.getString(R.string.dim_humidity, String.valueOf(tableValues[3])),
                res.getString(R.string.dim_humidity, String.valueOf(tableValues[4])),
        };


        for (int i = 0; i< tableValues.length; i++) {
            View row = inflater.inflate(R.layout.item_row_main_activity, null);
            TextView identifier = row.findViewById(R.id.row_identifier);
            TextView value = row.findViewById(R.id.row_value);

            identifier.setText(tableIdentifiers[i]);
            value.setText(valueInputs[i]);

            if (i%2 == 0) {row.setBackgroundColor(res.getColor(R.color.colorTableLight));}
            table.addView(row);
        }

    }

    public void action_button(View v) {
        Toast.makeText(getApplicationContext(),res.getString(R.string.msg_bt_connecting),Toast.LENGTH_SHORT).show();
        connectToPairedDevice();
    }
}