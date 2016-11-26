package com.muki;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by miika on 26.11.2016.
 */

public class AsyncJsonFetcher extends AsyncTask<String, Void, JSONObject> {
    public AsyncResponse delegate = null;
    public AsyncJsonFetcher(AsyncResponse Delegate)
    {
        delegate = Delegate;
    }
    @Override
    protected JSONObject doInBackground(String... urls) {
        Log.d("AsyncJsonFetcher", "Started");
        HttpURLConnection urlConnection = null;
        JSONObject json = null;
        try {
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            json = new JSONObject(stringBuilder.toString());
        } catch (IOException e) {
            Log.d("AsyncJsonFetcher", "doInBackground:" + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d("AsyncJsonFetcher", "doInBackground:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return json;
    }

    protected void onPostExecute(JSONObject json) {
        if (json == null)
        {
            Log.d("asyncJsonFetcher", "JSONArray null");
        }
        delegate.onAsyncJsonFetcherComplete(json); // calls AsyncResponse.java interface
    }

}
