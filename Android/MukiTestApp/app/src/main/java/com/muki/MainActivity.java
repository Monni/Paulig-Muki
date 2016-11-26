package com.muki;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.muki.core.MukiCupApi;
import com.muki.core.MukiCupCallback;
import com.muki.core.model.Action;
import com.muki.core.model.DeviceInfo;
import com.muki.core.model.ErrorCode;
import com.muki.core.model.ImageProperties;
import com.muki.core.util.ImageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.os.Handler;

public class MainActivity extends AppCompatActivity implements AsyncResponse {

    private EditText mSerialNumberEdit;
    private TextView mCupIdText;
    private TextView mDeviceInfoText;
    private ImageView mCupImage;
    private ProgressDialog mProgressDialog;

    private int mContrast = ImageProperties.DEFAULT_CONTRACT;

    private String mCupId;
    private MukiCupApi mMukiCupApi;

    private List<String> quoteList;
    private String quoteOfTheMoment;
    private Bitmap finalImage;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private Handler handler = new Handler();

    public static final String PREFS_NAME = "MukiPrefsFile";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME,0);
        mCupId = settings.getString("cupId", "");
        setContentView(R.layout.activity_main);

        quoteList = new ArrayList<>();
        // Start timed pictures after 60s
        handler.postDelayed(runnable, 60000);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Loading. Please wait...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mMukiCupApi = new MukiCupApi(getApplicationContext(), new MukiCupCallback() {
            @Override
            public void onCupConnected() {
                showToast("Cup connected");
            }

            @Override
            public void onCupDisconnected() {
                showToast("Cup disconnected");
            }

            @Override
            public void onDeviceInfo(DeviceInfo deviceInfo) {
                hideProgress();
                mDeviceInfoText.setText(deviceInfo.toString());
            }

            @Override
            public void onImageCleared() {
                showToast("Image cleared");
            }

            @Override
            public void onImageSent() {
                showToast("Image sent");
            }

            @Override
            public void onError(Action action, ErrorCode errorCode) {
                showToast("Error:" + errorCode + " on action:" + action);
            }
        });

        Launch();

        mSerialNumberEdit = (EditText) findViewById(R.id.serailNumberText);
        mCupIdText = (TextView) findViewById(R.id.cupIdText);
        mCupIdText.setText(mCupId);
        mDeviceInfoText = (TextView) findViewById(R.id.deviceInfoText);
        mCupImage = (ImageView) findViewById(R.id.imageSrc);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timedLaunch();
            handler.postDelayed(this, 55000);
        }
    };

    private void Launch() {
        Toast.makeText(MainActivity.this, "Loading image..", Toast.LENGTH_LONG).show();
        AsyncJsonFetcher asyncJsonFetcher = new AsyncJsonFetcher(MainActivity.this);
        asyncJsonFetcher.delegate = MainActivity.this;
        asyncJsonFetcher.execute("https://api.whatdoestrumpthink.com/api/v1/quotes");
    }

    private void timedLaunch(){
        Toast.makeText(MainActivity.this, "Loading timed image..", Toast.LENGTH_SHORT).show();
        quoteOfTheMoment = getRandomText();
        finalImage = createImage();
        setupImage();
        mMukiCupApi.sendImage(finalImage, new ImageProperties(mContrast), mCupId);
    }

    private void setupImage() {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                Log.d("setupImage", "Asynctask started");
               // Bitmap result = Bitmap.createBitmap(finalImage);
                ImageUtils.convertImageToCupImage(finalImage, mContrast);
                return finalImage;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mCupImage.setImageBitmap(bitmap);
                hideProgress();
            }
        }.execute();
    }



    public void send(View view) {
        showProgress();
        mMukiCupApi.sendImage(finalImage, new ImageProperties(mContrast), mCupId);
    }


    public void request(View view) {
        String serialNumber = mSerialNumberEdit.getText().toString();
        showProgress();
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    String serialNumber = strings[0];
                    return MukiCupApi.cupIdentifierFromSerialNumber(serialNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                mCupId = s;
                mCupIdText.setText(mCupId);
                hideProgress();
            }
        }.execute(serialNumber);
    }

    public void deviceInfo(View view) {
        showProgress();
        mMukiCupApi.getDeviceInfo(mCupId);
    }

    private void showToast(final String text) {
        hideProgress();
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void showProgress() {
        mProgressDialog.show();
    }

    private void hideProgress() {
        mProgressDialog.dismiss();
    }

    public void onAsyncJsonFetcherComplete(JSONObject json) {
        try {
            JSONObject jsonObject = json.getJSONObject("messages");
            JSONArray jsonArray = jsonObject.getJSONArray("personalized");
            for (int i = 0; i < jsonArray.length(); i++) {
                quoteList.add(jsonArray.get(i).toString());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        quoteOfTheMoment = getRandomText();
        finalImage = createImage();
        setupImage();
        mMukiCupApi.sendImage(finalImage, new ImageProperties(mContrast), mCupId);
    }

    private String getRandomText() {
        Random random = new Random();
        String text = "";
        // Check quoteList length for randomizer
        int length = quoteList.size();
        while (text.length() <= 1 || text.length() >= 30) {
            int n = random.nextInt(length);
            text = quoteList.get(n);
        }
        return text;
    }

    public Bitmap createImage() {
        String quote = quoteOfTheMoment;
        Bitmap map = BitmapFactory.decodeResource(getResources(), R.drawable.trump);
        Bitmap bitmap = map.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(90);
        paint.setTextScaleX(1);;
        String quote1 = "";
        String quote2 = "";


        Log.d("Quote", quote);
        int length = quote.length();
        if (length >= 24) {
            String[] splitted = quote.split("\\s+");
            Log.d("Splitted", splitted.toString());
            for (int i = 0; i < splitted.length / 2; i++) {
                quote1 += splitted[i] + " ";
            }
            for (int i = splitted.length / 2; i < splitted.length; i++) {
                quote2 += splitted[i] + " ";
            }
            canvas.drawText(quote1, 200, 280, paint);
            canvas.drawText(quote2, 200, 365, paint);

           // String first = quote.substring(0, quote.length() / 2);
          //  String second = quote.substring(quote.length() / 2);
         //   canvas.drawText(first, 200, 280, paint);
          //  canvas.drawText(second, 200, 365, paint);
        } else {
            canvas.drawText(quote, 100, 280, paint);
        }
        return bitmap;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();
        editor.putString("cupId", mCupId);
        editor.commit();
    }

}
