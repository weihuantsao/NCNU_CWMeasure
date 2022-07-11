package com.example.arcm_beta_2_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.arcm_beta_2_0.common.helpers.StoreUtils;
import com.example.arcm_beta_2_0.db.DbUtils;
import com.example.arcm_beta_2_0.model.ImageDAO;

import static com.example.arcm_beta_2_0.GlobalVariable.STORE_PATH;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;
import static com.example.arcm_beta_2_0.common.ImageUtils.rotationBitMap;


public class HistoryActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        init(); // 初始化
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 當 HistoryDetailActivity 結束時回傳結果
        // TODO Auto-generated method stub
        switch (resultCode) {
            case 1: // 當回傳結果為 1 時
                finish(); // 結束 HistoryActivity
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() { // 初始化
        final ProgressDialog pd = ProgressDialog.show(this, null, "載入中...", true, false); // 建立並顯示載入中訊息

        Handler handler = new Handler(); // 產生並使用 Handler 物件與正常程序公平搶系統資源
        handler.post(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                final ListView lv = (ListView) findViewById(R.id.images); // 取得 ListView 物件
                final List<ImageDAO> imageList = getImageList(); // 取得圖片列表
                if(imageList.size() == 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this); // 產生互動式訊息
                    builder.setTitle("目前無量測資料！"); // 設定顯示訊息'
                    builder.setMessage("是否逕行裂縫量測");
                    builder.setNegativeButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(HistoryActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                    builder.setPositiveButton("取消", new DialogInterface.OnClickListener() { // 設定刪除按鈕被點擊時
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.show();
                }else {
                    lv.setAdapter(new ImageAdapter(HistoryActivity.this, imageList)); // 產生圖片 Adapter 並設定圖片列表 之後設定到 ListView 顯示結果
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() { // 設定 ListView 的項目被點擊時

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // TODO Auto-generated method stub
                            DbUtils ih = new DbUtils(HistoryActivity.this); // 產生與資料庫溝通的圖片物件
                            List<ImageDAO> imageList = getImageList(); // 取得圖片列表
                            ImageDAO[] images = ih.getGroup(imageList.get(position).getName()); // 以群的方式取得圖片資料
                            ih.close(); // 關閉資料庫連結

//                            Bundle bundle = new Bundle(); // 產生傳遞訊息給 HistoryDetailActivity 的 Bundle
//                            bundle.putString("first", images[0].getName()); // 放入原始圖片檔名
//                            bundle.putInt("position",position);
                            imgObj = images[0];


                            imgObj.setBitmap(StoreUtils.findBitmap(imgObj));
                            Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class); // 產生前往 HistoryDetailActivity 的意圖
//                            intent.putExtras(bundle); // 設定 Bundle 給意圖
                            startActivityForResult(intent, 0); // 前往 HistoryDetailActivity
                            HistoryActivity.this.finish();
                        }

                    });
                }

                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // 設定 ListView 的項目被長壓時

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                        // TODO Auto-generated method stub
                        AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this); // 產生互動式訊息

                        String name = imageList.get(position).getName(); // 取得圖片名稱
                        builder.setMessage(DbUtils.SDF.format(new Date(Long.parseLong(name.substring(name.indexOf("-") + 1, name.indexOf("-", name.indexOf("-") + 1))))) + " 的歷史資料是否刪除？"); // 設定顯示訊息

                        builder.setPositiveButton("刪除", new DialogInterface.OnClickListener() { // 設定刪除按鈕被點擊時

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                DbUtils ih = new DbUtils(HistoryActivity.this); // 產生與資料庫溝通的圖片物件
                                List<ImageDAO> imageList = getImageList(); // 取得圖片列表
                                ImageDAO image[] = ih.getGroup(imageList.get(position).getName()); // 以群的方式取得圖片資料
                                for (int i=0; i<image.length; i++) {
                                    new File(MainActivity.Save_path + "/" + image[i].getName()).delete(); // 刪除圖片
                                    ih.delete(image[i]); // 刪除在資料庫的圖片資料
                                }
                                ih.close(); // 關閉資料庫連結
                                lv.setAdapter(new ImageAdapter(HistoryActivity.this, getImageList())); // 重設 ListView 狀態
                            }

                        });
                        builder.setNegativeButton("取消", null); // 設定取消按鈕被點擊時
                        builder.create().show(); // 建立並顯示互動式訊息
                        return true;
                    }
                });

                pd.dismiss(); // 載入中訊息關閉
            }
        });
    }

    private List<ImageDAO> getImageList() { // 取得圖片資料列表
        List<ImageDAO> imageList = new ArrayList<ImageDAO>(); // 建立新的圖片列表物件

        DbUtils ih = new DbUtils(this); // 產生與資料庫溝通的圖片物件
        ImageDAO images[] = ih.getAll(); // 取得所有圖片資料
        ih.close(); // 關閉資料庫連結

        if (images != null) // 如果有圖片資料
            for (int i=0; i<images.length; i++)
                if (images[i].getName().indexOf("first") > -1) // 如果是原始圖片檔名
                    imageList.add(images[i]); // 設定到圖片列表上

        return imageList;
    }

    private class ImageAdapter extends BaseAdapter { // 實作圖片 Adapter

        private LayoutInflater li;
        private List<ImageDAO> imageList;

        ImageAdapter(Context context, List<ImageDAO> imageList) {
            this.li = LayoutInflater.from(context);
            this.imageList = imageList;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return imageList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return imageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }


        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ViewHolder viewHolder;
            if (convertView == null){
                convertView = li.inflate(R.layout.listview_image, null); // 建立新的列表物件
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }


            ImageDAO image = imageList.get(position); // 取得圖片物件

            Bitmap bitmap = rotationBitMap(image.getName());
            viewHolder.imageView.setImageBitmap(bitmap); // 設定到顯示圖片的物件上
            String name = image.getName(); // 取得檔名
            viewHolder.time.setText(DbUtils.SDF.format(new Date(Long.parseLong(name.substring(name.indexOf("-") + 1, name.indexOf("-", name.indexOf("-") + 1)))))); // 將圖片檔名處理後設定在顯示檔名的物件上
            return convertView;
        }

        class ViewHolder {
            TextView time;
            ImageView imageView;

            public ViewHolder(View convertView) {
                // 建立ViewHolder時，ViewHolder負責findViewById
                time = (TextView) convertView.findViewById(R.id.time);
                imageView = (ImageView) convertView.findViewById(R.id.imageview);
            }

        }




    }

}