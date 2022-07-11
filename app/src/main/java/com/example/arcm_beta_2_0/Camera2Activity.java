package com.example.arcm_beta_2_0;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.example.arcm_beta_2_0.common.Camera2GLSurfaceView;
import com.example.arcm_beta_2_0.common.Camera2Proxy;
import com.example.arcm_beta_2_0.common.ImageUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;

import static com.example.arcm_beta_2_0.GlobalVariable.currentTimeFormat;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;
import static com.example.arcm_beta_2_0.GlobalVariable.imgURI;

public class Camera2Activity extends AppCompatActivity {

    private static final String TAG = "Camera2Activity";

    private Camera2GLSurfaceView mCameraView;
    private Camera2Proxy mCameraProxy;

    private Bundle bundle = new Bundle();
    private boolean hasPic = false;

    Uri myUri;

    //private boolean captureSessionChangesPossible = true;
    CapturePictureThread capturePictureThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera2);

        mCameraView = findViewById(R.id.camera_view);
        mCameraProxy = mCameraView.getCameraProxy();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!hasPic) {
            hasPic = true;
            capturePictureThread = new CapturePictureThread();
            capturePictureThread.start();
        }
    }

    private void goCWMeasureActivity() {
        Intent intent = new Intent(this, CWMeasureActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, 0);
        this.finish();
    }

    View.OnAttachStateChangeListener onAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
            mCameraProxy.setImageAvailableListener(mOnImageAvailableListener);
            mCameraProxy.captureStillPicture(); // 拍照
        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    };


    class CapturePictureThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(500);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraProxy.setImageAvailableListener(mOnImageAvailableListener);
                        mCameraProxy.captureStillPicture(); // 拍照
                        Toast.makeText(Camera2Activity.this, "Take Pic Success", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCrop() {
        CropImage.activity(myUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setActivityTitle("Crop Image")
                //.setFixAspectRatio(true)
                .setCropMenuCropButtonTitle("Done")
                .start(this);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener
            () {
        @Override
        public void onImageAvailable(ImageReader reader) {
            new ImageSaveTask().execute(reader.acquireNextImage()); // 保存图片
        }
    };

    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap;
            long time = System.currentTimeMillis();
            if (mCameraProxy.isFrontCamera()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Log.d(TAG, "BitmapFactory.decodeByteArray time: " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
                // 前置摄像头需要左右镜像
                Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, 0, true, true);
                Log.d(TAG, "rotateBitmap time: " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
                ImageUtils.saveBitmap(rotateBitmap);
                Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));
                rotateBitmap.recycle();
            } else {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ImageUtils.saveImage(Camera2Activity.this,bytes);
                Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));

                myUri = Uri.fromFile(new File(imgURI));
                startCrop();

            }
            images[0].close();
            return bitmap;
        }

        //@Override
        //protected void onPostExecute(Bitmap bitmap) { mPictureIv.setImageBitmap(bitmap); }
    }



    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCameraProxy.releaseCamera();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = BitmapFactory.decodeFile(resultUri.getPath());

                imgObj.setName(imgURI);
                imgObj.setBitmap(bitmap);

                goCWMeasureActivity();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private synchronized void waitUntilCameraCaptureSessionIsActive() {
        while (!mCameraProxy.captureSessionChangesPossible) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
            }
        }
    }


}