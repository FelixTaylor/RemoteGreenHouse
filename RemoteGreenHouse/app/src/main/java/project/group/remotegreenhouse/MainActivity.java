package project.group.remotegreenhouse;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.nfc.Tag;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements
        TabHost.OnTabChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName(); // TAG for logging data
    private static final int REQUEST_ENABLE_BT = 1;         // Code for Enable_Request
    private Resources res;                                  // Using resources directory
    private BluetoothAdapter bluetoothAdapter;              // Local bluetooth adapter
    private BluetoothDevice device;                         // Bluetooth device
    private BluetoothSocket bluetoothSocket;                // Bluetooth communication Socket
    private volatile boolean stopWorker;                    // Variable to stop the communication thread
    private boolean b_isInitialized;                        // Set to true, if state got initialized data from arduino
    private byte[] readBuffer;                              // Serial Buffer
    private int readBufferPosition;                         // Current pointer position for data reading
    private int readLimiterPosition;                        // Current pointer position for searching limiter characters
    private int it_getValues;                               // Iterator in getSensorValues

    private InputStream inputStream;                        // Bluetooth communication Inputsream
    private long thread_pastMillis;                         // last send data time
    private OutputStream outputStream;                      // Bluetooth communication Outputstream
    private SeekBar sb_LEDLightControl;                     // SeekBar to control the LED Stripes
    private Set<BluetoothDevice> pairedDevices;             // Set of paired bluetooth devices
    private TextView tv_ValueLEDState, tv_lightOnTime, tv_lightOffTime;
    private Thread workerThread;                            // Thread for bluetooth data stream
    private int val_LED_state;
    private String time_led_on, time_led_off;
    private double sensorValues[];

    private LayoutInflater inflater;
    private TableLayout table;

    // 0 temperature, 1 humidity, 2 pressure, 3 brightness, 4 moisture,
    private String tableIdentifiers[];
    private double tableValues[];
    private TabHost tabHost;
    private TimePickerDialog timePickerDialog;

    private class SeekbarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            tv_ValueLEDState.setText(Integer.toString(sb_LEDLightControl.getProgress()));
        }
        @Override public void onStopTrackingTouch(SeekBar seekBar) {
            if(bluetoothAdapter.isEnabled() && outputStream != null) {
                val_LED_state = sb_LEDLightControl.getProgress();
                sendData();
            }
        }
    }

    public void lightOnClick(View v){
        Calendar calendar = Calendar.getInstance();
        timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                Calendar timeCalendar = Calendar.getInstance();
                timeCalendar.set(Calendar.HOUR_OF_DAY, i);
                timeCalendar.set(Calendar.MINUTE, i1);
                String timeString = DateUtils.formatDateTime(MainActivity.this,timeCalendar.getTimeInMillis(),DateUtils.FORMAT_SHOW_TIME);
                tv_lightOnTime.setText(timeString);
                timeString = Integer.toString(i*3600000+i1*60000);
                time_led_on = timeString;
                sendData();
            }
        },calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    public void lightOffClick(View v){
        Calendar calendar = Calendar.getInstance();
        timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                Calendar timeCalendar = Calendar.getInstance();
                timeCalendar.set(Calendar.HOUR_OF_DAY, i);
                timeCalendar.set(Calendar.MINUTE, i1);
                String timeString = DateUtils.formatDateTime(MainActivity.this,timeCalendar.getTimeInMillis(),DateUtils.FORMAT_SHOW_TIME);
                tv_lightOffTime.setText(timeString);
                timeString = Integer.toString(i*3600000+i1*60000);
                time_led_off = timeString;
                sendData();
            }
        },calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = getResources();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeTabHost();

        // Prepare and set receiver for bluetooth actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(mReceiver, filter);

        sb_LEDLightControl = findViewById(R.id.sb_LEDState);
        tv_ValueLEDState   = findViewById(R.id.tv_LEDState);
        tv_lightOnTime     = findViewById(R.id.tv_lightOnTime);
        tv_lightOffTime    = findViewById(R.id.tv_lightOffTime);
        table              = findViewById(R.id.table);

        // Initialize Values
        sensorValues = new double[7];
        tableValues = new double[5];
        tableIdentifiers = res.getStringArray(R.array.array_label_identifiers);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        sb_LEDLightControl.setMax(100);
        sb_LEDLightControl.setProgress(0);
        tv_ValueLEDState.setText(Integer.toString(sb_LEDLightControl.getProgress()));
        b_isInitialized = false;


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sb_LEDLightControl.setOnSeekBarChangeListener(new SeekbarListener());
        updateTable();
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem bluetooth = menu.getItem(0);

        if(bluetoothAdapter.isEnabled()){
            bluetooth.setIcon(res.getDrawable(R.mipmap.ic_bluetooth_white));
        } else {
            bluetooth.setIcon(res.getDrawable(R.mipmap.ic_bluetooth_disabled_white));
        }

        return super.onPrepareOptionsMenu(menu);
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_bluetooth : {
                boolean bluetoothIsEnabled = true;

                if(bluetoothAdapter.isEnabled()){
                    bluetoothIsEnabled = false;
                    item.setIcon(res.getDrawable(R.mipmap.ic_bluetooth_disabled_white));
                } else {
                    item.setIcon(res.getDrawable(R.mipmap.ic_bluetooth_white));
                }

                toggleBluetooth(bluetoothIsEnabled);
                break;
            }

            case R.id.action_settings : {
                break;
            }
        }
        return true;
    }
    @Override public void    onTabChanged(String tabId) {

        //unselected
        for(int i=0; i<tabHost.getTabWidget().getChildCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(
                    res.getColor(R.color.color_primary_dark));

            ((TextView) tabHost.getTabWidget().getChildAt(i)
                    .findViewById(android.R.id.title))
                    .setTextColor(res.getColor(R.color.colorActivityBackground));
        }

        // selected
        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab())
                .setBackgroundColor(res.getColor(R.color.colorActivityBackground));

        ((TextView) tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab())
                .findViewById(android.R.id.title))
                .setTextColor(res.getColor(R.color.color_primary_dark));
    }

    /*---------------------------
      -----Additional Methods-----
      ---------------------------*/
    private void toggleBluetooth(boolean b) {
        String msg = "";

        if(b && !bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            while(!bluetoothAdapter.isEnabled()){
                //wait until bt is enabled
            }
            msg = res.getString(R.string.msg_bt_on);

        } else if (!b && bluetoothAdapter.isEnabled()){

            stopListenForData();
            bluetoothAdapter.disable();

            while(bluetoothAdapter.isEnabled()){
                //wait until bt is disabled
            }
            msg = res.getString(R.string.msg_bt_off);
        }

        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void connectToPairedDevice() {
        if (bluetoothAdapter != null) {

            pairedDevices = bluetoothAdapter.getBondedDevices();
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();                       // get bonded devices

            if (bondedDevices.size() > 0) {
                for (BluetoothDevice mDevice : pairedDevices) {
                    if (mDevice.getName().equals("HC-05")) {                                                // Looking for HC-05 device

                        device = mDevice;
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
                        String sendString = "w" + getTime() + ";";
                        Log.d(TAG, "Sending: '" + sendString + "'");
                        serialWrite(sendString);
                    }
                }

                if (device == null) {
                    Toast.makeText(getApplicationContext(),
                            res.getString(R.string.msg_no_device_found),
                            Toast.LENGTH_SHORT
                    ).show();
                }

            } else {
                Log.e("error", "No bonded devices.");
            }
        }
    }

    public void serialWrite(String s) {
        // writes the string s to the output stream
        try {
            outputStream.write(s.getBytes());
        } catch(IOException e){
            Log.i(TAG,"could not send String");
        }
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

        for (int i=0; i<tableValues.length; i++) {
            tableValues[i] = 0.0;
        }

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
                        String sendString = "w" + getTime() + ";";
                        Log.d(TAG, "Sending: '" + sendString + "'");
                        serialWrite(sendString);
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
                                            Log.d(TAG,"data: '" + data + "'");
                                            getValuesFromData(data);
                                            //setTableValues();
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
        it_getValues = 0;
        for(int i=0; i<sensorValues.length; i++){
            sensorValues[i] = getSensorValues(str_data);
        }
        if(!b_isInitialized){
            sb_LEDLightControl.setProgress((int) val_LED_state);
            b_isInitialized = true;
        }
    }

    private double getSensorValues(String str_data){
        double mDouble;
        int myInt = it_getValues;
        for(int j = it_getValues +1; j < str_data.length(); j++){
            if(str_data.substring(j,j+1).equals(";")){
                readLimiterPosition = j;
                it_getValues = j+1;
                break;
            }
        }
        try{
            mDouble = Double.valueOf(str_data.substring(myInt, readLimiterPosition));
        }catch(NumberFormatException ex){
            mDouble = Double.NaN;
        }
        return mDouble;
    }

    private void updateTable() {
        table.removeAllViews();
        String valueInputs[] = new String[]{
                res.getString(R.string.dim_temperature, String.valueOf(sensorValues[0])),
                res.getString(R.string.dim_humidity, String.valueOf(sensorValues[1])),
                res.getString(R.string.dim_pressure, String.valueOf(sensorValues[2])),
                res.getString(R.string.dim_humidity, String.valueOf(sensorValues[3])),
                res.getString(R.string.dim_brightness, String.valueOf(sensorValues[4])),
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
        String msg;
        if(bluetoothAdapter.isEnabled()) {
            msg = res.getString(R.string.msg_bt_connecting);
            connectToPairedDevice();
        } else {
            msg = res.getString(R.string.msg_bt_turn_on_bt);
        }

        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void initializeTabHost() {
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        String tabText[] = {
                res.getString(R.string.label_overview),
                res.getString(R.string.label_brightness)
        };
        int layoutIds[] = {
                R.id.content_overview,
                R.id.content_brightness,
        };

        for (int i=0; i<tabText.length; i++) {
            tabHost.addTab(
                    makeTab(
                            tabText[i],
                            layoutIds[i]
                    )
            );

            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(
                    res.getColor(R.color.color_primary_dark)); //selected

            ((TextView) tabHost.getTabWidget().getChildAt(i)
                    .findViewById(android.R.id.title))
                    .setTextColor(res.getColor(R.color.colorActivityBackground));
        }

        tabHost.setOnTabChangedListener(MainActivity.this);
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(
                res.getColor(R.color.colorActivityBackground)); //selected

        ((TextView) tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab())
                .findViewById(android.R.id.title))
                .setTextColor(res.getColor(R.color.color_primary_dark));
    }

    private TabHost.TabSpec makeTab(String tabText, int contentId) {
        TabHost.TabSpec tab = tabHost.newTabSpec(tabText);
        tab.setIndicator(tab.getTag()).setContent(contentId);
        return tab;
    }

    private String getTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();
        return Integer.toString(3600000*calendar.get(Calendar.HOUR_OF_DAY)+60000*calendar.get(Calendar.MINUTE)+1000*calendar.get(Calendar.SECOND));
    }

    private void sendData(){
        //send: xtime;ledValue;ledOnTime;ledOffTime;
        String sendString = "x" + getTime() + ";" + val_LED_state + ";" + time_led_on + ";" + time_led_off + ";";
        Log.d(TAG, "Sendstring: '" + sendString + "'");
        serialWrite(sendString);
    }
}