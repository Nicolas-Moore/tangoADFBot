package com.example.z64q991.tangoadfbot;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteActivity extends AppCompatActivity {
    Thread serverThread;
    ServerSocket serverSocket;
    Socket socket;
    TextView dumpText;
    ScrollView scroller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        dumpText = (TextView)findViewById(R.id.dumpText);
        scroller = (ScrollView)findViewById(R.id.scroll);


    }

    public void dump(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dumpText.append(s + "\n");
                scroller.fullScroll(View.FOCUS_DOWN);
            }
        });

    }

    protected void onResume(){
        super.onResume();
        serverThread = new Thread(new ListenerThread());
        serverThread.start();
    }
    public void restart(){
        stopit();
        serverThread = new Thread(new ListenerThread());
        serverThread.start();
    }

    void stopit(){

        if (serverSocket != null) {
            try {
                if(serverSocket.isBound()){
                    if(socket != null && socket.isConnected()){
                        socket.close();
                    }
                    Log.e("TAT","closing serverSocket" );
                    serverSocket.close();
                    Log.e("TAT", "closedserverSocket");


                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

    }


    protected void onPause() {
        stopit();
        super.onPause();
    }




    public void gotCommand(char mes){
        switch(mes){
            case 'w':
                break;
            case 'a':
                break;
            case 's':
                break;
            case 'd':
                break;
            case ' ':
                break;
        }
    }

    class ListenerThread implements Runnable{
        @Override
        public void run() {

                try {
                    dump("Creating ServerSocket");
                    serverSocket = new ServerSocket(5010);
                    dump("Creating Socket, waiting for connection");
                    socket = serverSocket.accept();
                    dump("waiting for message");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while(!Thread.currentThread().isInterrupted()) {
                        String mes = reader.readLine();
                        if (mes != null && mes.length() > 0) {
                            dump(mes);
                            gotCommand(mes.charAt(0));
                        }
                        else{
                            dump("disconnect");
                            serverSocket.close();
                            restart();

                        }
                    }

                } catch (IOException e) {
                    dump("exception" + e.getMessage());
                }
            }
        }
    }


