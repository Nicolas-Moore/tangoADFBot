package com.example.z64q991.tangoadfbot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;



public class MainActivity extends AppCompatActivity {


    private Button manualButton;
    private Button automatedButton;
    private Button remoteControlButton;
    private Button adfRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adfloader);

        manualButton = (Button) findViewById(R.id.manualButton);
        automatedButton = (Button) findViewById(R.id.automatedButton);
        remoteControlButton = (Button) findViewById(R.id.remoteControlButton);
        adfRecorder = (Button) findViewById(R.id.recordButton);
        setUpButtons();

    }



    public void setUpButtons(){


        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runActivity(0);

            }
        });

        automatedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runActivity(1);
            }
        });

        remoteControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
         runActivity(2);
            }
        });



    }
    // maybe take in button id and switch based on that
    public void runActivity(int buttonID){
        Intent intent;
        switch(buttonID){
            case 0:
                intent = new Intent(this, ManualControlActivity.class);
                this.startActivity(intent);
                break;
            case 1:
                intent = new Intent(this, AutomatedActivity.class);
                this.startActivity(intent);
                break;
            case 2:
                intent = new Intent(this, RemoteActivity.class);
                this.startActivity(intent);
                break;
            case 3:
                intent = new Intent(this, ADFRecorder.class);
                this.startActivity(intent);
                break;

        }




    }


}
