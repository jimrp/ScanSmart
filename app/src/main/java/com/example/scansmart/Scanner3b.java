package com.example.scansmart;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.util.ArrayList;
import java.util.List;


public class Scanner3b extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;

    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;
    private static String scanResult;
    ClipboardManager clipboard;
    ClipData clip;
    public String type, ssid, pass = "";
    public WifiConfiguration conf;


    Button bRescan, bCopy;
    ImageView mPreviewIv, bRescanWifi;
    EditText eText;
    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner3b);

        mPreviewIv = findViewById(R.id.imageIv);
        eText = findViewById(R.id.resultEt);
        bCopy = findViewById(R.id.btnCopy);
        bRescan = findViewById(R.id.btnReScan);
        bRescanWifi = findViewById(R.id.reScan);

        listView = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (!wifiManager.isWifiEnabled()){
            Toast.makeText(this, "WiFi is disabled. You need to enable it.", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        scanWiFi();

        eText.setText("");
        pickCamera();

        bRescanWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wifiManager.isWifiEnabled()){
                    Toast.makeText(Scanner3b.this, "WiFi is disabled. You need to enable it.", Toast.LENGTH_SHORT).show();
                    wifiManager.setWifiEnabled(true);
                }
                scanWiFi();
            }
        });

        bCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pass = eText.getText().toString();
                clip = ClipData.newPlainText("text", eText.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Scanner3b.this, "Copied Password : " + pass, Toast.LENGTH_SHORT).show();
            }
        });
        bRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eText.setText("");
                pickCamera();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ssid = results.get(i).SSID;
                type = results.get(i).capabilities;

                conf = new WifiConfiguration();
                conf.SSID = "\"" + ssid + "\"";
                if(type.contains("WEP")){
                    conf.wepKeys[0] = "\"" + pass + "\"";
                    conf.wepTxKeyIndex = 0;
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                }
                else if(type.contains("WPA")){
                    conf.preSharedKey = "\""+ pass +"\"";
                }
                else{
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int netId = wifiManager.addNetwork(conf);
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();

                Toast.makeText(Scanner3b.this, "Trying to use '" + pass + "' to connect to '" + ssid + "' ...", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.addImage){
            pickGallery();
        }
        return super.onOptionsItemSelected(item);
    }

//    private void showImageImportDialog() {
//        String[] items = {"Camera", "Gallery"};
//        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//        dialog.setTitle("Select Image");
//        dialog.setItems(items, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if(which == 0){
//                    pickCamera();
//                }
//                if(which == 1){
//                    pickGallery();
//                }
//            }
//        });
//        dialog.create().show();
//    }

    private void pickGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To text");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void scanWiFi(){
        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for(ScanResult scanResult : results){
                arrayList.add(scanResult.SSID);
                adapter.notifyDataSetChanged();
            }
        }
    };

    //results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mPreviewIv.setImageURI(resultUri);

                BitmapDrawable bitmapDrawable = (BitmapDrawable)mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if(!recognizer.isOperational()){
                    Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show();
                }
                else{
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i<items.size(); i++){
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    //show result
                    String sb2 = sb.toString();
                    scanResult = sb2;
                    if (sb2.length() > 0) {
                        String str = scanResult;
                        scanResult = str.substring(0, str.length() - 1);
                    }
                    String str2 = scanResult;
                    pass = str2;

                    clip = ClipData.newPlainText("text", str2);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Password Copied", Toast.LENGTH_SHORT).show();
                    eText.setText(scanResult);
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error = result.getError();
                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}