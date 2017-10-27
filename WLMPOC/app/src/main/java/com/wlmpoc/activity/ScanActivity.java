package com.wlmpoc.activity;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.wlmpoc.R;
import com.wlmpoc.data.preference.AppPreference;
import com.wlmpoc.data.preference.PrefKey;
import com.wlmpoc.utility.CodeGenerator;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanActivity extends AppCompatActivity {

    private ZXingScannerView zXingScannerView;
    private int camId, frontCamId, rearCamId;
    private FrameLayout contentFrame;
    private ArrayList<Integer> mSelectedIndices;
    private RelativeLayout rlBottomView;
    private DisplayMetrics displaymetrics;
    private ImageView outputBitmap;
    private TextView outputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        camId = AppPreference.getInstance(this).getInteger(PrefKey.CAM_ID); // back camera by default
        if (camId == -1) {
            camId = rearCamId;
        }
        loadCams();
        zXingScannerView = new ZXingScannerView(this);
        zXingScannerView.getFormats().remove(BarcodeFormat.MAXICODE);
        zXingScannerView.getFormats().remove(BarcodeFormat.QR_CODE);
        setupFormats();
        initView();
        initListener();
    }


    private void initView() {

        contentFrame = (FrameLayout) findViewById(R.id.content_frame);
        rlBottomView = (RelativeLayout) findViewById(R.id.rlBottomView);
        outputBitmap = (ImageView) findViewById(R.id.outputBitmap);
        outputText = (TextView) findViewById(R.id.outputText);

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                (int) (displaymetrics.heightPixels * 40) / 100);
        zXingScannerView.setLayoutParams(param);

        LinearLayout.LayoutParams param_content_frame = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                (int) (displaymetrics.heightPixels * 40) / 100);
        contentFrame.setLayoutParams(param_content_frame);

        LinearLayout.LayoutParams param_bottom_view = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                (int) (displaymetrics.heightPixels * 60) / 100);
        rlBottomView.setLayoutParams(param_bottom_view);

        RelativeLayout.LayoutParams param_output = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                (int) (displaymetrics.heightPixels * 30) / 100);
        outputBitmap.setLayoutParams(param_output);


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

                String lastResult = previousResult.get(previousResult.size() - 1);
                if (lastResult.length() != 0) {
                    generateCode(lastResult.toString());
                }

                //ActivityUtils.getInstance().invokeActivity(ScanActivity.this, ResultActivity.class, false);
            }
        });
    }

    public void setupFormats() {
        List<BarcodeFormat> formats = new ArrayList<>();
        if (mSelectedIndices == null || mSelectedIndices.isEmpty()) {
            mSelectedIndices = new ArrayList<>();
            for (int i = 0; i < ZXingScannerView.ALL_FORMATS.size(); i++) {
                mSelectedIndices.add(i);
            }
        }

        for (int index : mSelectedIndices) {
            formats.add(ZXingScannerView.ALL_FORMATS.get(index));
        }
        if (zXingScannerView != null) {
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

    /* GENERATE QR CODE FROM RESULT */

    private static final int TYPE_QR = 0, TYPE_BAR = 1;
    private static int TYPE = TYPE_BAR;
    private void generateCode(final String result) {

        CodeGenerator codeGenerator = new CodeGenerator();
        if (TYPE == TYPE_BAR) {
            codeGenerator.generateBarFor(result);
        } else {
            codeGenerator.generateQRFor(result);
        }
        codeGenerator.setResultListener(new CodeGenerator.ResultListener() {
            @Override
            public void onResult(Bitmap bitmap) {

                outputBitmap.setImageBitmap(bitmap);
                outputText.setText(result);

            }
        });
        codeGenerator.execute();
    }

}
