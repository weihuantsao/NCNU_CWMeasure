package com.example.arcm_beta_2_0.common;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.arcm_beta_2_0.GlobalVariable;
import com.example.arcm_beta_2_0.db.DbUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.arcm_beta_2_0.GlobalVariable.STORE_PATH;
import static com.example.arcm_beta_2_0.GlobalVariable.currentTimeFormat;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;
import static com.example.arcm_beta_2_0.GlobalVariable.imgURI;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static Context sContext = MyApp.getInstance();

    private static final String GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM) + File.separator + "Camera";

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Thumbnails._ID,
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static Bitmap rotateBitmap(Bitmap source, int degree, boolean flipHorizontal, boolean recycle) {
        if (degree == 0 && !flipHorizontal) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (flipHorizontal) {
            matrix.postScale(-1, 1);
        }
        Log.d(TAG, "source width: " + source.getWidth() + ", height: " + source.getHeight());
        Log.d(TAG, "rotateBitmap: degree: " + degree);
        Bitmap rotateBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        Log.d(TAG, "rotate width: " + rotateBitmap.getWidth() + ", height: " + rotateBitmap.getHeight());
        if (recycle) {
            source.recycle();
        }
        return rotateBitmap;
    }

    public static void saveImage(Context context,byte[] jpeg) {
        sContext = new MyApp();
//        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".png";
        String fileName = currentTimeFormat() + "-" + "1-first.png";

        File outFile = new File(STORE_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        imgURI = outFile.getAbsolutePath();

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            os.write(jpeg);
            os.flush();
            os.close();
            //insertToDB(outFile.getAbsolutePath());

            DbUtils dbUtils = new DbUtils(context);

            imgObj.setName(fileName);
            dbUtils.set(imgObj);

            // update ExternalStorage
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(new File(STORE_PATH));
            intent.setData(uri);
            context.sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveBitmap(Bitmap bitmap) {
//        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
//        File outFile = new File(GALLERY_PATH, fileName);
        String fileName = currentTimeFormat() + "-" + "1-first.jpg";
        File outFile = new File(STORE_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.d(TAG, "saveBitmap: " + success);
            if (success) {
                //insertToDB(outFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void insertToDB(String picturePath) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = sContext.getContentResolver();
        values.put(MediaStore.Images.ImageColumns.DATA, picturePath);
        values.put(MediaStore.Images.ImageColumns.TITLE, picturePath.substring(picturePath.lastIndexOf("/") + 1));
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap getLatestThumbBitmap() {
        Bitmap bitmap = null;
        // 按照时间顺序降序查询
        Cursor cursor = MediaStore.Images.Media.query(sContext.getContentResolver(), MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        boolean first = cursor.moveToFirst();
        if (first) {
            long id = cursor.getLong(0);
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(sContext.getContentResolver(), id, MediaStore.Images
                    .Thumbnails.MICRO_KIND, null);
            Log.d(TAG, "bitmap width: " + bitmap.getWidth());
            Log.d(TAG, "bitmap height: " + bitmap.getHeight());
        }
        cursor.close();
        return bitmap;
    }

    public static Bitmap rotationBitMap(String imgName) {
        try {
            ExifInterface exif = new ExifInterface(STORE_PATH + imgName);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);

            Matrix matrix = new Matrix();
            if (rotation != 0) {
                matrix.preRotate(rotationInDegrees);
            }

            BitmapFactory.Options options = new BitmapFactory.Options(); // 防止OOM(Out of Memory 所設定的選項物件
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inSampleSize = 2;


            Bitmap bitmap = null; // 讀取原始圖檔

            bitmap = BitmapFactory.decodeStream(new FileInputStream(STORE_PATH + imgName), null, options);
            Bitmap adjustedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return adjustedBitmap;
        } catch (IOException e) {
            Log.e(TAG, "rotationBitMap: error");
            return null;
        }
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }
}
