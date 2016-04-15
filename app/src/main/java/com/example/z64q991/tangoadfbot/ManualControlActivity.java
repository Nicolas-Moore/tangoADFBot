package com.example.z64q991.tangoadfbot;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;




public class ManualControlActivity extends AppCompatActivity {

    private Button forwardButton;
    private Button reverseButton;
    private Button rightButton;
    private Button leftButton;
    private Button stopButton;
    private Button backButton;
    private TextView statusText;
    public List<UsbSerialDriver> availableDrivers;
    public UsbManager manager;
    public UsbSerialDriver driver;
    public UsbDeviceConnection connection;
    public UsbSerialPort port;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        forwardButton = (Button) findViewById(R.id.forwardButton);
        reverseButton = (Button) findViewById(R.id.reverseButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        rightButton = (Button) findViewById(R.id.rightButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        backButton = (Button) findViewById(R.id.backButton);
        statusText = (TextView) findViewById(R.id.statusText);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);





        tryConnect();
        setUpButtons();


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

    public void setUpButtons(){

        forwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                sendCommand('w');
            }
        });

        reverseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                sendCommand('s');
            }

        });
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                sendCommand(' ');
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                sendCommand('d');
            }
        });
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendCommand('a');
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goBack();
            }
        });
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


    public void goBack(){
        super.onBackPressed();
    }


}
