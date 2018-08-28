package com.liang.netimagineview;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private PhotoView Iv;
    private ArrayList<String> Paths;
    private int count=0;
    private float StartX;
    private float EndX;
    private Button Pre;
    private Button Next;
    private LinearLayout Ll;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Pre = findViewById(R.id.pre);
        Next = findViewById(R.id.next);
        Iv = findViewById(R.id.iv);
        Ll = findViewById(R.id.ll);
        Ll.setOnTouchListener(new MYLL());
        Iv.setOnTouchListener(new MYLL());
        LoadImaginePath();
    }
    /**
     *
     */
    public class MYLL implements View.OnTouchListener{

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    StartX = motionEvent.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    EndX = motionEvent.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    float change = StartX - EndX;
                    if (change>=400){
                        loadimaginebypath(Paths.get(count));
                        count++;
                        if (count==Paths.size())
                            count=0;
                    }else if (change<=-400){
                        loadimaginebypath(Paths.get(count));
                        count--;
                        if (count<0)
                            count=Paths.size()-1;
                    }
                    break;
            }
            return true;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler hh =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    Iv.setImageBitmap((Bitmap) msg.obj);
                    break;
                    default:break;
            }
        }
    };

    public void pre(View view) {
        loadimaginebypath(Paths.get(count));
        count--;
        if (count<0)
            count=Paths.size()-1;
    }
    public void next(View view) {
        loadimaginebypath(Paths.get(count));
        count++;
        if (count==Paths.size())
            count=0;

    }
    private void LoadImaginePath() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    URL url = new URL("http://192.168.23.1/tupian.html");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    InputStream is = connection.getInputStream();
                    File file = new File(getCacheDir(), "info.txt");
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len=is.read(buffer))!=-1){
                        fos.write(buffer,0,len);
                    }
                    is.close();
                    fos.close();
                    beginloadimagine();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void beginloadimagine() {
        Paths = new ArrayList<>();
        File file = new File(getCacheDir(), "info.txt");
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line=reader.readLine())!=null){
                Paths.add(line);
            }
            fis.close();
            loadimaginebypath(Paths.get(count));
            count++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadimaginebypath(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(getCacheDir(), getDownLoadFileName(path));
                if (file.exists()&&file.length()>0){
                    Message message = new Message();
                    message.what=1;
                    message.obj=BitmapFactory.decodeFile(file.getPath());
                    hh.sendMessage(message);
                }else {
                    try {
                        URL url = new URL(path);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        int code = connection.getResponseCode();
                        if(code==200){
                            InputStream inputStream = connection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            FileOutputStream fos = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG,10,fos);
                            fos.close();
                            inputStream.close();
                            Message message = new Message();
                            message.what=1;
                            message.obj=bitmap;
                            hh.sendMessage(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    /**
     * @param path 文件路径
     * @return 文件名
     */
    private static String getDownLoadFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
