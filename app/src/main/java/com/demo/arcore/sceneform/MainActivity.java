package com.demo.arcore.sceneform;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    private String openGlVersion;
    private static final double MIN_OPEN_GL_VERSION = 3.0;

    private static final int RC_CAMERA_PERM = 123;

    private static final String[] CAMERA_PERMISSION =
            {Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        openGlVersion = am.getDeviceConfigurationInfo().getGlEsVersion();

        try{
            if (Double.parseDouble(openGlVersion) >= MIN_OPEN_GL_VERSION) {
                requestPermissionsSetWallpaper();
//                supportFragmentManager.inTransaction { replace(R.id.fragmentContainer, ArVideoFragment()) }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Device is not supported")
                        .setMessage("OpenGL ES 3.0 or higher is required. The device is running OpenGL ES $openGlVersion.")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).show();
            }
        }catch (Exception exx){

        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CAMERA_PERM)
    private void requestPermissionsSetWallpaper() {
        if (EasyPermissions.hasPermissions(this, CAMERA_PERMISSION)) {
            // Have permission, do the thing!
            acceptPermissionsCamera();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_camera),
                    RC_CAMERA_PERM, CAMERA_PERMISSION);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

        if(requestCode == RC_CAMERA_PERM){
            acceptPermissionsCamera();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
    private void acceptPermissionsCamera(){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ArVideoFragment fragment = new ArVideoFragment();
        ft.replace(R.id.fragmentContainer, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
    }
}
