package com.muki;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by miika on 26.11.2016.
 */

public interface AsyncResponse
{
    void onAsyncJsonFetcherComplete(JSONObject json);
}
