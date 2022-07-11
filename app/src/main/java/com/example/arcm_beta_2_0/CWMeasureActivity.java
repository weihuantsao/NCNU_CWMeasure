package com.example.arcm_beta_2_0;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.example.arcm_beta_2_0.common.helpers.StoreUtils;
import com.example.arcm_beta_2_0.model.ImageDAO;

import static com.example.arcm_beta_2_0.GlobalVariable.BINARY;
import static com.example.arcm_beta_2_0.GlobalVariable.CANNY;
import static com.example.arcm_beta_2_0.GlobalVariable.currentTimeFormat;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;
import static com.example.arcm_beta_2_0.ImageView.Binary;
import static com.example.arcm_beta_2_0.ImageView.Canny;

public class CWMeasureActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = CWMeasureActivity.class.getName();
    public  ImageView iv;
    private SeekBar mSeekBar1;

    private int defaultThresh = 50;

    //建立共用變數類別
    GlobalVariable gv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cwmeasure);
        gv = (GlobalVariable)getApplicationContext();
        mSeekBar1 = findViewById(R.id.seekBar);
        mSeekBar1.setOnSeekBarChangeListener(this);
        init(); // 初始化
    }

    @Override
    protected void onDestroy() { // 當顯示圖片的 Activity 結束被釋放時
        super.onDestroy();
        ImageView.reset(); // 重新設定 ImageView 有關靜態部份共用的變數
    }

    @Override
    protected void onPause() { // onPause() 為 TabActivity 所使用
        iv.setVisibility(View.INVISIBLE); // 隱藏 Canny 圖片
        super.onPause();
    }

    @Override
    protected void onResume() { // onResume() 為 TabActivity 所使用
        super.onResume();
        iv.setVisibility(View.VISIBLE); // 顯示 Canny 圖片
    }

    public void historyOnClick(View view) {
        startActivity(new Intent(CWMeasureActivity.this, HistoryActivity.class));
        this.finish();
    }

    public void changeTypeOnClick(View view) {
        Log.d(TAG, "changeTypeOnClick: ");
        Bitmap b = imgObj.getBitmap().copy(imgObj.getBitmap().getConfig(), true);

        switch (imgObj.getImage_type()) {
            case CANNY:
                imgObj.setImage_type(BINARY);
                imgObj.setAfterProcessBitmap(Binary(b, defaultThresh));
                mSeekBar1.setMax(255);
                mSeekBar1.setProgress(130);
                break;
            case BINARY:
                imgObj.setImage_type(CANNY);
                imgObj.setAfterProcessBitmap(Canny(b, defaultThresh));
                mSeekBar1.setMax(100);
                mSeekBar1.setProgress(50);
                break;
        }
    }

    public void showAfterOnClick(View view) {
        Log.d(TAG, "showAfterOnClick: ");

        if(iv.getDrawBitmap().sameAs(imgObj.getBitmap())){
            iv.setBitmap(imgObj.getAfterProcessBitmap());
        } else {
            iv.setBitmap(imgObj.getBitmap());
        }

        iv.updateView();
    }

    public void adjustmentOnClick(View view) {
        Log.d(TAG, "adjustmentOnClick: ");

        if (mSeekBar1.getVisibility() == View.VISIBLE) {
            mSeekBar1.setVisibility(View.INVISIBLE);
        } else {
            mSeekBar1.setVisibility(View.VISIBLE);
        }

    }


    private void init() {  // 初始化
        iv = new ImageView(this); // 產生顯示圖片物件
        iv.setBitmap(imgObj.getBitmap());
        ((FrameLayout) findViewById(R.id.origin)).addView(iv); // 將顯示圖片物件設定到 Layout 上

        ImageDAO tmpImageDAO = new ImageDAO();

        // save Binary bitmap
        tmpImageDAO.setName(currentTimeFormat() + "-" + "1B-first.png");
        Bitmap binaryBitmap = imgObj.getBitmap().copy(imgObj.getBitmap().getConfig(), true);
        tmpImageDAO.setBitmap(Binary(binaryBitmap, defaultThresh));
        StoreUtils.storeBitmap(this, tmpImageDAO);

        // save Canny bitmap
        tmpImageDAO = new ImageDAO();
        tmpImageDAO.setName(currentTimeFormat() + "-" + "1C-first.png");
        Bitmap cannyBitmap = imgObj.getBitmap().copy(imgObj.getBitmap().getConfig(), true);
        tmpImageDAO.setBitmap(Canny(cannyBitmap, 100));
        StoreUtils.storeBitmap(this, tmpImageDAO);

        switch (imgObj.getImage_type()) {
            case CANNY:
                imgObj.setAfterProcessBitmap(cannyBitmap);
                mSeekBar1.setMax(100);
                break;
            case BINARY:
                imgObj.setAfterProcessBitmap(binaryBitmap);
                mSeekBar1.setMax(255);
                break;
            default:

        }

//        findViewById(R.id.history).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(CWMeasureActivity.this, HistoryActivity.class));
//                CWMeasureActivity.this.finish();
//            }
//        });

//        findViewById(R.id.Adjustment).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Bundle bundle = getIntent().getExtras();
//                if (bundle.getString("Image processing").equals(CANNY)) {
//                    mSeekBar1 = (SeekBar) findViewById(R.id.seekBar);
//                    mSeekBar1.setVisibility(View.VISIBLE);
//                    mSeekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//                        @Override
//                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                            gv.setThreshold1(progress);
//                        }
//
//                        @Override
//                        public void onStartTrackingTouch(SeekBar seekBar) {
//
//                        }
//
//                        @Override
//                        public void onStopTrackingTouch(SeekBar seekBar) {
//
//                        }
//                    });
//                } else {
//                    mSeekBar2 = (SeekBar) findViewById(R.id.seekBar2);
//                    mSeekBar2.setVisibility(View.VISIBLE);
//                    mSeekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//                        @Override
//                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                            gv.setThreshold2(progress);
//                        }
//
//                        @Override
//                        public void onStartTrackingTouch(SeekBar seekBar) {
//
//                        }
//
//                        @Override
//                        public void onStopTrackingTouch(SeekBar seekBar) {
//
//                        }
//                    });
//                }
//            }
//        });

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.w(TAG, "onProgressChanged: progress = " + progress);
        defaultThresh = progress;
        Bitmap b = imgObj.getBitmap().copy(imgObj.getBitmap().getConfig(), true);

        switch (imgObj.getImage_type()) {
            case BINARY:
                imgObj.setAfterProcessBitmap(Binary(b, defaultThresh));
                break;
            case CANNY:
                imgObj.setAfterProcessBitmap(Canny(b, defaultThresh));
                break;
        }

        iv.setBitmap(imgObj.getAfterProcessBitmap());
        iv.updateView();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Bitmap b = imgObj.getBitmap().copy(imgObj.getBitmap().getConfig(), true);
        switch (imgObj.getImage_type()) {
            case CANNY:
                imgObj.setAfterProcessBitmap(Canny(b, defaultThresh));
                break;
            case BINARY:
                imgObj.setAfterProcessBitmap(Binary(b, defaultThresh));
                break;
        }
    }
}
