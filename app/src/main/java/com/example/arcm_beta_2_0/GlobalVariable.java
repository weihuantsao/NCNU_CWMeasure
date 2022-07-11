package com.example.arcm_beta_2_0;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Bitmap;
import android.os.Environment;

import com.example.arcm_beta_2_0.model.ImageDAO;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GlobalVariable extends Application {
    // constant
    public static String STORE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/AR/";
    public static String ORIGIN = "Origin";
    public final static String CANNY = "Canny";
    public final static String BINARY = "Binary";
    public static String imgURI;
    // main image object
    public static ImageDAO imgObj;

    @SuppressLint("SimpleDateFormat")
    public static String currentTimeFormat() {
        return new SimpleDateFormat("yyyyMMddHHmmss-").format(new Date()) + System.currentTimeMillis();
    }

    //修改 變數値
    private Bitmap CannyBitMap;     //Canny 處理過的影像
    private Bitmap BinaryBitMap;    //Binary 處理過的影像
    private Bitmap OrginBitMap;     //Orgin 處理過的影像
    private float RealPixelUnit; //真實物理單位
    private int Threshold1,Threshold2;//閾值

    public void setCannyBitMap(Bitmap _CannyBitMap){
        this.CannyBitMap = _CannyBitMap;
    }
    public void setBinaryBitMap(Bitmap _BinaryBitMap){
        this.BinaryBitMap = _BinaryBitMap;
    }
    public void setOriginBitMap(Bitmap _OriginBitMap){
        this.OrginBitMap = _OriginBitMap;
    }
    public void setThreshold1(int _Threshold1){
        this.Threshold1 = _Threshold1;
    }
    public void setThreshold2(int _Threshold2){
        this.Threshold2 = _Threshold2;
    }
    public void setImgURI(String imgURI) {
        this.imgURI = imgURI;
    }

    public float getRealPixelUnit(){ return  RealPixelUnit;}
    public Bitmap getCannyBitMap(){ return CannyBitMap; }
    public Bitmap getBinaryBitMap(){
        return BinaryBitMap;
    }
    public Bitmap getOriginBitMap(){
        return OrginBitMap;
    }
    public int getThreshold1(){
        return Threshold1;
    }
    public int getThreshold2(){
        return Threshold2;
    }
    public String getImgURI() {
        return imgURI;
    }



}