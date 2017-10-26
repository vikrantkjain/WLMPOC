package com.wlmpoc.activity;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.wlmpoc.R;
import com.wlmpoc.data.preference.AppPreference;
import com.wlmpoc.data.preference.PrefKey;
import com.wlmpoc.utility.ActivityUtils;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanActivity extends AppCompatActivity {

    private ZXingScannerView zXingScannerView;
    private int camId, frontCamId, rearCamId;
    private ViewGroup contentFrame;
    private ArrayList<Integer> mSelectedIndices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        camId = AppPreference.getInstance(this).getInteger(PrefKey.CAM_ID); // back camera by default
        if(camId == -1) {
            camId = rearCamId;
        }
        loadCams();
        zXingScannerView = new ZXingScannerView(this);
        setupFormats();
        initView();
        initListener();
    }


    private void initView() {
        contentFrame = (ViewGroup)findViewById(R.id.content_frame);

    }

    private void initListener() {

        zXingScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
            @Override
            public void handleResult(Result result) {

                String resultStr = result.getText();
                ArrayList<String> previousResult = AppPreference.getInstance(ScanActivity.this).getStringArray(PrefKey.RESULT_LIST);
                previousResult.add(resultStr);
                AppPreference.getInstance(ScanActivity.this).setStringArray(PrefKey.RESULT_LIST, previousResult);

                zXingScannerView.resumeCameraPreview(this);

                ActivityUtils.getInstance().invokeActivity(ScanActivity.this, ResultActivity.class, false);
            }
        });
    }

    public void setupFormats() {
        List<BarcodeFormat> formats = new ArrayList<>();
        if(mSelectedIndices == null || mSelectedIndices.isEmpty()) {
            mSelectedIndices = new ArrayList<>();
            for(int i = 0; i < ZXingScannerView.ALL_FORMATS.size(); i++) {
                mSelectedIndices.add(i);
            }
        }

        for(int index : mSelectedIndices) {
            formats.add(ZXingScannerView.ALL_FORMATS.get(index));
        }
        if(zXingScannerView != null) {
            zXingScannerView.setFormats(formats);
        }
    }


    private void loadCams() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCamId = i;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                rearCamId = i;
            }
        }
        AppPreference.getInstance(ScanActivity.this).setInteger(PrefKey.CAM_ID, rearCamId);

    }

    @Override
    public void onResume() {
        super.onResume();
        activateScanner();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (zXingScannerView != null) {
            zXingScannerView.stopCamera();
        }
    }

    private void activateScanner() {
        if (zXingScannerView != null) {

            if (zXingScannerView.getParent() != null) {
                ((ViewGroup) zXingScannerView.getParent()).removeView(zXingScannerView); // to prevent crush on re adding view
            }
            contentFrame.addView(zXingScannerView);

            if (zXingScannerView.isActivated()) {
                zXingScannerView.stopCamera();
            }

            zXingScannerView.startCamera(camId);
           // zXingScannerView.setFlash(isFlash);
           //zXingScannerView.setAutoFocus(isAutoFocus);
        }
    }

}
