package com.example.arcm_beta_2_0.common.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import com.example.arcm_beta_2_0.model.ImageDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.arcm_beta_2_0.GlobalVariable.STORE_PATH;
import static com.example.arcm_beta_2_0.common.ImageUtils.rotationBitMap;

public class StoreUtils {

    public static int storeBitmap(Context context, ImageDAO imgObj) {

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // ExternalStorage Error
            return -1;
        }
        String fullPathFile = STORE_PATH + imgObj.getName();
        File picFile = new File(fullPathFile);
        try {
            if (!picFile.getParentFile().exists()) {
                picFile.getParentFile().mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(picFile);
            imgObj.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // Save Error
            return -2;
        }
        // update ExternalStorage
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(STORE_PATH));
        intent.setData(uri);
        context.sendBroadcast(intent);
        return 0;
    }

    public static Bitmap findBitmap(ImageDAO imgObj) {
        return rotationBitMap(imgObj.getName());
    }
}
