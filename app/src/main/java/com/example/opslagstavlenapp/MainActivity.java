package com.example.opslagstavlenapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    Button CameraButton;
    ImageView PictureView;
    Button GetImageButton;
    RelativeLayout Layout;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public String apiUrl = "http://192.168.0.10:5158/api/";
    public String takenPicture;
    public int xDelta;
    public int yDelta;

    public List<String> imageList;

    OkHttpClient client = new OkHttpClient();


    public static final int RequestPermissionCode = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraButton = findViewById(R.id.CameraButton);
        PictureView = findViewById(R.id.PictureView);
        GetImageButton = findViewById(R.id.GetImagesButton);
        Layout = findViewById(R.id.RelativeImageLayout);
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

    /***
     * Called when the Intent for the camera is returned.
     * @param requestCode Code for checking what intent is being parsed
     * @param resultCode The result code
     * @param data The intent
     */
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

    /***
     * Post request to the API for posting the picture taken by the camera
     * @param postUrl The post url to the database
     * @param postBody Body containing the base64 image taken by the camera
     * @throws IOException
     */
    public void PostRequest(String postUrl, String postBody) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, postBody);

        Request request = new Request.Builder()
                .url(postUrl + "Image/PostImage?imageString=" + postBody)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }

    /***
     * Get request from the API. Puts each base64 image from the response into a list, converts them to bitmaps
     * and creates a RelativeLayout with and ImageView for each base64 string
     * @throws IOException
     */
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

                final String apiResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            imageList = Arrays.asList(apiResponse.split(","));

                            for (int i = 0; i < imageList.size(); i++) {
                                Bitmap imageBitMap = GetBitmapImageFromBase64(imageList.get(i));
                                ImageView imageView = new ImageView(Layout.getContext());
                                imageView.setImageBitmap(imageBitMap);
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(550, 550);
                                imageView.setLayoutParams(params);
                                imageView.setOnTouchListener(onTouchListener());
                                Layout.addView(imageView);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                }
        });
    }

    public Bitmap GetBitmapImageFromBase64(String base64String){
        byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
        Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return decodedImage;
    }

    /***
     * This method adds an on touch listener to each image view/picture to make them draggable within the Relative layout
     * @return
     */
    private View.OnTouchListener onTouchListener(){
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final int x = (int) motionEvent.getRawX();
                final int y = (int) motionEvent.getRawY();
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                                view.getLayoutParams();
                        xDelta = x - params.leftMargin;
                        yDelta = y - params.topMargin;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                        layoutParams.leftMargin = x - xDelta;
                        layoutParams.topMargin = y - yDelta;
                        layoutParams.rightMargin = 0;
                        layoutParams.bottomMargin = 0;
                        view.setLayoutParams(layoutParams);

                }
                return true;
            }
        };
    }
}
