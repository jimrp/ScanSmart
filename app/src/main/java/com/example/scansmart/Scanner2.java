package com.example.scansmart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.zxing.Result;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.List;

public class Scanner2 extends AppCompatActivity {

    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;
    private static String scanResult;
    ClipboardManager clipboard;
    ClipData clip;

    Button bCall, bEmail, bRescan;
    ImageView mPreviewIv;
    EditText eText;
    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner2);

        mPreviewIv = findViewById(R.id.imageIv);
        eText = findViewById(R.id.resultEt);
        bCall = findViewById(R.id.btnCall);
        bEmail = findViewById(R.id.btnEmail);
        bRescan = findViewById(R.id.btnReScan);

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        eText.setText("");
        pickCamera();

        bCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PhoneNumberUtils.isGlobalPhoneNumber(eText.getText().toString())){
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + eText.getText().toString()));
                    startActivity(Intent.createChooser(intent, "Choose application"));
                }
                else {
                    Toast.makeText(Scanner2.this, "That's not a phone number..", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(android.util.Patterns.EMAIL_ADDRESS.matcher(eText.getText().toString()).matches()){
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + eText.getText().toString()));
                    startActivity(Intent.createChooser(intent, "Choose application"));
                }
                else {
                    Toast.makeText(Scanner2.this, "That's not an email address..", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eText.setText("");
                pickCamera();
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
                    //result
                    scanResult = sb.toString();
                    scanResult = scanResult.replaceAll("\n","");
                    scanResult = scanResult.replaceAll(" ","");
                    eText.setText(scanResult);
                    if(PhoneNumberUtils.isGlobalPhoneNumber(scanResult)){
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + scanResult));
                        startActivity(Intent.createChooser(intent, "Choose application"));
                    }
                    else if (android.util.Patterns.EMAIL_ADDRESS.matcher(scanResult).matches()) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:" + scanResult));
                        startActivity(Intent.createChooser(intent, "Choose application"));
                    }
                    else {
                        Toast.makeText(this, "No phone or email found..", Toast.LENGTH_SHORT).show();
                    }
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