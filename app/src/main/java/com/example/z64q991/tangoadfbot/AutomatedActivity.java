package com.example.z64q991.tangoadfbot;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;


import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AutomatedActivity extends AppCompatActivity {
    private static final double UPDATE_INTERVAL_MS = 100.0;
    private double mPreviousPoseTimeStamp;

    private double mTimeToNextUpdate = UPDATE_INTERVAL_MS;
    private static final int SECS_TO_MILLISECS = 1000;
    private Tango mTango;
    private ArrayList<String> mFullUUIDList;
    private Context mContext;
    private TangoConfig mConfig;
    double yAngle;
    double[] q;
    double[] p;
    double[] target;
    private TextView quats;
    boolean localized;
    private TextView targetQuats;
    public List<UsbSerialDriver> availableDrivers;
    public UsbManager manager;
    public UsbSerialDriver driver;
    public UsbDeviceConnection connection;
    public UsbSerialPort port;
    private TextView statusText;
    private Button goButton;
    private Button stopButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        targetQuats = (TextView) findViewById(R.id.targetQuats);
        statusText = (TextView) findViewById(R.id.statusText);
        goButton = (Button) findViewById(R.id.goButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        backButton = (Button) findViewById(R.id.backButton);
        quats = (TextView) findViewById(R.id.quats);
        mTango = new Tango(this);
        localized = false;
        mConfig = setTangoConfig(mTango, false, false);
        startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), Tango.TANGO_INTENT_ACTIVITYCODE);
        mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        getPermissions();
        tryConnect();

        target = new double[3];
        target[0] = 2;
        target[1] = 2;
        target[2] = 0;

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adfDirection();
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                goBack();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(' ');
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();

        // Clear the relocalization state: we don't know where the device has been since our app
        // was paused.
        localized = false;

        // Re-attach listeners.
        try {
            setTangoListeners();
        } catch (TangoErrorException e) {

        } catch (SecurityException e) {

        }

        // Connect to the tango service (start receiving pose updates).
        try {
            mTango.connect(mConfig);
        } catch (TangoOutOfDateException e) {

        } catch (TangoErrorException e) {

        } catch (TangoInvalidException e) {

        }
    }


    public void sendCommand(char mes) {
        if(connection == null){
            statusText.setText("No connection Available");
            return;
        }

        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            byte message[] = new byte[8];
            for(int i = 0; i<8;i++) {
                message[i] = (byte) mes;
            }
            port.write(message, 1000);
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
    }

    private void setTangoListeners() {
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Format Translation and Rotation data

                // Output to LogCat

                final double deltaTime = (pose.timestamp - mPreviousPoseTimeStamp)
                        * SECS_TO_MILLISECS;
                mPreviousPoseTimeStamp = pose.timestamp;
                mTimeToNextUpdate -= deltaTime;

                // Throttle updates to the UI based on UPDATE_INTERVAL_MS.
                if (mTimeToNextUpdate < 0.0) {
                    mTimeToNextUpdate = UPDATE_INTERVAL_MS;

                    // Display data in TextViews. This must be done inside a
                    // runOnUiThread call because
                    // it affects the UI, which will cause an error if performed
                    // from the Tango
                    // service thread
                    if (pose.statusCode == TangoPoseData.POSE_VALID) {
                        localized = true;
                        q = pose.rotation;
                        p = pose.translation;

                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (localized) {
                                DecimalFormat df = new DecimalFormat("#.00");
                                double turn = Math.atan2(2 * (q[3] * q[2] + q[0] * q[1]), 1 - 2 * (q[1] * q[1] + q[2] * q[2]));
                                quats.setText("Position: (" + df.format(p[0]) + "," + df.format(p[1]) + "," + df.format(p[2]) + ")" +
                                        "\n Rotation on Y = " + df.format(turn) + ".");
                                yAngle = turn;
                            } else {
                                quats.setText("Tango is not Localized.");
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

    public void adfDirection() {

        double dist; // setting up dist
        do {
            targetQuats.setText("Got past boolean checks..");

            double yDif = target[1] - p[1];
            double xDif = target[0] - p[0];

            dist = Math.sqrt(((target[1] - p[1]) * (target[1] - p[1])) + ((target[0] - p[0]) * (target[0] - p[0])));


            double angleChange = Math.atan2(yDif, xDif);
            DecimalFormat df = new DecimalFormat("#.00");
            targetQuats.setText("Distance from target =" + df.format(dist) + " and angle needed to change is:" + df.format(angleChange));


            // another if statement for if we are near enough to target, stop.

            if (angleChange - yAngle > .25) {
                sendCommand('d');
            } else if (angleChange - yAngle < -.25) {
                sendCommand('a');
            } else {
                sendCommand('w');
            }
        }while(dist <0.25);

            sendCommand(' ');
            return;




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

    public void getPermissions(){

        if(Tango.hasPermission(this,Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            if(Tango.hasPermission(this,Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
                mFullUUIDList = mTango.listAreaDescriptions();
                if (mFullUUIDList.size() > 0) {
                    mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                            mFullUUIDList.get(mFullUUIDList.size() - 2));
                }}

        }
        else{
            getPermissions();
        }
    }

    public void goBack(){
        super.onBackPressed();
    }
}
