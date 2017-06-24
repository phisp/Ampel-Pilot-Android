package de.hsaugsburg.ampelpilot;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class SplashScreen extends Activity {
    private static final String TAG = "SplashScreen";
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length > 0)
            switch (permissions[0]) {
                case Manifest.permission.CAMERA:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission granted, onResume will start next activity
                    } else {
                        // permission denied by the user
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                showCameraRequestDialog();
                            }
                        });
                        break;
                    }
            }
    }

    private void showCameraRequestDialog() {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Kein Kamerazugriff")
                .setMessage("Ampelpilot benötigt Zugriff auf die Kamera.")
                .setPositiveButton("Zugriff anfordern.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delay so that the alert-dialog has time to close
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                ActivityCompat.requestPermissions(SplashScreen.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                            }
                        });
                    }
                })
                .setNeutralButton("App-Einstellungen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent i = new Intent();
                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(i);
                    }
                })
                .create();
        dlg.show();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraActivity();
        }
    }

    void startCameraActivity() {
        Intent i = new Intent(this, LdActivity.class);
        startActivity(i);
        finish();
    }
}
