package com.example.arcm_beta_2_0.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.arcm_beta_2_0.model.ImageDAO;

import java.text.SimpleDateFormat;

public class DbUtils {

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss"); // 格式化時間日期

    private static interface TableInfo { // 資料表資訊
        String TABLE = "image_table",
                NAME = "name",
                REAL_PIXEL_UNIT = "real_pixel_unit",
                IMAGE_TYPE = "Image_type",
                RESULT = "result",
                MESSAGE = "message",
                FORM[] = {NAME, REAL_PIXEL_UNIT, IMAGE_TYPE, RESULT,MESSAGE};
    }
    private SQLiteDatabase db;

    public DbUtils(Context context) {
        Database database = new Database(context);
        db = database.getWritableDatabase();
    }

    private ContentValues createContentValues(ImageDAO image) { // 新增資料動作的設定
        ContentValues values = new ContentValues();
        values.put(TableInfo.NAME, image.getName());
        values.put(TableInfo.REAL_PIXEL_UNIT, image.getRealPixelUnit());
        values.put(TableInfo.IMAGE_TYPE, image.getImage_type());
        values.put(TableInfo.RESULT, image.getResult());
        values.put(TableInfo.MESSAGE, image.getMessage());

        return values;
    }

    private ImageDAO getEntity(Cursor cursor) { // 取得資料動作的設定
        ImageDAO image = new ImageDAO();
        image.setName(cursor.getString(0));
        image.setRealPixelUnit(cursor.getFloat(1));
        image.setImage_type(cursor.getString(2));
        image.setResult(cursor.getDouble(3));
        image.setMessage(cursor.getString(4));

        return image;
    }

    public ImageDAO[] getAll() { // 取得所有資料
        Cursor cursor = db.query(TableInfo.TABLE, null, null, null, null, null, TableInfo.NAME + " desc");
        int size = cursor.getCount();
        ImageDAO images[] = null;
        if (size != 0) {
            cursor.moveToFirst();
            images = new ImageDAO[size];
            if (size > 0) {
                for (int i=0; i<size; i++) {
                    images[i] = getEntity(cursor);
                    cursor.moveToNext();
                }
            }
        }

        cursor.close();

        return images;
    }

    public ImageDAO[] get(String name) { // 取得指定資料
        Cursor cursor = db.query(TableInfo.TABLE, TableInfo.FORM, TableInfo.NAME + " = '" + name + "'", null, null, null, TableInfo.NAME);
        cursor.moveToFirst();

        ImageDAO images[]= null;
        if (cursor.getCount() > 0) {
            images = new ImageDAO[cursor.getCount()];
            for (int i=0; i<images.length; i++) {
                images[i] = getEntity(cursor);
                cursor.moveToNext();
            }
        }

        cursor.close();

        return images;
    }

    public ImageDAO[] getGroup(String name) { // 以群取得的資料
        Cursor cursor = db.query(TableInfo.TABLE, TableInfo.FORM, TableInfo.NAME + " like '" + name.substring(0, name.lastIndexOf("-") - 1) + "%'", null, null, null, TableInfo.NAME);
        cursor.moveToFirst();

        ImageDAO images[]= null;
        if (cursor.getCount() > 0) {
            images = new ImageDAO[cursor.getCount()];
            for (int i=0; i<images.length; i++) {
                images[i] = new ImageDAO();
                images[i].setName(cursor.getString(0));
                Float f = cursor.getString(1) == null? 0f: Float.parseFloat(cursor.getString(1));
                images[i].setRealPixelUnit(f);
                images[i].setImage_type(cursor.getString(2));
                Double d = cursor.getString(3) == null? 0d: Double.parseDouble(cursor.getString(3));
                images[i].setResult(d);
                images[i].setMessage(cursor.getString(4));
                cursor.moveToNext();
            }
        }

        cursor.close();

        return images;
    }

    public void setAll(ImageDAO[] images) { // 輸入所有資料
        db.beginTransaction();

        for (int i=0; i<images.length; i++)
            db.insert(TableInfo.TABLE, null, createContentValues(images[i]));

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void set(ImageDAO image) { // 輸入指定資料
        if (get(image.getName()) == null) {
            db.beginTransaction();
            db.insert(TableInfo.TABLE, null, createContentValues(image));
            db.setTransactionSuccessful();
            db.endTransaction();
        } else
            update(image);
    }

    public void update(ImageDAO image) { // 更新指定資料
        db.beginTransaction();
        db.update(TableInfo.TABLE, createContentValues(image), TableInfo.NAME + " = '" + image.getName() + "'", null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void deleteAll() { // 刪除所有資料
        db.beginTransaction();
        db.delete(TableInfo.TABLE, null, null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void delete(ImageDAO image) { // 刪除指定資料
        db.beginTransaction();
        db.delete(TableInfo.TABLE, TableInfo.NAME + " = '" + image.getName() + "'", null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void close() { // 關閉資料庫連結
        db.close();
    }

}

