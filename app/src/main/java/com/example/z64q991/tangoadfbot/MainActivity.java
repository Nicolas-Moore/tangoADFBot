package com.example.z64q991.tangoadfbot;

import android.hardware.usb.UsbDeviceConnection;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.hardware.usb.UsbManager;


public class MainActivity extends AppCompatActivity {

    // this is all the tango and adf things required
    private Tango mTango;
    private ArrayList<String> mFullUUIDList;
    private Context mContext;
    private TangoConfig mConfig;

    private Button forwardButton;
    private Button reverseButton;
    private Button leftButton;
    private Button rightButton;
    private Button stopButton;
    private Button manualButton;
    private Button automatedButton;
    private Button remoteControlButton;
    private TextView statusText;
    private TextView adfStatus;
    private TextView controlType;
    private TextView quats;
    private TextView targetQuats;
    private TextView values;
    private double[] q;
    private double[] p;
    private double[] target;
    public double yAngle;
    private boolean localized;
    private char lastCommand;
    private boolean connected;
    private boolean automated;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String sTranslationFormat = "Translation: %f, %f, %f";
    private static final String sRotationFormat = "Rotation: %f, %f, %f, %f";

    private static final int SECS_TO_MILLISECS = 1000;
    private static final double UPDATE_INTERVAL_MS = 100.0;

    private double mPreviousTimeStamp;
    private double mTimeToNextUpdate = UPDATE_INTERVAL_MS;


    // setting up everything for the serial USB
    public List<UsbSerialDriver> availableDrivers;
    public UsbManager manager;
    public UsbSerialDriver driver;
    public UsbDeviceConnection connection;
    public UsbSerialPort port;

    boolean mIsTangoServiceConnected;
    ArrayList<String> fullUUIDList = new ArrayList<String>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adfloader);
        mIsTangoServiceConnected = false;
        forwardButton = (Button) findViewById(R.id.buttonForward);
        reverseButton = (Button) findViewById(R.id.buttonReverse);
        stopButton = (Button) findViewById(R.id.buttonStop);
        rightButton = (Button) findViewById(R.id.buttonRight);
        leftButton = (Button) findViewById(R.id.buttonLeft);
        manualButton = (Button) findViewById(R.id.manualButton);
        automatedButton = (Button) findViewById(R.id.automatedButton);
        remoteControlButton = (Button) findViewById(R.id.remoteControlButton);
        setUpButtons();
        statusText = (TextView) findViewById(R.id.statusText);
        controlType = (TextView) findViewById(R.id.controlText);
        adfStatus =(TextView) findViewById(R.id.adfStatus);
        quats = (TextView)  findViewById(R.id.quats);
        targetQuats = (TextView) findViewById(R.id.targetQuat);
        values = (TextView) findViewById(R.id.values);
        quats.setText("No quaternians available.");
        targetQuats.setText("Awaiting Localization.");
        localized = false;
        mTango = new Tango(this);
        target = new double[3];
        target[0]= 2.0;
        target[1] =2.0;
        target[2] = 0.0;

        startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), Tango.TANGO_INTENT_ACTIVITYCODE);
        mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        getPermissions();










    }

    public void getPermissions(){

        if(Tango.hasPermission(this,Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            if(Tango.hasPermission(this,Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
                fullUUIDList = mTango.listAreaDescriptions();
                if (fullUUIDList.size() > 0) {
                    mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                            fullUUIDList.get(fullUUIDList.size()- 2));
                    adfStatus.setText("ADF is Loaded.");
                }}

        }
        else{
            getPermissions();
        }
    }


    public void setUpButtons(){

        forwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(automated == false)
                    sendCommand('w');
            }
        });

        reverseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(automated == false)
                    sendCommand('s');

            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(automated == false)
                    sendCommand(' ');
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(automated == false)
                    sendCommand('d');
            }
        });
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(automated == false)
                    sendCommand('a');
            }
        });
        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                automated = false;
                controlType.setText("Manually Controlled");
                // instead of calling onResume make this its own activity that starts running on the button click
                // create intent then do start activity
                onResume();
            }
        });

        automatedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                automated = true;
                controlType.setText("Automated Control");
                // instead of calling onResume make this its own activity that starts running on the button click
                onResume();
            }
        });

        remoteControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                automated = false;
                controlType.setText("Remote Controlled");
                // instead of calling onResume make this its own activity that starts running on the button click
                onResume();
            }
        });



    }

    @Override
    public void onResume(){
        super.onResume();
        if(connected && automated){
            values.setText("Connected True, Automated True");
        }
        if(!connected && automated){
            values.setText("Connected False, Automated True");
        }
        if(connected  && !automated){
            values.setText("Connected True, Automated False");
        }
        if(!connected && !automated){
            values.setText("Connected False, Automated False");
        }

                if (!mIsTangoServiceConnected) {
            try {
                setTangoListeners();
            } catch (TangoErrorException e) {
                Toast.makeText(this, "Tango Error! Restart the app!",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                mTango.connect(mConfig);
                mIsTangoServiceConnected = true;
            } catch (TangoOutOfDateException e) {
                Toast.makeText(getApplicationContext(),
                        "Tango Service out of date!", Toast.LENGTH_SHORT)
                        .show();
            } catch (TangoErrorException e) {
                Toast.makeText(getApplicationContext(),
                        "Tango Error! Restart the app!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        adfDirection();

        if(connected == false){
            tryConnect();
            return;
        }
        if(automated == false){

            return;
        }


        connection = manager.openDevice(port.getDriver().getDevice());
        if (connection == null) {
            statusText.setText("No Connection Available");
            return;
        }



    }


    private void setTangoListeners() {
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new OnTangoUpdateListener() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Format Translation and Rotation data

                // Output to LogCat

                final double deltaTime = (pose.timestamp - mPreviousTimeStamp)
                        * SECS_TO_MILLISECS;
                mPreviousTimeStamp = pose.timestamp;
                mTimeToNextUpdate -= deltaTime;

                // Throttle updates to the UI based on UPDATE_INTERVAL_MS.
                if (mTimeToNextUpdate < 0.0) {
                    mTimeToNextUpdate = UPDATE_INTERVAL_MS;

                    // Display data in TextViews. This must be done inside a
                    // runOnUiThread call because
                    // it affects the UI, which will cause an error if performed
                    // from the Tango
                    // service thread
                    if(pose.statusCode == TangoPoseData.POSE_VALID){
                        localized = true;
                        q = pose.rotation;
                        p = pose.translation;

                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(localized) {
                                DecimalFormat df = new DecimalFormat("#.00");
                                double turn = Math.atan2(2*(q[3]*q[2]+q[0]*q[1]), 1-2*(q[1]*q[1]+q[2]*q[2]));
                                targetQuats.setText("Position: ("+df.format(p[0])+","+df.format(p[1])+","+df.format(p[2])+")" +
                                        "\n Rotation on Y = "+ df.format(turn)+".");
                                yAngle = turn;
                            }
                            else {
                                targetQuats.setText("Tango is not Localized.");
                            }

                        }
                    });
                }

            }


            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0) {
                // Ignoring XyzIj data
            }

            @Override
            public void onTangoEvent(TangoEvent arg0) {
                // Ignoring TangoEvents
            }

            @Override
            public void onFrameAvailable(int arg0) {
                // Ignoring onFrameAvailable Events
            }
        });


    }





    public void adfDirection(){
        if(connected == false || automated == false){
            return;
        }

        quats.setText("Got past boolean checks..");

        double yDif = target[1] -p[1];
        double xDif = target[0] - p[0];

        double dist = Math.sqrt(((target[1]-p[1])*(target[1]-p[1])) + ((target[0]-p[0])*(target[0]-p[0])) );


        double angleChange =  Math.atan2(yDif, xDif);
        DecimalFormat df = new DecimalFormat("#.00");
        quats.setText("Distance from target =" +df.format(dist)+" and angle needed to change is:"+df.format(angleChange));


        // another if statement for if we are near enough to target, stop.

        if(angleChange-yAngle >.25){
            sendCommand('d');
        }
        else if(angleChange-yAngle <-.25){
            sendCommand('a');
        }
        else{
            sendCommand('w');
        }

        if(dist <.25){
            sendCommand(' ');
            return;
        }
        else{
            onResume();
        }

    }


    public void tryConnect(){
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            statusText.setText("No Drivers Available");
            return;
        }

        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            statusText.setText("No Connection Available");
            return;
        }
        port = driver.getPorts().get(0);
        statusText.setText("Connection Available");
        connected = true;
    }






    public void sendCommand(char mes) {

        if(connected == false){
            return;
        }

        lastCommand = mes;
        if(lastCommand!= mes && lastCommand != ' '){
            mes = ' ';
        }

        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            byte message[] = new byte[8];
    for(int i = 0; i<8;i++) {
    message[i] = (byte) mes;
            }
            port.write(message,1000);
        } catch (IOException e) {
            // Deal with error.
        } finally {
            try{
                port.close();
            }
            catch (IOException e){

            }



        }
    }


    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {
        TangoConfig config = new TangoConfig();
        config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);

        // Check if learning mode
        if (isLearningMode) {
            // Set learning mode to config.
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }
        // Check for Load ADF/Constant Space relocalization mode
        if (isLoadAdf) {
            ArrayList<String> fullUUIDList = new ArrayList<String>();
            // Returns a list of ADFs with their UUIDs
            fullUUIDList = tango.listAreaDescriptions();
            // Load the latest ADF if ADFs are found.
            if (fullUUIDList.size() > 0) {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(fullUUIDList.size() - 1));
            }
        }
        return config;
    }



}
