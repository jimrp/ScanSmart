package com.example.scansmart;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.common.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Scanner3 extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView scannerView;
    private static String scanResult;
    public String temp, type, ssid, pass = "";
    public int temp1;
    public WifiConfiguration conf;
    public WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    public void handleResult(Result result) {
        scanResult = result.getText();

        temp = "";
        type = "";
        ssid = "";
        pass = "";

        //TESSTTTTTTTT

//        if (scanResult.startsWith("WIFI:")) {
//            temp = scanResult.substring(5);
//
//            if(temp.indexOf(";")!=-1){
//                temp1 = temp.indexOf(";");
//                if (temp.startsWith("T")){
//                    temp = temp.substring(2);
//                    type = temp.substring(0,temp1); //get network type
//                } else if (temp.startsWith("P")){
//                    temp = temp.substring(2);
//                    pass = temp.substring(0,temp1); //get password
//                } else if (temp.startsWith("S")){
//                    temp = temp.substring(2);
//                    ssid = temp.substring(0,temp1); //get ssid
//                }
//
//                if(temp.length()>temp1+3){
//
//                    if(temp.indexOf(";")!=-1){
//                        temp1 = temp.indexOf(";");
//                        if (temp.startsWith("T")){
//                            temp = temp.substring(2);
//                            type = temp.substring(0,temp1); //get network type
//                        } else if (temp.startsWith("P")){
//                            temp = temp.substring(2);
//                            pass = temp.substring(0,temp1); //get password
//                        } else if (temp.startsWith("S")){
//                            temp = temp.substring(2);
//                            ssid = temp.substring(0,temp1); //get ssid
//                        }
//
//                        if(temp.length()>temp1+3){
//
//                            if(temp.indexOf(";")!=-1){
//                                temp1 = temp.indexOf(";");
//                                if (temp.startsWith("T")){
//                                    temp = temp.substring(2);
//                                    type = temp.substring(0,temp1); //get network type
//                                } else if (temp.startsWith("P")){
//                                    temp = temp.substring(2);
//                                    pass = temp.substring(0,temp1); //get password
//                                } else if (temp.startsWith("S")){
//                                    temp = temp.substring(2);
//                                    ssid = temp.substring(0,temp1); //get ssid
//                                }
//                            }
//                        }
//                    }
//                }
//            }


        if (scanResult.startsWith("WIFI:T:")) {
            temp = scanResult.substring(7);

            if(temp.indexOf(";")!=-1){
                temp1 = temp.indexOf(";");
                type = temp.substring(0,temp1); //get network type

                if(temp.length()>temp1+3){
                    temp = temp.substring(temp1+3);

                    if(temp.indexOf(";")!=-1){
                        temp1 = temp.indexOf(";");
                        ssid = temp.substring(0,temp1); //get ssid

                        if(temp.length()>temp1+3){
                            temp = temp.substring(temp1+3);

                            if(temp.indexOf(";")!=-1){
                                temp1 = temp.indexOf(";");
                                pass = temp.substring(0,temp1); //get password
                            }
                        }
                    }
                }
            }

            if (!type.equals("") && !ssid.equals("")){
                new AlertDialog.Builder(this)
                        .setTitle("Result")
                        .setMessage("Wifi Network\nSecurity type = " + type + "\nSSID = " + ssid + "\nPassword = " + pass)
                        .setPositiveButton("Connect to " + ssid, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getApplicationContext(), "Trying to connect..", Toast.LENGTH_SHORT).show();

                                conf = new WifiConfiguration();
                                conf.SSID = "\"" + ssid + "\"";

                                if(type.equals("WEP")){
                                    conf.wepKeys[0] = "\"" + pass + "\"";
                                    conf.wepTxKeyIndex = 0;
                                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                                }
                                else if(type.equals("WPA")){
                                    conf.preSharedKey = "\""+ pass +"\"";
                                }
                                else if(type.equals("None")){
                                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                }

                                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                int netId = wifiManager.addNetwork(conf);
                                wifiManager.disconnect();
                                wifiManager.enableNetwork(netId, true);
                                wifiManager.reconnect();

                                scannerView.resumeCameraPreview(Scanner3.this);
                            }})
                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                scannerView.resumeCameraPreview(Scanner3.this);
                            }})
                        .setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    arg0.dismiss();
                                    scannerView.resumeCameraPreview(Scanner3.this);
                                }
                                return true;
                            }
                        })
                        .create()
                        .show();
            }
        }
        else{
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("That's not a WiFi code..")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            scannerView.resumeCameraPreview(Scanner3.this);
                        }
                    })
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                arg0.dismiss();
                                scannerView.resumeCameraPreview(Scanner3.this);
                            }
                            return true;
                        }
                    })
                    .create()
                    .show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    public void onDestroy(){
        scannerView.stopCamera();
        super.onDestroy();
    }
}
