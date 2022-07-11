/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.arcm_beta_2_0;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arcm_beta_2_0.common.helpers.CameraPermissionHelper;
import com.example.arcm_beta_2_0.common.helpers.DisplayRotationHelper;
import com.example.arcm_beta_2_0.common.helpers.SnackbarHelper;
import com.example.arcm_beta_2_0.common.helpers.StoreUtils;
import com.example.arcm_beta_2_0.common.helpers.TapHelper;
import com.example.arcm_beta_2_0.common.rendering.BackgroundRenderer;
import com.example.arcm_beta_2_0.common.rendering.ObjectRenderer;
import com.example.arcm_beta_2_0.common.rendering.PlaneRenderer;
import com.example.arcm_beta_2_0.common.rendering.PointCloudRenderer;
import com.example.arcm_beta_2_0.db.DbUtils;
import com.example.arcm_beta_2_0.model.ImageDAO;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.arcm_beta_2_0.GlobalVariable.currentTimeFormat;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;
import static com.example.arcm_beta_2_0.GlobalVariable.BINARY;
import static com.example.arcm_beta_2_0.GlobalVariable.CANNY;


/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    //顯示LOG
    private TextView tv_result;

    private boolean installRequested;

    private Session session;
    private Config config;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};

    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    private int mWidth;
    private int mHeight;
    private  boolean capturePicture = false;
    public static final String Save_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/AR";
    private Bundle bundle = new Bundle();
    private ProgressDialog progress;
    private SensorManager SM;
    private TextView showAccelerometerX,showAccelerometerY,canny,binary,origin;
    private Canvas canvas;

    // Anchors created from taps used for object placing with a given color.
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;

        public ColoredAnchor(Anchor a, float[] color4f) {
            this.anchor = a;
            this.color = color4f;
        }
    }
    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

    //建立共用變數類別
    GlobalVariable gv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gv = (GlobalVariable)getApplicationContext();
        /*新增*/
        tv_result = findViewById(R.id.tv_result);
        canny = (TextView)findViewById(R.id.canny);
        binary = (TextView)findViewById(R.id.binary);
        origin = (TextView)findViewById(R.id.origin);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(this);

        // Set up tap listener.
        tapHelper = new TapHelper(this);
        surfaceView.setOnTouchListener(tapHelper);

        imgObj = new ImageDAO();
        imgObj.setImage_type(CANNY);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;
        if(OpenCVLoader.initDebug()) {
            Log.d("Test","OpenCV執行成功！");
        } else {
            Log.d("Test","OpenCV執行失敗！");
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        showAccelerometerX = (TextView)findViewById(R.id.AccelerometerX);
        showAccelerometerY = (TextView)findViewById(R.id.AccelerometerY);
    }

    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            DecimalFormat df = new DecimalFormat("0.0");
            showAccelerometerX.setText("");
            showAccelerometerX.append("X : " + df.format(event.values[1]) + " ");
            showAccelerometerY.setText("");
            showAccelerometerY.append("Y : " + df.format(event.values[2]) + "");
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onResume() {
        super.onResume();
        SM.registerListener(listener, SM.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);
                config = new Config(session);
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                boolean enableAutoFocus = true;
                if (enableAutoFocus) {
                    config.setFocusMode(Config.FocusMode.AUTO);
                } else {
                    config.setFocusMode(Config.FocusMode.FIXED);
                }
                session.configure(config);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
        SM.unregisterListener(listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/point_512px.png");
            pointCloudRenderer.createOnGlThread(this);

            virtualObject.createOnGlThread(this, "models/cube2.obj", "models/cube_green.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            // -----
//            Display display = getWindowManager().getDefaultDisplay();
//            android.graphics.Point size = new android.graphics.Point();
//            display.getRealSize(size);
//            int width = size.x;
//            int height = size.y;
//            final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
//            // Gets all excluding insets
//            final WindowInsets windowInsets = metrics.getWindowInsets();
//            Insets insets = windowInsets.getInsetsIgnoreVisibility(WindowInsets.Type.navigationBars()
//                    | WindowInsets.Type.displayCutout());
//
//            int insetsWidth = insets.right + insets.left;
//            int insetsHeight = insets.top + insets.bottom;
//
//            // Legacy size that Display#getSize reports
//            final Rect bounds = metrics.getBounds();
//            final Size legacySize = new Size(bounds.width() - insetsWidth,
//                    bounds.height() - insetsHeight);
            // -----
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Handle one tap per frame.
            customhandleTap(frame, camera,width,height);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

//            if (capturePicture) {
//                capturePicture = false;
//                Bitmap b = createBitmapFromGLSurface(mWidth, mHeight, gl);
//                Bitmap c = addRedPoint(b,mWidth,mHeight);
//                SavePicture(c);
//            }

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                        this, TrackingStateHelper.getTrackingFailureReasonString(camera));
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                messageSnackbarHelper.hide(this);
            } else {
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
            }

            // Visualize planes.
            planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            float distanceCm1 = 0; // 單位是公分
            DecimalFormat df = new DecimalFormat("0.000###");
            if(anchors.size() == 2){
                //第一個點
                Pose Xpoint0 = getPose(anchors.get(0).anchor);
                //第二個點
                Pose Xpoint1 = getPose(anchors.get(1).anchor);
                //距離
                distanceCm1 = (float)(getDistance(Xpoint0, Xpoint1) * 1000)/10.0f; // 為了下面setRealPixelUnit的型態做更動
                //顯示單位換算跟顯示實際距離
                imgObj.setRealPixelUnit(distanceCm1/500); //需要float型態，故上面先切換成float型態
                showResult("1px width : "+df.format(distanceCm1/500) + " \n\n"+"Distance : "+distanceCm1); // 待更動
                bundle.putDouble("Distance_result",(distanceCm1/500));
            }

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (ColoredAnchor coloredAnchor : anchors) {
                if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

                // Update and draw the model and its shadow.
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    public void onClickCapture(View view) {
//        progress = new ProgressDialog(MainActivity.this);
//        progress.setMessage("影像處理中．．．");
//        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progress.setCanceledOnTouchOutside(false);
//        progress.show();
//        capturePicture = true;


        Intent intent = new Intent(this, Camera2Activity.class);
        ((AppCompatActivity)this).startActivity(intent);
        MainActivity.this.finish();
    }
    // 選擇Canny圖作為底圖顯示
    public void selectCannyImage(View view){
        binary.setTextColor(Color.WHITE);
        canny.setTextColor(Color.YELLOW);
        origin.setTextColor(Color.WHITE);
        imgObj.setImage_type(CANNY);
    }
    // 選擇Binary圖作為底圖顯示
    public void selectBinaryImage(View view){
        canny.setTextColor(Color.WHITE);
        binary.setTextColor(Color.YELLOW);
        origin.setTextColor(Color.WHITE);
        imgObj.setImage_type(BINARY);
    }
    // 選擇Origin圖作為底圖顯示
    public void selectOriginImage(View view){
        canny.setTextColor(Color.WHITE);
        binary.setTextColor(Color.WHITE);
        origin.setTextColor(Color.YELLOW);
        imgObj.setImage_type("Origin");
    }

    public void SavePicture(Bitmap b) {
        int ret;
        DbUtils dbUtils = new DbUtils(this);

        imgObj.setName(currentTimeFormat() + "-" + "1-first.png");
        imgObj.setBitmap(b);

        ret = StoreUtils.storeBitmap(this, imgObj);

        if(ret == 0) {
            // save to DB
            dbUtils.set(imgObj);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "照片保存成功", Toast.LENGTH_SHORT).show();
                }
            });
        }
        progress.dismiss();
        goCWMeasureActivity();
    }


    //算出兩點距離
    private double getDistance(Pose pose0, Pose pose1){
        float dx = pose0.tx() - pose1.tx();
        float dy = pose0.ty() - pose1.ty();
        float dz = pose0.tz() - pose1.tz();
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }
    //自動在螢幕上點擊兩點，在算出距離
    private void customhandleTap(Frame frame, Camera camera,int Width,int height) {
        //螢幕正中心 X
        float Center_X = Width/2;
        //螢幕正中心 Y
        float Center_Y = height/2;
        //自動點擊X座標
        float X[] = {Center_X - 250 ,Center_X + 250,Center_X,Center_X};//290.0,790.0,540.0,540.0
        //自動點擊Y座標
        float Y[] = {Center_Y,Center_Y,Center_Y - 250,Center_Y + 250};//1074.0,1074.0,824.0,1324.0

        Log.d("X[]","X[]:"+X[0]+X[1]+X[2]+X[3]);
        Log.d("Y[]","Y[]:"+Y[0]+Y[1]+Y[2]+Y[3]);


        for(int i = 0; i < anchors.size(); i++){
            anchors.get(i).anchor.detach();
            anchors.remove(i);
        }

        if(camera.getTrackingState() == TrackingState.TRACKING){
            for(int i=0;i<2;i++){
                for (HitResult hit : frame.hitTest(X[i],Y[i])) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    // Creates an anchor if a plane or an oriented point was hit.
                    //檢測是否點擊到平面
                    if ((trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                            && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                            || (trackable instanceof Point
                            && ((Point) trackable).getOrientationMode()
                            == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (anchors.size() > 1) {
                            anchors.get(0).anchor.detach();
                            anchors.remove(0);
                        }

                        // Assign a color to the object for rendering based on the trackable type
                        // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
                        // for AR_TRACKABLE_PLANE, it's green color.
                        float[] objColor;
                        if (trackable instanceof Point) {
                            objColor = new float[] {66.0f, 133.0f, 244.0f, 255.0f};
                        } else if (trackable instanceof Plane) {
                            objColor = new float[] {139.0f, 195.0f, 74.0f, 255.0f};
                        } else {
                            objColor = DEFAULT_COLOR;
                        }

                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3D model
                        // in the correct position relative both to the world and to the plane.
                        //h_result.add(new ColoredAnchor(hit.createAnchor(), objColor));
                        anchors.add(new ColoredAnchor(hit.createAnchor(), objColor));

                        break;
                    } else {

                        if (anchors.size() > 1) {
                            anchors.get(0).anchor.detach();
                            anchors.remove(0);
                        }
                        break;
                    }
                }

            }
        }
    }

    //取得 Anchor 姿態
    private final float[] mPoseTranslation = new float[3];
    private final float[] mPoseRotation = new float[4];
    private Pose getPose(Anchor anchor){
        Pose pose = anchor.getPose();
        pose.getTranslation(mPoseTranslation, 0);
        pose.getRotationQuaternion(mPoseRotation, 0);
        return new Pose(mPoseTranslation, mPoseRotation);
    }
    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    private void showResult(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_result.setText(result);
            }
        });
    }

    private Bitmap createBitmapFromGLSurface(int w, int h, GL10 gl) {
        int[] bitmapBuffer = new int[w * h];
        int[] bitmapSource = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    private void goCWMeasureActivity() {
        Intent intent = new Intent(this, CWMeasureActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, 0);
        this.finish();
    }

    public Bitmap addRedPoint(Bitmap b,int width, int height) {
        Bitmap mutableBitmap = b.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint2 = new Paint();
        paint2.setColor(Color.RED);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(2f);
        canvas.drawPoint(width/2+250,height/2,paint2);
        canvas.drawPoint(width/2-250,height/2,paint2);
        canvas.drawCircle(width/2+250,height/2,5f,paint2);
        canvas.drawCircle(width/2-250,height/2,5f,paint2);
//        canvas.drawRect((float) width/4, (float) height/2 + 10 , (float) width/4 + 10 , (float) height/2 - 10, paint2);
//        canvas.drawRect((float) ((float) width*0.75), (float) height/2 + 10 , (float) ((float) width*0.75) + 10 , (float) height/2 - 10, paint2);
        return mutableBitmap;
    }

}
