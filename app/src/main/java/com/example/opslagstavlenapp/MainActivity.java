package com.example.opslagstavlenapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//OK Http3
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    Button CameraButton;
    ImageView PictureView;
    Button GetImageButton;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public String apiUrl = "http://192.168.0.10:5158/api/";
    public String takenPicture;

    OkHttpClient client = new OkHttpClient();


    public static final int RequestPermissionCode = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraButton = findViewById(R.id.CameraButton);
        PictureView = findViewById(R.id.PictureView);
        GetImageButton = findViewById(R.id.GetImagesButton);
        EnableRuntimePermission();

        CameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 7);
            }
        });

        GetImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    GetRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7 && resultCode == RESULT_OK) {
            Bitmap picture = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            takenPicture = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            PictureView.setImageBitmap(picture);
            try {
                PostRequest(apiUrl, takenPicture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void EnableRuntimePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.CAMERA)) {
            Toast.makeText(MainActivity.this,"CAMERA permission allows us to Access CAMERA app",     Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);
        }
    }

    public void PostRequest(String postUrl, String postBody) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, postBody);

        //.Builder urlBuilder = HttpUrl.parse(apiUrl + "Image/PostImage").newBuilder();
        //urlBuilder.addQueryParameter("imageString", postBody);
        //String builtUrl = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(postUrl + "Image/PostImage")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("TAG", response.body().string());
            }
        });
    }
    void GetRequest() throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(apiUrl + "Image/GetImages")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String myResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });

            }
        });
    }
}
