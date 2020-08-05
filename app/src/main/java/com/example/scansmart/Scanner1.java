package com.example.scansmart;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;

import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Scanner1 extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView scannerView;
    private static String scanResult;

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
        final String[] urls = extractLinks(scanResult);

        if(urls.length == 1){
            new AlertDialog.Builder(this)
            .setTitle("Result")
            .setMessage(urls[0] + "\n\n" + "Do you want to visit this website?")
            .setPositiveButton("Visit", new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!urls[0].startsWith("http://") && !urls[0].startsWith("https://")){
                        urls[0] = "http://" + urls[0];
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urls[0]));
                        startActivity(intent);
                    }
                    else{
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urls[0]));
                        startActivity(intent);
                    }
                    scannerView.resumeCameraPreview(Scanner1.this);
                }})
            .setNeutralButton("Cancel", new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    scannerView.resumeCameraPreview(Scanner1.this);
                }})
            .setOnKeyListener(new Dialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        arg0.dismiss();
                        scannerView.resumeCameraPreview(Scanner1.this);
                    }
                    return true;
                }
            })
            .create()
            .show();
        }
        else{
            new AlertDialog.Builder(this)
                    .setTitle("Result")
                    .setMessage(scanResult)
                    .setPositiveButton("Search the web", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            scanResult = scanResult.replaceAll("[^A-Za-z0-9 ]","");
                            scanResult = scanResult.replaceAll(" ", "+").toLowerCase();
                            scanResult = "https://www.google.com/search?q=" + scanResult;
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scanResult));
                            startActivity(intent);
                            scannerView.resumeCameraPreview(Scanner1.this);
                        }})
                    .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            scannerView.resumeCameraPreview(Scanner1.this);
                        }})
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                arg0.dismiss();
                                scannerView.resumeCameraPreview(Scanner1.this);
                            }
                            return true;
                        }
                    })
                    .create()
                    .show();
        }
        return;
    }

    public String[] extractLinks(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }

        return links.toArray(new String[links.size()]);
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