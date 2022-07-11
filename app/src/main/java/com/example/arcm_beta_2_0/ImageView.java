package com.example.arcm_beta_2_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.arcm_beta_2_0.common.helpers.StoreUtils;
import com.example.arcm_beta_2_0.db.DbUtils;
import com.example.arcm_beta_2_0.model.ImageDAO;
import com.example.arcm_beta_2_0.model.Point;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import static com.example.arcm_beta_2_0.GlobalVariable.BINARY;
import static com.example.arcm_beta_2_0.GlobalVariable.CANNY;
import static com.example.arcm_beta_2_0.GlobalVariable.ORIGIN;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;

public class ImageView extends SurfaceView implements SurfaceHolder.Callback {

    private final int DRAG = 1;
    private final int ZOOM = 2;
    private final int NONE = 3;
    private final int FLAG_COUNT = 5;

    private static Point point[] = new Point[3];

    private static Point FIndpoint[] = new Point[3];

    private static boolean holdScalePoint;
    private static float x;
    private static String message;

    private SurfaceHolder holder;
    private Bitmap drawBitmap;
    private Matrix matrix;
    private Matrix savedMatrix;
    private Matrix tmpMatrix;
    private Paint pointPaint;
    private Paint pointPaintFind;

    private float oldDistance;

    private Point startPoint = new Point();
    private Point midPoint = new Point();
    private Point indexPoint = new Point();
    private Point currentPoint = new Point();
    private RectF src, dst;
    private int flag;
    private int action = NONE;
    private float scale;
    private long startTime;
    private Context context;
    private Canvas canvas;
    private GlobalVariable gv;
    private SeekBar mSeekBar;

    private float floatpostTranslate_x;
    private float floatpostTranslate_y;

    public String type;


    private ProgressDialog progress;
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Test","OpenCV執行成功");
        }else {
            Log.d("Test", "OpenCV執行失敗！");
        }
    }

    public ImageView(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);
        gv = (GlobalVariable)context.getApplicationContext();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { // 當建立畫布完成後
        tmpMatrix = new Matrix();

        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setStrokeWidth(5.0f);
        pointPaint.setAntiAlias(true);

        pointPaintFind = new Paint();
        pointPaintFind.setColor(Color.GREEN);
        pointPaintFind.setStyle(Paint.Style.STROKE);
        pointPaintFind.setStrokeWidth(5.0f);
        pointPaintFind.setAntiAlias(true);


        Point bitmapPoint = new Point();
        bitmapPoint.x = 0.0f;
        bitmapPoint.y = 0.0f;

        canvas = holder.lockCanvas();

        indexPoint.x = canvas.getWidth() / 2.0f;
        indexPoint.y = canvas.getHeight() / 2.0f;

        matrix.setTranslate(bitmapPoint.x, bitmapPoint.y);
        src = new RectF(0.0f, 0.0f, drawBitmap.getWidth(), drawBitmap.getHeight());
        dst = new RectF(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
        savedMatrix.set(matrix);

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(drawBitmap, matrix, null);

        drawPoint(canvas);

        holder.unlockCanvasAndPost(canvas);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { // 當畫布大小被改變後

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { // 當繪布被釋放後
        point = toBitmapPoint(point);

        matrix.set(savedMatrix);
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);

        point = toDispalyPoint(point);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) { // 當繪布被觸碰時
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                startPoint.x = event.getX();
                startPoint.y = event.getY();
                startTime = System.currentTimeMillis();
                flag = FLAG_COUNT;
                action = DRAG;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (System.currentTimeMillis() - startTime < 150L)
                    onClick(event);
                action = NONE;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                oldDistance = getDistance(event);
                if (oldDistance > 10f) {
                    action = ZOOM;
                    savedMatrix.set(matrix);
                    midPoint = pointMid(event);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: { //兩根手指頭變一根
                flag = 0;
                action = DRAG;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (action == DRAG)
                    onDrag(event);
                flag++;
                if (action == ZOOM)
                    onZoom(event);
            }

            default:
                onNone(event);
        }
        return true;
    }

    public void updateView() {
        canvas = holder.lockCanvas();
        canvas.drawBitmap(drawBitmap, savedMatrix, null);
        drawPoint(canvas);
        holder.unlockCanvasAndPost(canvas);
    }

    private void onDrag(MotionEvent event) { // 當為拖拉事件時
        matrix.set(savedMatrix);

        currentPoint.x = event.getX();
        currentPoint.y = event.getY();

        if (flag > FLAG_COUNT) {
            point = toBitmapPoint(point);

            matrix.postTranslate((float) (currentPoint.x - startPoint.x),
                    (float) (currentPoint.y - startPoint.y));


            floatpostTranslate_x = (float) (currentPoint.x - startPoint.x);
            floatpostTranslate_y = (float) (currentPoint.y - startPoint.y);

            indexPoint.x = indexPoint.x + currentPoint.x - startPoint.x;
            indexPoint.y = indexPoint.y + currentPoint.y - startPoint.y;

            point = toDispalyPoint(point);
        }

        Canvas canvas = holder.lockCanvas();

        canvas.drawColor(Color.BLACK);

//        if (imgObj.getImage_type().equals(ORIGIN)) {
//            canvas.drawBitmap(imgObj.getBitmap(), matrix, null);
//        } else {
//            canvas.drawBitmap(imgObj.getAfterProcessBitmap(), matrix, null);
//        }

        canvas.drawBitmap(drawBitmap, matrix, null);

        drawPoint(canvas);

        holder.unlockCanvasAndPost(canvas);

        startPoint.x = currentPoint.x;
        startPoint.y = currentPoint.y;
        savedMatrix.set(matrix);
    }

    private void onZoom(MotionEvent event) { // 當為放大事件時
        float newDistance = getDistance(event);
        if (newDistance > 10.0f) {
            point = toBitmapPoint(point);

            matrix.set(savedMatrix);
            scale = (oldDistance + ((newDistance - oldDistance) / 2.0f)) / oldDistance;
            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
            float distance = getDistance(midPoint, indexPoint);
            float sina = (indexPoint.y - midPoint.y) / distance;
            float cosa = (indexPoint.x - midPoint.x) / distance;
            distance *= scale;
            indexPoint.x = distance * cosa + midPoint.x;
            indexPoint.y = distance * sina + midPoint.y;
            oldDistance = newDistance;

            point = toDispalyPoint(point);

            Canvas canvas = holder.lockCanvas();

            canvas.drawColor(Color.BLACK);

//            if (imgObj.getImage_type().equals(ORIGIN)) {
//                canvas.drawBitmap(imgObj.getBitmap(), matrix, null);
//            } else {
//                canvas.drawBitmap(imgObj.getAfterProcessBitmap(), matrix, null);
//            }

            canvas.drawBitmap(drawBitmap, matrix, null);

            drawPoint(canvas);

            holder.unlockCanvasAndPost(canvas);

            savedMatrix.set(matrix);

        }
    }

    @SuppressLint("NewApi")
    private void onClick(MotionEvent event) { // 當為點擊事件時

        for(int i=0;i<point.length;i++){
            point[i] = null;
            FIndpoint[i] = null;
        }

        if(imgObj.getImage_type().equals(CANNY)){
            setCannyClickPoint(event.getX(), event.getY());
        } else {
            setBinaryClickPoint(event.getX(), event.getY());
        }

        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(drawBitmap, matrix, null);
        drawPoint(canvas);
        holder.unlockCanvasAndPost(canvas);

        if (point[0] != null && point[1] !=null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("是否計算距離？");
            builder.setPositiveButton("是", (dialog, which) -> {
                processImageEvent();
            });
            builder.setNegativeButton("否", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
//                    point[0] = null;
//                    point[1] = null;

                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }

    /*
     MIN
     日期 : 2019/07/07
     函數名稱 : processImageEvent
     解說 : 顯示處理影像的訊息窗，並執行ImageProcessrunnable。
    */
    private void processImageEvent(){
        progress = new ProgressDialog(context);
        progress.setMessage("影像處理中．．．");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setCanceledOnTouchOutside(false);
        progress.show();

        Thread t = new Thread(ImageProcessrunnable);
        t.start();
    }

    /*
      MIN
      日期 : 2019/07/07
      函數名稱 : ImageProcessrunnable
      解說 : 處理圖片轉換成Canny，並且根據使用者點擊位置算出縫隙距離，當執行完成後，會交給resultDisPlayHandler顯示UI結果。
    */
    private Runnable ImageProcessrunnable = new Runnable() {
        @Override
        public void run() {
            try {

                point = toBitmapPoint(point);
                // format部分都為取到第n位，n+1位做四捨五入
                DecimalFormat df1 = new DecimalFormat("0.0000"); // 小數點以下取4位
                DecimalFormat df2 = new DecimalFormat("0.000000"); // 單位mm，小數點以下取6位
                DecimalFormat df3 = new DecimalFormat("0.00000"); // 單位mm，小數點以下取5位
                df1.setRoundingMode(RoundingMode.CEILING);
                df2.setRoundingMode(RoundingMode.CEILING);
                df3.setRoundingMode(RoundingMode.CEILING);
                double distance1 = getDistance(point[0], point[1]);

                point = toDispalyPoint(point);
                double ar = 0.0;
//                double x = ((Activity)context).getIntent().getExtras().getDouble("Distance");
                double x1 = Double.parseDouble(df1.format(((Activity)context).getIntent().getExtras().getDouble("Distance_result")*10)),
                        y = ((int)(distance1*1000d))/1000d;
                if(x1 != 0.0)
                    ar = x1;

                message = "裂縫寬度為 " + df1.format(distance1) + " Pixel單位" + "\n"
                        + "單位Pixel長度 : " + df2.format(ar) + " mm" + " \n"
                        + "裂縫寬度 : " + df3.format(ar*distance1) +" mm ";


                DbUtils ih = new DbUtils(context);

                String Imageprocessing = imgObj.getImage_type();
                String first = imgObj.getName();
                ImageDAO image = ih.get(first)[0];
                image.setResult(ar);

                String imageMessage = image.getMessage();
                if (Imageprocessing.equals(CANNY)) {
                    message = "Canny坎尼邊緣偵測 : " + "\n\n" + message + "\n\n";
                    if (imageMessage.contains(CANNY)) {
                        if (imageMessage.contains(BINARY))
                            imageMessage = message + imageMessage.substring(imageMessage.indexOf("Binary"), imageMessage.length());
                        else
                            imageMessage = message;
                    } else
                        imageMessage = message + imageMessage;
                } else {
                    message = "Binary二值化 : " + "\n\n" + message + " \n\n";

                    if (imageMessage.contains(BINARY)) {
                        if (imageMessage.contains(CANNY))
                            imageMessage = imageMessage.substring(0,imageMessage.indexOf("Binary")) + message;
                        else
                            imageMessage = message;
                    } else
                        imageMessage = imageMessage + message;
                }

                image.setMessage(imageMessage);
                ih.update(image);
                ih.close();

                progress.dismiss();

                Message msg = new Message();
                msg.what = 1;
                resultDisPlayHandler.sendMessage(msg);
            } catch (Exception e) {
                Log.d("Test",e.getMessage()) ;
                e.printStackTrace();
            }
        }
    };

    /*
     MIN
     日期 : 2019/07/07
     函數名稱 : resultDisPlayHandler
     解說 : 將結果顯示在圖片上，並關掉處理狀態的dialog。
    */
    @SuppressLint("HandlerLeak")
    private Handler resultDisPlayHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    Log.d("Test","結束執行") ;
                    Canvas canvas = holder.lockCanvas();
                    canvas.drawColor(Color.BLACK);
                    String Imageprocessing = imgObj.getImage_type();
                    if (Imageprocessing.equals(CANNY) || Imageprocessing.equals(BINARY))
                        canvas.drawBitmap(imgObj.getAfterProcessBitmap(), matrix, null);
                    else
                        canvas.drawBitmap(imgObj.getBitmap(), matrix, null);

                    drawPoint(canvas);

                    holder.unlockCanvasAndPost(canvas);
                    progress.dismiss();

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("量測結果");
                    builder.setMessage(message);
                    builder.setPositiveButton("確定", null);
                    builder.create().show();

                    break;
            }
        }
    };

    /*
          MIN
          日期 : 2019/07/07
          函數名稱 : getImagePoints
          輸入參數 :
          1. image_X : View 的 X 座標
          2. image_Y : View 的 Y 座標
          回傳參數 : 回傳轉換成Image的座標
          */
    private Point getImagePoints(float view_X,float view_Y){
        Point m_ImagePoints = ViewToImage_Point(view_X, view_Y);
        Log.d("Test","轉換輸入座標(View To Image) : X:"+m_ImagePoints.x+" Y:"+m_ImagePoints.y) ;
        return  m_ImagePoints;
    }

    /*
    MIN
    日期 : 2019/07/07
    函數名稱 : getViewPoints
    輸入參數 :
    1. image_X : Image 的 X 座標
    2. image_Y : Image 的 Y 座標
    回傳參數 : 回傳轉換成View的座標
    */
    private Point getViewPoints(float image_X,float image_Y){
        Point m_ViewPoints = ImageToView_Point(image_X, image_Y);
        Log.d("Test","轉換輸入座標(Image To View) : X:"+m_ViewPoints.x+" Y:"+m_ViewPoints.y) ;
        return  m_ViewPoints;
    }

    //螢幕視窗座標轉換成真實照片
    private Point ViewToImage_Point(float view_X, float view_Y){
        float[] ViewToImage_Points = {view_X, view_Y};
        //轉換成真實照片
        tmpMatrix.reset();
        matrix.invert(tmpMatrix);
        tmpMatrix.mapPoints(ViewToImage_Points);
        Point point = new Point();
        point.x = ViewToImage_Points[0];
        point.y = ViewToImage_Points[1];
        return point;
    }

    //真實照片轉換成螢幕視窗座標
    private Point ImageToView_Point(float image_X, float image_Y){
        float[] ImageToView_Points = {image_X, image_Y};
        //轉換成ImageView
        tmpMatrix.reset();
        matrix.invert(tmpMatrix);
        tmpMatrix.invert(tmpMatrix);
        tmpMatrix.mapPoints(ImageToView_Points);
        Point point = new Point();
        point.x = ImageToView_Points[0];
        point.y = ImageToView_Points[1];
        return point;
    }

    private void onNone(MotionEvent event) { // 當為沒有任何事件時

    }

    private void drawPoint(Canvas canvas) { // 畫出點擊所產生的點
        for(int i = 0;i < point.length;i++)
            if (point[i] != null) {//point[i] != null
                canvas.drawCircle(point[i].x, point[i].y, 20.0f, pointPaint);

                if ((i+1) % 2 == 0)
                    canvas.drawLine(point[i-1].x, point[i-1].y, point[i].x, point[i].y, pointPaint);
            }
    }

    //20210315庭宇修改setBinaryClickPoint
    private void setBinaryClickPoint(float x, float y) { // 設定點擊點及裂縫兩邊之找點
        point[2] = set(x, y);
        if (point[0] == null) {
            Point[] binarypoints = searchBinaryPoints(x, y,imgObj.getAfterProcessBitmap());
            point[0] = binarypoints[0];
            point[1] = binarypoints[1];
        }
    }

    //20210315庭宇修改setCannyClickPoint
    private void setCannyClickPoint(float x, float y) { // 設定點擊點及裂縫兩邊之找點
        point[2] = set(x, y);
        if (point[0] == null) {
            Point[] cannypoints = searchCannyPoints(x, y, imgObj.getAfterProcessBitmap());
            point[0] = cannypoints[0];
            point[1] = cannypoints[1];
        }
    }

    private Point set(float x,float y){
        float[] pts = {x, y};
        Point point = new Point();
        point.x = pts[0];
        point.y = pts[1];
        return point;
    }

    private Point[] toBitmapPoint(Point[] point) { // 將觸碰點位置轉換成影像的實際位置
        for (int i=0; i<point.length; i++)
            if (point[i] != null)
                point[i] = getBitmapPoint(point[i]);
            else
                break;

        return point;
    }

    private Point getBitmapPoint(Point point) { // 取得觸碰點在影像的實際位置
        float[] pts = {point.x, point.y};
        tmpMatrix.reset();
        matrix.invert(tmpMatrix);
        tmpMatrix.mapPoints(pts);
        point.x = pts[0];
        point.y = pts[1];
        return point;
    }

    private Point[] toDispalyPoint(Point[] point) {  // 將觸碰點位置轉換成螢幕的實際位置
        for (int i=0; i<point.length; i++)
            if (point[i] != null)
                point[i] = getDisplayPoint(point[i]);
            else
                break;

        return point;
    }

    private Point getDisplayPoint(Point point) { // 取得觸碰點在螢幕的實際位置
        float[] pts = {(point.x ), (point.y )};
        matrix.mapPoints(pts);
        point.x = pts[0];
        point.y = pts[1];
        return point;
    }

    //20210315庭宇修改searchBinaryPoints
    private Point[] searchBinaryPoints(float Orgin_x, float Orgin_y, Bitmap binaryBitmap) { // 搜尋最近的跨越裂縫的兩點
        Point clickPoint = getImagePoints(Orgin_x, Orgin_y); // 點擊處的影像像素座標
        final float height = binaryBitmap.getHeight();
        final float width = binaryBitmap.getWidth();

        Point point1 = new Point();
        Point point2 = new Point();

        if (clickPoint.x >= width || clickPoint.y >= height || clickPoint.x < 0 || clickPoint.y < 0) {
            //如果點擊點超出手機範圍，則設定找到值皆為0
            point1.x = 0;
            point1.y = 0;
            point2.x = 0;
            point2.y = 0;
        } else {
            //以點擊點找最近點，再由斜率找第二點
            if (binaryBitmap.getPixel((int) clickPoint.x, (int) clickPoint.y) == Color.BLACK) {
                // 點擊Black Pixel，搜尋最近White Pixel，再搜尋另一邊White Pixel
                point1 = findTheNearestPoint(binaryBitmap, Color.WHITE, clickPoint.x, clickPoint.y);
                point2 = findPointBySlope(binaryBitmap, Color.WHITE, point1.x, point1.y, clickPoint.x, clickPoint.y, false);
            } else {
                // 點擊White Pixel，搜尋最近Black Pixel，再搜尋另一邊White Pixel
                point1 = findTheNearestPoint(binaryBitmap, Color.BLACK, clickPoint.x, clickPoint.y);
                point2 = findPointBySlope(binaryBitmap, Color.WHITE, clickPoint.x, clickPoint.y, point1.x, point1.y, false);
            }

            //binary調整pixel位置(點擊White or Black Pixel時都要做調整)
            float chk1x = point1.x + 1;
            float chk1y = point1.y + 1;
            float chk2x = point2.x + 1;
            float chk2y = point2.y + 1;
            if ((int) clickPoint.x - point1.x > 0 && chk1x < width && chk1x >= 0)
                point1.x = chk1x;
            if ((int) clickPoint.y - point1.y > 0 && chk1y < height && chk1y >= 0)
                point1.y = chk1y;
            if ((int) clickPoint.x - point2.x < 0 && chk2x < width && chk2x >= 0)
                point2.x = chk2x;
            if ((int) clickPoint.y - point2.y < 0 && chk2y < height && chk2y >= 0)
                point2.y = chk2y;
        }

        //轉換座標及回傳最近點及第二點
        Point[] foundPoints = new Point[2];
        foundPoints[0] = getViewPoints(point1.x, point1.y);
        foundPoints[1] = getViewPoints(point2.x, point2.y);
        return foundPoints;
    }

    //20210315庭宇修改searchCannyPoints
    private Point[] searchCannyPoints(float Orgin_x, float Orgin_y, Bitmap cannyBitmap) { // 搜尋最近的跨越裂縫的內側的兩點
        Point clickPoint = getImagePoints(Orgin_x, Orgin_y); // 點擊處的影像像素座標
        final float height = cannyBitmap.getHeight();
        final float width = cannyBitmap.getWidth();

        Point point1;
        Point point2 = new Point();
        Point point3 = new Point();

        //如點擊點超出手機範圍，則設定找到值皆為0
        if (clickPoint.x >= width || clickPoint.y >= height || clickPoint.x < 0 || clickPoint.y < 0) {
            point2.x = 0;
            point2.y = 0;
            point3.x = 0;
            point3.y = 0;
        } else {
            if (cannyBitmap.getPixel((int) clickPoint.x, (int) clickPoint.y) == Color.BLACK) {
                // 點擊Black Pixel，即不做事
                point2 = clickPoint;
                point3 = clickPoint;
            } else {
                // 點擊White Pixel，搜尋最近Black Pixel，再射到White Pixel，再射到Black Pixel
                point1 = findTheNearestPoint(cannyBitmap, Color.BLACK, clickPoint.x, clickPoint.y);
                point2 = findPointBySlope(cannyBitmap, Color.WHITE, clickPoint.x, clickPoint.y, point1.x, point1.y, true);
                point3 = findPointBySlope(cannyBitmap, Color.BLACK, clickPoint.x, clickPoint.y, point2.x, point2.y, true);

                //canny調整pixel位置(僅對點擊White Pixel時做調整)
                float chk2x = point2.x + 1;
                float chk2y = point2.y + 1;
                float chk3x = point3.x + 1;
                float chk3y = point3.y + 1;
                if ((int) clickPoint.x - point1.x > 0 && chk2x < width && chk2x >= 0)
                    point2.x = chk2x;
                if ((int) clickPoint.y - point1.y > 0 && chk2y < height && chk2y >= 0)
                    point2.y = chk2y;
                if ((int) clickPoint.x - point2.x > 0 && chk3x < width && chk3x >= 0)
                    point3.x = chk3x;
                if ((int) clickPoint.y - point2.y > 0 && chk3y < height && chk3y >= 0)
                    point3.y = chk3y;
            }
        }

        //轉換座標及回傳兩個點(point2&point3)
        Point[] foundPoints = new Point[2];
        foundPoints[0] = getViewPoints(point2.x, point2.y);
        foundPoints[1] = getViewPoints(point3.x, point3.y);
        return foundPoints;
    }

    //20210315庭宇修改findTheNearestPoint
    private Point findTheNearestPoint(Bitmap bmp, int color, float xorig, float yorig) { // 螺旋找點法
        final int width = bmp.getWidth();
        final int height = bmp.getHeight();

        int xstart = (int) xorig; // xoring之前已被轉換成點擊處的影像像素座標, 這裡再將其int
        int ystart = (int) yorig; // yoring之前已被轉換成點擊處的影像像素座標, 這裡再將其int
        int xgo = xstart; //以點擊點為起點開始找點
        int ygo = ystart;
        int xfind = 0, yfind = 0;//找到點

        int end = 2147483647;
        int xincr = 0, yincr = 0, xtemp, ytemp;
        boolean outOfBounds1 = false, outOfBounds2 = false, outOfBounds3 = false, outOfBounds4 = false;
        boolean notFound = true, withinBounds;
        double newDistance, oldDistance = 0;

        for (int i = 2; i < end; i++) {
            int direction = i % 4; //direction從2開始輪迴，2為向右,3為向下,0為向左,1為向上
            switch (direction) {
                case 2:
                    xincr = 1;
                    yincr = 0;
                    break;
                case 3:
                    xincr = 0;
                    yincr = 1;
                    break;
                case 0:
                    xincr = -1;
                    yincr = 0;
                    break;
                case 1:
                    xincr = 0;
                    yincr = -1;
                    break;
            }

            int step = i / 2; //此為計算要走幾步之指標
            for (int a = 0; a < step; a++) {
                xtemp = xgo + xincr;
                ytemp = ygo + yincr;

                if (xtemp >= width) {
                    withinBounds = false;
                    outOfBounds1 = true;
                } else if (ytemp >= height) {
                    withinBounds = false;
                    outOfBounds2 = true;
                } else if (xtemp < 0) {
                    withinBounds = false;
                    outOfBounds3 = true;
                } else if (ytemp < 0) {
                    withinBounds = false;
                    outOfBounds4 = true;
                } else {
                    withinBounds = true;
                }

                if (outOfBounds1 && outOfBounds2 && outOfBounds3 && outOfBounds4) {
                    //手機四個邊都碰到的話就跳出迴圈
                    end = 0;//跳出i迴圈
                }

                if (withinBounds) {
                    xgo = xtemp;
                    ygo = ytemp;
                    if (bmp.getPixel(xgo, ygo) == color) {
                        if (notFound) {
                            //第1次找到指定顏色點
                            notFound = false;
                            end = i * 10;
                            xfind = xgo;
                            yfind = ygo;
                            oldDistance = (Math.pow((xfind - xstart), 2) + Math.pow((yfind - ystart), 2));
                        } else {
                            //第2次以後找到指定顏色點
                            newDistance = (Math.pow((xgo - xstart), 2) + Math.pow((ygo - ystart), 2));
                            if (newDistance <= oldDistance) {
                                xfind = xgo;
                                yfind = ygo;
                                oldDistance = newDistance;
                            }
                        }
                    }
                } else step = 0;//如果超出範圍，就跳出a迴圈
            }
        }

        Point returnpoint = new Point();
        returnpoint.x = xfind;
        returnpoint.y = yfind;
        return returnpoint;
    }

    //20210315庭宇修改findPointBySlope
    private Point findPointBySlope(Bitmap bmp, int color, float x1, float y1, float x2, float y2, boolean oneStepForward) { // 向量找點法
        final int width = bmp.getWidth();
        final int height = bmp.getHeight();

        //點位設定int
        int xstart = (int) x1; //第一點(起點)
        int ystart = (int) y1; //第一點(起點)
        int xnext = (int) x2; //第二點(射的方向)
        int ynext = (int) y2; //第二點(射的方向)

        //向量計算
        float dx = xnext - xstart;
        float dy = ynext - ystart;
        float length = (float) Math.sqrt((dx * dx + dy * dy));//向量的長度
        float dxunit = (dx / length) * 0.05f;//x的單位向量*0.05
        float dyunit = (dy / length) * 0.05f;//y的單位向量*0.05

        //迴圈設定
        float xgo = xnext;
        float ygo = ynext;
        int xfind = (int) xgo;
        int yfind = (int) ygo;
        boolean checker = true, withinBounds = true;

        while (checker) {
            xgo = xgo + dxunit;
            ygo = ygo + dyunit;
            int xtemp = (int) xgo;
            int ytemp = (int) ygo;

            if (xgo >= width || ygo >= height || xgo < 0 || ygo < 0) {
                //碰到手機邊緣
                checker = false;
                withinBounds = false;
            }

            if (withinBounds) {
                if (bmp.getPixel(xtemp, ytemp) != color) {
                    //顏色不對，先暫時存為找到點，之後繼續找
                    xfind = xtemp;
                    yfind = ytemp;
                } else {
                    //顏色對了，判斷要回傳指定顏色(oneStepForward=true)或回傳前一格(oneStepForward=false)
                    if (oneStepForward) {
                        //如果要回傳指定顏色，找到點不可以是起點，否則繼續找！
                        if (xnext != xtemp || ynext != ytemp) {
                            checker = false;
                            xfind = xtemp;
                            yfind = ytemp;
                        }
                    } else checker = false;
                }
            }
        }

        Point returnpoint = new Point();
        returnpoint.x = xfind;
        returnpoint.y = yfind;
        return returnpoint;
    }

    private float getDistance(MotionEvent event) { // 取得距離
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float getDistance(Point p1, Point p2) { // 取得距離
        float x = (p1.x - p2.x) * (p1.x - p2.x);
        float y = (p1.y - p2.y) * (p1.y - p2.y);
        return (float) Math.sqrt(x + y);
    }

    private Point pointMid(MotionEvent event) { // 取得兩點中心
        Point pointMid = new Point();
        pointMid.x = (event.getX(0) + event.getX(1)) / 2.0f;
        pointMid.y = (event.getY(0) + event.getY(1)) / 2.0f;
        return pointMid;
    }

    public void setBitmap(Bitmap bitmap) { // 設定圖片
        drawBitmap = bitmap;
        if (matrix == null || savedMatrix == null){
            matrix = new Matrix();
            savedMatrix = new Matrix();
            matrix.setTranslate(0f, 0f);
            savedMatrix.setTranslate(0f, 0f);
        }

        holder = getHolder();
        holder.addCallback(this);
    }

    public static void reset() {
        for (int i=0; i<point.length; i++)
            point[i] = null;
        message = null;
        x = 0.0f;
        holdScalePoint = false;
    }

    public static Bitmap Binary(Bitmap bitmap , int thresh) { // Binary方法
        Mat srcBitmapMat = new Mat();
        Utils.bitmapToMat(bitmap, srcBitmapMat); // 將 Bitmap 轉換成 OpenCV 可處理的 Mat 物件
        Mat bitmapMat = new Mat();
        Imgproc.cvtColor(srcBitmapMat, bitmapMat, Imgproc.COLOR_BGR2GRAY, 1); // RGB轉換成灰階形式
//        Imgproc.adaptiveThreshold(bitmapMat,bitmapMat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 2);//OpenCV中另一種Binary處理方法
        Imgproc.threshold(bitmapMat, bitmapMat, thresh, 255, Imgproc.THRESH_BINARY);
        Utils.matToBitmap(bitmapMat, bitmap); // 將 Mat 轉換成 Android 可處理的 Bitmap 物件;
        return bitmap;
    }

    public static Bitmap Canny(Bitmap bitmap, int thresh) { // Canny 方法
        Mat srcBitmapMat = new Mat();
        Utils.bitmapToMat(bitmap, srcBitmapMat); // 將 Bitmap 轉換成 OpenCV 可處理的 Mat 物件
        Mat bitmapMat = new Mat();
        Imgproc.cvtColor(srcBitmapMat, bitmapMat, Imgproc.COLOR_BGR2GRAY, 1); // RGB轉換成灰階形式
        Imgproc.blur(bitmapMat, bitmapMat, new Size(1.0, 1.0)); // 模糊化
        Imgproc.Canny(bitmapMat, bitmapMat, thresh, thresh, 3, true); // Canny
        Core.bitwise_not(bitmapMat, bitmapMat);
        Imgproc.cvtColor(bitmapMat, bitmapMat, Imgproc.COLOR_GRAY2BGRA, 4); // 灰階轉換成RGB形式
        Utils.matToBitmap(bitmapMat, bitmap); // 將 Mat 轉換成 Android 可處理的 Bitmap 物件

        return bitmap;
    }

    public Bitmap getDrawBitmap() {
        return drawBitmap;
    }

}