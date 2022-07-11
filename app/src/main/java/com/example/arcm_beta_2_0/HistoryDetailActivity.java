package com.example.arcm_beta_2_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.arcm_beta_2_0.db.DbUtils;
import com.example.arcm_beta_2_0.model.ImageDAO;

import org.json.JSONObject;

import static com.example.arcm_beta_2_0.GlobalVariable.STORE_PATH;
import static com.example.arcm_beta_2_0.GlobalVariable.imgObj;


public class HistoryDetailActivity extends Activity {
    private int position;
    private String jsonText;
    private ProgressDialog progress;
    GlobalVariable gv;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);
        gv = (GlobalVariable)getApplicationContext();
        init(); // 初始化
    }

    @SuppressLint("SetTextI18n")
    private void init() { // 初始化
//        DbUtils ih = new DbUtils(this); // 產生與資料庫溝通的圖片物件
//        ImageDAO imageview = ih.get(getIntent().getExtras().getString("first"))[0]; // 取得原始圖片資料
//        ih.close(); // 關閉資料庫連結



//            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(MainActivity.Save_path + "/" + imageview.getName()), null, options); // 讀取原始圖檔

            ((ImageView) findViewById(R.id.view)).setImageBitmap(imgObj.getBitmap()); // 取得顯示圖片物件並設定到顯示圖片的物件上
            ((TextView) findViewById(R.id.detail)).setText(  // 取得顯示結果資料的物件並設定結果
                    "日期：" + DbUtils.SDF.format(new Date(Long.parseLong(imgObj.getName().substring(imgObj.getName().indexOf("-") + 1, imgObj.getName().indexOf("-", imgObj.getName().indexOf("-") + 1))))) + "\n\n" +
                            imgObj.getMessage());


        findViewById(R.id.measure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HistoryDetailActivity.this,MainActivity.class));
                HistoryDetailActivity.this.finish();
            }
        });

        findViewById(R.id.edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryDetailActivity.this, CWMeasureActivity.class);
                startActivity(intent);
                HistoryDetailActivity.this.finish();
            }
        });
        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position = getIntent().getExtras().getInt("position");
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryDetailActivity.this); // 產生互動式訊息
                builder.setTitle("確定刪除？"); // 設定顯示訊息'
                builder.setMessage(DbUtils.SDF.format(new Date(Long.parseLong(imgObj.getName().substring(imgObj.getName().indexOf("-") + 1, imgObj.getName().indexOf("-", imgObj.getName().indexOf("-") + 1))))));
                builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final List<ImageDAO> imageviewList = getImageList(); // 取得圖片列表
                        DbUtils ih = new DbUtils(HistoryDetailActivity.this); // 產生與資料庫溝通的圖片物件
                        ImageDAO imageview[] = ih.getGroup(imageviewList.get(position).getName()); // 以群的方式取得圖片資料
                        for (int i = 0; i< imageview.length; i++) {
                            new File(STORE_PATH + "/" + imageview[i].getName()).delete(); // 刪除圖片
                            ih.delete(imageview[i]); // 刪除在資料庫的圖片資料
                        }
                        ih.close(); // 關閉資料庫連結
                        startActivity(new Intent(HistoryDetailActivity.this,HistoryActivity.class));
                        HistoryDetailActivity.this.finish();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() { // 設定刪除按鈕被點擊時
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
        });

        findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryDetailActivity.this);
                builder.setTitle("是否上傳資料？");
                builder.setMessage(DbUtils.SDF.format(new Date(Long.parseLong(imgObj.getName().substring(imgObj.getName().indexOf("-") + 1, imgObj.getName().indexOf("-", imgObj.getName().indexOf("-") + 1))))));
                builder.setPositiveButton("是", (dialog, which) -> {
                    progress = new ProgressDialog(HistoryDetailActivity.this);
                    progress.setMessage("資料上傳中．．．");
                    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progress.setCanceledOnTouchOutside(false);
                    progress.show();
                    Thread thread = new Thread(mutiThread);
                    thread.start();
                });
                builder.setNegativeButton("取消", null);
                builder.create().show();
            }
        });
    }

    private Runnable mutiThread = new Runnable(){
        public void run(){
            try{
                DbUtils ih = new DbUtils(HistoryDetailActivity.this); // 產生與資料庫溝通的圖片物件
                ImageDAO imageview = ih.get(getIntent().getExtras().getString("first"))[0]; // 取得原始圖片資料
                JSONObject json_obj_1 = new JSONObject();//用來當內層被包覆的JSON物件
                json_obj_1.put("name",imageview.getName());
                json_obj_1.put("result",imageview.getResult());
                json_obj_1.put("Image_type",imageview.getImage_type());
                json_obj_1.put("Message",imageview.getMessage());
                jsonText = json_obj_1.toString();
                Log.i("text", "json_obj_1="+json_obj_1+"\n");
                URL endpoint = new URL("http://systemdynamics.tw/im/crackSave.php?i=103&data="+ jsonText);//設定上傳網址
                HttpURLConnection httpConnection = (HttpURLConnection) endpoint.openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setDoInput(true);
                httpConnection.setDoOutput(true);
                httpConnection.setRequestProperty("Content-Type", "application/json");


                DataOutputStream outputStream = new DataOutputStream(httpConnection.getOutputStream());
                outputStream.write(jsonText.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                InputStreamReader isr = new InputStreamReader(httpConnection.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line = "";
                while( (line = br.readLine()) != null ) {
                    System.out.println(line);
                    if(line.equals("OK")){
                        Message msg = new Message();
                        msg.what = 1;
                        resultDisPlayHandler.sendMessage(msg);
                    }else{
                        Message msg = new Message();
                        msg.what = 2;
                        resultDisPlayHandler.sendMessage(msg);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                Message msg = new Message();
                msg.what = 2;
                resultDisPlayHandler.sendMessage(msg);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler resultDisPlayHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    Log.d("Test","結束執行") ;
                    progress.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(HistoryDetailActivity.this);
                    builder.setTitle("完成！");
                    builder.setMessage("資料上傳成功");
                    builder.setPositiveButton("確定", null);
                    builder.create().show();
                    break;
                case 2:
                    Log.d("Test","結束執行") ;
                    progress.dismiss();
                    builder = new AlertDialog.Builder(HistoryDetailActivity.this);
                    builder.setTitle("錯誤！");
                    builder.setMessage("資料上傳失敗");
                    builder.setPositiveButton("確定", null);
                    builder.create().show();
                    break;
            }
        }
    };

    private List<ImageDAO> getImageList() { // 取得圖片資料列表
        List<ImageDAO> imageviewList = new ArrayList<ImageDAO>(); // 建立新的圖片列表物件

        DbUtils ih = new DbUtils(this); // 產生與資料庫溝通的圖片物件
        ImageDAO imageviews[] = ih.getAll(); // 取得所有圖片資料
        ih.close(); // 關閉資料庫連結

        if (imageviews != null) // 如果有圖片資料
            for (int i = 0; i< imageviews.length; i++)
                if (imageviews[i].getName().indexOf("first") > -1) // 如果是原始圖片檔名
                    imageviewList.add(imageviews[i]); // 設定到圖片列表上

        return imageviewList;
    }
}