package com.jahja.rain;

import android.app.Notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationManagerCompat;

import com.jahja.rain.utilities.NetworkUtilities;

import org.json.JSONObject;

import java.net.URL;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String RAIN_NOTIF_ID = "notification-id";
    public static final String NOTIFICATION = "notification";
    public static final String RECEIVER_COORDINATES = "coordinates";

    @Override
    public void onReceive(Context context, Intent intent) {
        // an Intent broadcast.
        final Context innerContext = context;
        final Intent innerIntent = intent;
        double[] unboxCoordinates = intent.getDoubleArrayExtra(RECEIVER_COORDINATES);
        Double[] currentCoordinates = {unboxCoordinates[0], unboxCoordinates[1]};
        final PendingResult pendingResult = goAsync();
        AsyncTask<Double[], Void, Boolean> asyncTask = new AsyncTask<Double[], Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Double[]... params) {
                URL requestUrl = NetworkUtilities.buildUrl(NetworkUtilities.FORECAST_WEATHER_URL,
                        params[0]);

                try{
                    String jsonStringResponse = NetworkUtilities.getResponseFromHttpUrl(requestUrl);
                    JSONObject jsonResponseObject =  new JSONObject(jsonStringResponse);
                    String weatherCode;
                    if (jsonResponseObject.getInt("cod") != 200) {
                        pendingResult.finish();
                        return null;
                    }
                    for (int i = 0; i < 8; i++) {
                        weatherCode = jsonResponseObject.getJSONArray("list").getJSONObject(i)
                                .getJSONArray("weather").getJSONObject(0).getString("id");

                        if (Integer.valueOf(weatherCode) >= 200 && Integer.valueOf(weatherCode) <= 600) {
                            pendingResult.finish();
                            return true;
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Must call finish() so the BroadcastReceiver can be recycled.
                pendingResult.finish();
                return false;
            }

            @Override
            protected void onPostExecute(Boolean isRaining) {
                if (isRaining) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(innerContext);
                    Notification notification = innerIntent.getParcelableExtra(NOTIFICATION);
                    int id = innerIntent.getIntExtra(RAIN_NOTIF_ID, 0);
                    notificationManager.notify(id, notification);
                }
            }
        };
                asyncTask.execute(currentCoordinates);
    }
}
