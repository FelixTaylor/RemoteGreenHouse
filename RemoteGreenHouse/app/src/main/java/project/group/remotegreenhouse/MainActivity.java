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
    private TextView tv_ValueLEDState;
    private Thread workerThread;                            // Thread for bluetooth data stream
    private double val_temperature, val_pressure, val_brightness, val_humidity, val_moisture, val_LED_state, val_fan_state;

    private LayoutInflater inflater;
    private TableLayout table;

    // 0 temperature, 1 humidity, 2 pressure, 3 brightness, 4 moisture,
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

        // Initialize Values

        tableValues = new double[5];
        tableIdentifiers = res.getStringArray(R.array.array_label_identifiers);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        sb_LEDLightControl.setMax(100);
        sb_LEDLightControl.setProgress(0);
        tv_ValueLEDState.setText(Integer.toString(0));
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
                String sendString = "x" + Integer.toString(sb_LEDLightControl.getProgress());
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
                stopListenForData();
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
                        serialWrite("w");                                                                    // send initial data
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

    void stopListenForData(){
        stopWorker = true;
        b_isInitialized = false;
        //reset readBuffer
        readBufferPosition = 0;
        //reset tableValues and SeekBar
        sb_LEDLightControl.setProgress(0);
        tableValues[0] = 0.0;
        tableValues[1] = 0.0;
        tableValues[2] = 0.0;
        tableValues[3] = 0.0;
        updateTable();
    }

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
                        serialWrite("w");
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
                                            getValuesFromData(data);
                                            setTableValues();
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

    private void getValuesFromData(String str_data){
        Log.d(TAG,"Data: '" + str_data + "'");
        for(int i = 0; i < str_data.length(); i++){
            if(str_data.substring(i,i+1).equals("t")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("h")){
                        readLimiterPosition = j;
                    }
                }
                try{
                    val_temperature = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                    val_temperature = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("h")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("p")){
                        readLimiterPosition = j;
                    }
                }
                try{
                    val_humidity = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                    val_humidity = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("p")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("m")){
                        readLimiterPosition = j;
                    }
                }
                try{
                    val_pressure = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                    val_pressure = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("m")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("b")){
                        readLimiterPosition = j;
                    }
                }
                try{
                   val_moisture = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                   val_moisture = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("b")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("l")){
                        readLimiterPosition = j;
                    }
                }
                try{
                   val_brightness = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                   val_brightness = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("l")){
                for(int j = i; j < str_data.length(); j++){
                    if(str_data.substring(j,j+1).equals("f")){
                        readLimiterPosition = j;
                    }
                }
                try{
                   val_LED_state = Double.valueOf(str_data.substring(i+1, readLimiterPosition));
                }catch(NumberFormatException ex){
                   val_LED_state = Double.NaN;
                }
            }
            if(str_data.substring(i,i+1).equals("f")){
                try{
                   val_fan_state = Double.valueOf(str_data.substring(i+1, str_data.length()-1));
                }catch(NumberFormatException ex){
                   val_fan_state = Double.NaN;
                }
            }
        }
        if(!b_isInitialized){
            sb_LEDLightControl.setProgress((int) val_LED_state);
            b_isInitialized = true;
        }
    }

    private void setTableValues() {
        tableValues[0] = val_temperature;
        tableValues[1] = val_humidity;
        tableValues[2] = val_pressure;
        tableValues[3] = val_moisture;
        tableValues[4] = val_brightness;
    }

    private void updateTable() {
        // We will generate and update
        // the table with te correct tableValues
        // here.

        table.removeAllViews();
        String valueInputs[] = new String[]{
                res.getString(R.string.dim_temperature, String.valueOf(tableValues[0])),
                res.getString(R.string.dim_humidity, String.valueOf(tableValues[1])),
                res.getString(R.string.dim_pressure, String.valueOf(tableValues[2])),
                res.getString(R.string.dim_humidity, String.valueOf(tableValues[3])),
                res.getString(R.string.dim_brightness, String.valueOf(tableValues[4])),
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