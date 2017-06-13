package com.example.android.mojicnc;

/*
Android Example to connect to and communicate with Bluetooth
In this exercise, the target is a Arduino Due + HC-06 (Bluetooth Module)

Ref:
- Make BlueTooth connection between Android devices
http://android-er.blogspot.com/2014/12/make-bluetooth-connection-between.html
- Bluetooth communication between Android devices
http://android-er.blogspot.com/2014/12/bluetooth-communication-between-android.html
 */
import android.animation.TypeConverter;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

import static android.R.id.list;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.example.android.mojicnc.R.id.BTsend;
import static com.example.android.mojicnc.R.id.time;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket BTsocket = null;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    TextView textInfo, textStatus, textStatus2, textByteCnt;
    ListView listViewPairedDevice;
    LinearLayout inputPane;
    EditText inputField;
    Button btnSend, btnClear, BTsend;
    Button myOpenFileButton;
    Integer delayValFinal = 500;

    int roundCount=0;
    byte[] myByteArray = new byte[100];
    String[] myStringArray = new String[10];
    String mconvString;
    String myFinalString;
    boolean myUpdateState = false;
    int myUpdateRound = 0;
    String lineTemp;
    String devicenNameTemp;
    String Btname = "";

    String sentText;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 6384; // onActivityResult request
    String pathFileTemp;

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "0001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInfo = (TextView)findViewById(R.id.info);
        textStatus = (TextView)findViewById(R.id.status);
        textStatus2 = (TextView)findViewById(R.id.status2);
        textByteCnt = (TextView)findViewById(R.id.textbyteCnt);
        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        inputPane = (LinearLayout)findViewById(R.id.inputpane);
        inputField = (EditText)findViewById(R.id.input);
        btnSend = (Button)findViewById(R.id.send);
        myOpenFileButton = (Button) findViewById(R.id.openFile) ;
        BTsend = (Button) findViewById(R.id.BTsend) ;



        btnSend.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(myThreadConnected!=null){
                    byte[] bytesToSend = inputField.getText().toString().getBytes();
                    myThreadConnected.write(bytesToSend);
                    byte[] NewLine = "\n".getBytes();
                    myThreadConnected.write(NewLine);
                    sentText = inputField.getText().toString();
                    //textByteCnt.append(sentText + " *SENT");
                    textStatus.append(sentText + " *Sent\n");
                }
            }});

        BTsend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendBTData();   //method to turn off
                textStatus2.setText("");
            }
        });

        myOpenFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooser();      //method to turn on
                textStatus2.setText("");
            }
        });



        btnClear = (Button)findViewById(R.id.clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textStatus.setText("");
                textStatus2.setText("");
                textByteCnt.setText("");

            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
       // textInfo.setText(stInfo);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<>();
            final ArrayList list = new ArrayList();

            for (BluetoothDevice device : pairedDevices) {

                pairedDeviceArrayList.add(device);
                list.add(device.getName() + "\n" + device.getAddress());
                Btname = "Name: " + device.getName() + "\n"
                        + "Address: " + device.getAddress() + "\n"
                        + "BondState: " + device.getBondState() + "\n"
                        + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                        + "Class: " + device.getClass();


            }

            pairedDeviceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);

                    Toast.makeText(MainActivity.this,"Connecting . . .",Toast.LENGTH_SHORT).show();

                    devicenNameTemp = String.valueOf(device.getName());
                    textInfo.setText(devicenNameTemp);

                    textStatus.setText("start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    public void timeDelay(long t) {
        try {
            Thread.sleep(t);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    //extView tv2 =  (TextView)findViewById(R.id.mojiTV2);
                    //tv2.append("OK\n");
                }
            }, t);
        } catch (InterruptedException e) {}

    }
    private void sendBTData() {
        int sendBTCount = 0;
        if (myThreadConnected != null) {
            try {
                File sdcard = Environment.getExternalStorageDirectory();
                Log.e("FILE LOCATIO",sdcard.toString());
                Log.e("BEEEEEEEEE",Environment.getExternalStorageDirectory().toString());

                //Get the text file
                File file = new File(sdcard,"moji.txt");

                //Read text from file
                final StringBuilder text = new StringBuilder();

                BufferedReader br = new BufferedReader(new FileReader(pathFileTemp));
                String line;
                Log.e("myfileTest",file.toString());
               // Log.e("myBrTest",br.toString());

                /**[RMK] make show file name and location in views **/
                text.append("moji.txt");
                while ((line = br.readLine()) != null ) {
                    lineTemp = line;
                    Log.e("SentBTFunc"  ,"ItWorks!");
                    text.append(line);
                    byte[] lineToSend = line.toString().getBytes();
                    myThreadConnected.BTsendText((lineTemp));
                    //timeDelay(50);

//                    new Handler().postDelayed(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            myThreadConnected.BTsendText((lineTemp));
//                        }
//                    }, 1000);



//                    if (myUpdateState = true) {
                      if (myUpdateRound < 4) {
                        byte[] NewLine = "\n".getBytes();
                        myThreadConnected.write(NewLine);
                        // timeDelay(50);
                        sendBTCount++;
                        Log.e("BEEEEEEE", line);
                        Log.e("SendBT_Count: ", String.valueOf(sendBTCount));
                        text.append('\n');
                        textStatus.append("\n");
                        myUpdateRound++;
                    }
                      else if (myUpdateRound == 4){
                          byte[] NewLine = "\n".getBytes();
                          myThreadConnected.write(NewLine);
                          timeDelay(4500);
                          myUpdateRound = 0;
                        //  myThreadConnected.BTsendText(("\n"));
                      }
                    else {
                        //timeDelay(30);
                        myUpdateRound = 0;
                        myThreadConnected.BTsendText((line));

                    }

                }
                myThreadConnected.BTsendText(("$G\n"));
                br.close();
            }
            catch (IOException e) {
                msg("Error");
            }
        }
    }
    private void showChooser() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.chooser_title));
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(this, uri);
                            Toast.makeText(MainActivity.this,
                                    "File Selected: " + path, Toast.LENGTH_LONG).show();
                            Log.e("BEEEEE",path);
                            pathFileTemp = path;

                            //=================================== BEE EDIT ===================================
                            File sdcard = Environment.getExternalStorageDirectory();
                            Log.e("FILE LOCATIO",sdcard.toString());
                            Log.e("BEEEEEEEEE",Environment.getExternalStorageDirectory().toString());

                            //Get the text file
                            File file = new File(sdcard,"moji.txt");

                            //Read text from file
                            StringBuilder text = new StringBuilder();

                            try {
                                BufferedReader br = new BufferedReader(new FileReader(path));
                                String line;

                                while ((line = br.readLine()) != null) {
                                    text.append(line);
                                    text.append('\n');
                                }
                                // timeDelay(delayValFinal);
                                br.close();
                            }
                            catch (IOException e) {
                                //You'll need to add proper error handling here
                            }
                            //Find the view by its id
                            //TextView tv = (TextView)findViewById(R.id.mojiTV1);
                           // tv.setMovementMethod(new ScrollingMovementMethod()); // Activate mojiTV1 TextView ScrollBar

                            //Set the text
                          //  tv.setText(text.toString());
                           // BTsend.setVisibility(View.VISIBLE); /** Show BTsend button when selected a file **/

//                            textByteCnt.setText(text.toString());
//                            textByteCnt.setMovementMethod(new ScrollingMovementMethod());


                            textStatus.setText(text.toString());
                            textStatus.setMovementMethod(new ScrollingMovementMethod());
                            //=================================== BEE EDIT ===================================

                        } catch (Exception e) {
                            Log.e("FileSelectorTest", "File select error", e);

                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("");
                        textByteCnt.setText("");
                        Toast.makeText(MainActivity.this, msgconnected, Toast.LENGTH_LONG).show();

                        listViewPairedDevice.setVisibility(View.GONE);
                        inputPane.setVisibility(View.VISIBLE);
                    }
                });

                startThreadConnected(bluetoothSocket);

            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        String Text;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;



            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        public  String stringArrayToString( String[] stringArray, String delimiter ) {
            StringBuilder sb = new StringBuilder();
            for ( String element : stringArray ) {
                if (sb.length() > 0) {
                    sb.append( delimiter );
                }
                sb.append( element );
            }
            return sb.toString();
        }

        public  boolean contains(String[] stringArray, String stringToSearch)
        {
            int x=0;
            boolean result = false;
            if (stringArray != null) return result;
            for (String element:stringArray) {
                if ( element.equals( stringToSearch )) {
                    result = true;
                    break;
                }
            }

            return result;

        }

        @Override
        public void run() {
//            final byte[] buffer = new byte[1024];
            final byte[] buffer = new byte[4096];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream is;
            String strRx = "";


            int bytes;



            while (true) {

                try {


                    bytes = connectedInputStream.read(buffer);
                    final String strText = buffer.toString();
                    final String strReceived = new String(buffer, 0, bytes);
                    final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
//                            cnt ++;
//                            if (cnt ==2){
//                                textStatus.append("*" +strReceived);
////                              textByteCnt.append(strByteCnt);
//                                textByteCnt.append(sentText + " Received\n");
//
//                            }
//                          textByteCnt.append(strByteCnt);
                            //textByteCnt.append(sentText + " Received\n");
//                            Log.e("Beeee",sentText);
                            Log.e("Received :",strReceived);
//                            if (!strReceived.isEmpty()) {
//                                textStatus2.append(strReceived.toString());
//                            }
                            myStringArray[roundCount]= strReceived;
                            if(roundCount == 1){
                            myStringArray[0]= myStringArray[0]+myStringArray[1];
                            }
                            //textStatus2.append(strReceived.toString());

                            // mconvString =  Arrays.toString(myStringArray);
//                            mconvString =  stringArrayToString(myStringArray, ",");
                            //mconvString = mconvString.substring(1, mconvString.length()-1).replaceAll(",", "");

                            //textStatus.append(mconvString);

                           // Log.e("HAHAHAHA: "  ,String.valueOf(contains(myStringArray, "ok")));
//                            if (!myStringArray[1].equals(null) && myStringArray[1].equals("k")){
//                                Log.e("HAHAHAHA: "  ,String.valueOf(contains(myStringArray, "Apple")));
//                            }
                            mconvString =  stringArrayToString(myStringArray, ",");
                            myFinalString = mconvString.substring(0,2);
                            Log.e("My New Array: "  ,mconvString);
                            Log.e("My Newest Array: "  ,myFinalString);
                            if (myFinalString.contains("ok")){
                                Log.e("My Newest Array: "  ,"IT IS TRUE !!!");
                                myFinalString = "";
                                myStringArray[0] = null;
                                roundCount = 0;
                                myUpdateState = true;
//                                textStatus2.append("OK\n");
                                textStatus2.append("o"+strReceived.toString());
                            }
                            else if (roundCount > 3) {
                                myStringArray[0] = null;
                                roundCount = 0;
                            }
                            else {

                                myUpdateState = false;
                                roundCount++;
                            }

                            Log.e("RoundCounter #: "  ,String.valueOf(roundCount));
                            Log.e("UpdateState  #: "  ,String.valueOf(myUpdateState));
                           // textStatus.append(String.valueOf(cnt));
                        }});



                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText(msgConnectionLost);
                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void BTsendText(String y){
            if (connectedBluetoothSocket!=null) {
                try {
                    connectedBluetoothSocket.getOutputStream().write(y.toString().getBytes());
                }
                catch (IOException e)
                { msg("Error");}
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}