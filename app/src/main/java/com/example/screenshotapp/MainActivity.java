package com.example.screenshotapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    int view = R.layout.activity_main;
    ImageView screenShot;
    TextView textView;
    public final static int REQUEST_CODE = 10;
    private static final int REQUEST_SCREENSHOT=59706;
    private MediaProjectionManager mgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(view);
        requestPermission();
        checkDrawOverlayPermission(this);
        FirebaseApp.initializeApp(this);
//        startService(new Intent(this, FloatingWidgetService.class));
        mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        startActivityForResult(mgr.createScreenCaptureIntent(),
                REQUEST_SCREENSHOT);
        final LinearLayout parent = findViewById(R.id.parent);
        textView = findViewById(R.id.text);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                if (launchIntent != null) {
//                    startActivity(new Intent(MainActivity.this, ScreenshotActivity.class));
//                    startActivity(launchIntent);
                } else {
                    Toast.makeText(MainActivity.this, "There is no package available in android", Toast.LENGTH_LONG).show();
                }
            }
        });
//        final LinearLayout parent = findViewById(R.id.parent);
//        System.out.println(parent);
//        screenShot=findViewById(R.id.screenShot);
//        textView=findViewById(R.id.text);
//        textView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                textView.setText("It is screen shot text");
//                screenShot(parent);
//            }
//        });
    }
    public void screenShot(final View view) {
        final Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        p.setColor(Color.BLACK);
        p.setStrokeWidth(10);

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        screenShot.setImageBitmap(bitmap);
        textView.setText("click");

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseApp.initializeApp(this);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                String resultText = firebaseVisionText.getText();
                                for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
                                    String blockText = block.getText();
                                    System.out.println("************"+blockText+"************");
//                                    Float blockConfidence = block.getConfidence();
//                                    List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//                                    Point[] blockCornerPoints = block.getCornerPoints();
                                    Rect blockFrame = block.getBoundingBox();
                                    System.out.println("************"+blockFrame.centerX()+","+blockFrame.centerY()+"************");
                                    for (FirebaseVisionText.Line line: block.getLines()) {
                                        String lineText = line.getText();
//                                        Float lineConfidence = line.getConfidence();
//                                        List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//                                        Point[] lineCornerPoints = line.getCornerPoints();
                                        Rect lineFrame = line.getBoundingBox();
                                        canvas.drawRect(lineFrame,p);
                                        view.draw(canvas);
                                        for (FirebaseVisionText.Element element: line.getElements()) {
                                            String elementText = element.getText();
//                                            Float elementConfidence = element.getConfidence();
//                                            List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//                                            Point[] elementCornerPoints = element.getCornerPoints();
                                            Rect elementFrame = element.getBoundingBox();
                                            System.out.println("*****"+elementText+" - "+elementFrame.centerX()+","+elementFrame.centerY());
                                        }
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });



    }

    private void requestPermission(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.FOREGROUND_SERVICE

                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                        }
                        for (int i=0;i<report.getDeniedPermissionResponses().size();i++) {
                            Log.d("dennial permision res", report.getDeniedPermissionResponses().get(i).getPermissionName());
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token)
                    {
                        token.continuePermissionRequest();
                    }
                }).check();



    }

    public void checkDrawOverlayPermission(Context context) {
        /** check if we already  have permission to draw over other apps */
        if (!Settings.canDrawOverlays(context)) {
            /** if not construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        /** check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
       /** if so check once again if we have permission */
            if (Settings.canDrawOverlays(this)) {
                Log.v("App", "Requesting Permission" + Settings.canDrawOverlays(this));
            }
            Log.v("App", "Requesting Permission" + Settings.canDrawOverlays(this));
        }
        else if (requestCode==REQUEST_SCREENSHOT) {
            if (resultCode==RESULT_OK) {
                Intent i=
                        new Intent(this, ScreenshotService.class)
                                .putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(ScreenshotService.EXTRA_RESULT_INTENT, data);
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");

                if(launchIntent != null)
                    startActivity(launchIntent);
                startService(i);
            }
        }

        finish();
    }

}
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.os.Bundle;
//
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import android.os.Environment;
//
//import android.provider.Settings;
//import android.util.Log;
//import android.view.View;
//
//
//import android.widget.Button;
//import android.widget.Toast;
//
//import com.karumi.dexter.Dexter;
//import com.karumi.dexter.MultiplePermissionsReport;
//import com.karumi.dexter.PermissionToken;
//import com.karumi.dexter.listener.PermissionDeniedResponse;
//import com.karumi.dexter.listener.PermissionGrantedResponse;
//import com.karumi.dexter.listener.PermissionRequest;
//import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
//import com.karumi.dexter.listener.single.PermissionListener;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//
//import java.util.Date;
//import java.util.List;
//
//public class MainActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
////        if(ContextCompat.checkSelfPermission(MainActivity.this,
////                Manifest.permission.SYSTEM_ALERT_WINDOW )!= PackageManager.PERMISSION_GRANTED){
////            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.SYSTEM_ALERT_WINDOW)){
////                ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.SYSTEM_ALERT_WINDOW},2003);
////            }else {
////                System.out.println("request");
////                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, 2003);
////            }
////
////        }
//
////        Intent svc = new Intent(this, OverlayShowingService.class);
////        startService(svc);
////        finish();
//        setContentView(R.layout.activity_main);
//        Button button = findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takeScreenshot();
//            }
//        });
//        requestPermission();
////
////        if(ContextCompat.checkSelfPermission(MainActivity.this,
////                Manifest.permission.WRITE_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED){
////            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
////                ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
////            }else {
////                System.out.println("request");
////                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
////            }
////
////        }
////        if(ContextCompat.checkSelfPermission(MainActivity.this,
////                Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED){
////            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
////                ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},2);
////            }else {
////                System.out.println("request");
////                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
////            }
////
////        }
//    }
//
//    private void requestPermission(){
//        Dexter.withActivity(this)
//                .withPermissions(
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE
//                )
//                .withListener(new MultiplePermissionsListener() {
//                    @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
//                        if (report.areAllPermissionsGranted()) {
//                            // do you work now
//                        }
//                        for (int i=0;i<report.getDeniedPermissionResponses().size();i++) {
//                            Log.d("dennial permision res", report.getDeniedPermissionResponses().get(i).getPermissionName());
//                        }
//                        // check for permanent denial of any permission
//                        if (report.isAnyPermissionPermanentlyDenied()) {
//                            // permission is denied permenantly, navigate user to app settings
//                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                                    Uri.fromParts("package", getPackageName(), null));
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        }
//                    }
//                    @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token)
//                    {
//                        token.continuePermissionRequest();
//                    }
//                }).check();
//
//
//
//    }
//
//    private void takeScreenshot() {
//        Date now = new Date();
//        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
//
//        try {
//            // image naming and path  to include sd card  appending name you choose for file
//            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";
//
//            // create bitmap screen capture
//            View v1 = getWindow().getDecorView().getRootView();
//            v1.setDrawingCacheEnabled(true);
//            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
//            v1.setDrawingCacheEnabled(false);
//
//            File imageFile = new File(mPath);
//
//            FileOutputStream outputStream = new FileOutputStream(imageFile);
//            int quality = 100;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
//            outputStream.flush();
//            outputStream.close();
//
//            openScreenshot(imageFile);
//        } catch (Throwable e) {
//            // Several error may come out with file handling or DOM
//            e.printStackTrace();
//        }
//
//    }
//
//    private void openScreenshot(File imageFile) {
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_VIEW);
//        Uri uri = Uri.fromFile(imageFile);
//        intent.setDataAndType(uri, "image/*");
//        startActivity(intent);
//    }
//
//    private void screenShot(){
//        Date date = new Date();
//        CharSequence now = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss",date);
//        String filename= Environment.getExternalStorageDirectory() + "/ScreeShooter/" + now + ".jpg";
//        System.out.println("**********"+filename+"************");
//        View root = getWindow().getDecorView();
//        root.setDrawingCacheEnabled(true);
//        Bitmap bitmap = Bitmap.createBitmap(root.getDrawingCache());
//        root.setDrawingCacheEnabled(false);
//
//        File file = new File(filename);
//        file.getParentFile().mkdir();
//
//        try{
//            FileOutputStream fileOutputStream = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
//            Uri uri = Uri.fromFile(file);
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(uri,"image/*");
//            startActivity(intent);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//}
