package com.jahja.rain;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.jahja.rain.utilities.NetworkUtilities;

import org.json.JSONObject;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Typeface weatherFont;

    FusedLocationProviderClient mFusedLocationClient;

    TextView tvCityName;
    TextView tvWeatherIcon;
    TextView tvWeatherInfo;
    TextView tvTemperature;
    TextView tvLastUpdatedInfo;
    ProgressBar progressBar;

    Task<Location> task;
    Location currentLocation;
    Double[] currentCoordinates = new Double[2]; // 0 is lat, 1 is lon
    private static final String CHANNEL_ID = "rain_notif_channel";
    private static final int RAIN_NOTIF_ID = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvCityName = (TextView) findViewById(R.id.tv_city);
        tvWeatherIcon = (TextView) findViewById(R.id.tv_weather_icon);
        tvWeatherInfo = (TextView) findViewById(R.id.tv_weather_info);
        tvTemperature = (TextView) findViewById(R.id.tv_temperature);
        tvLastUpdatedInfo = (TextView) findViewById(R.id.tv_last_updated_info);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //get instance of Fused Location Provider Client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weathericons.ttf");
        tvWeatherIcon.setTypeface(weatherFont);

        createNotificationChannel();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Rain")
                .setContentText("Forecast of rain tomorrow.")
                .setSmallIcon(R.drawable.icons8_rain_64)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(RAIN_NOTIF_ID, mBuilder.build());

        myCheckPermissions();
        getWeatherData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_action:
                getWeatherData();
                return true;
            case R.id.settings_action:
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID);
                intent.setClass(this, com.jahja.rain.SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.app_info_action:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.app_info)
                        .setTitle(R.string.app_info_title)
                        .setIcon(R.drawable.ic_info_black_24dp)
                        .setPositiveButton(R.string.close_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //close dialog
                            }
                        });
                builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void myCheckPermissions() {
        //check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                } else {
                    finish();
                }
            }
        }
    }

    public void getWeatherData() {
        task = mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                currentLocation = task.getResult();
                currentCoordinates[0] = currentLocation.getLatitude();
                currentCoordinates[1] = currentLocation.getLongitude();
                new FetchWeatherData().execute(currentCoordinates);
            }
        });
    }

    class FetchWeatherData extends AsyncTask<Double[], Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Double[]... params) {
            URL requestUrl = NetworkUtilities.buildUrl(params[0]);

            try{
                String jsonStringResponse = NetworkUtilities.getResponseFromHttpUrl(requestUrl);
                JSONObject jsonResponseObject =  new JSONObject(jsonStringResponse);
                if (jsonResponseObject.getInt("cod") != 200) {
                    return null;
                }
                return jsonResponseObject;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject weatherData) {
            try {
                tvCityName.setText(weatherData.getString("name").toUpperCase(Locale.US) +
                        ", " + weatherData.getJSONObject("sys").getString("country"));

                tvWeatherInfo.setText(weatherData.getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("description").toUpperCase(Locale.US));

                tvTemperature.setText(String.format("%.2f", weatherData.getJSONObject("main").getDouble("temp"))+ " ℃");

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String updatedOn = df.format(new Date(weatherData.getLong("dt")*1000));
                tvLastUpdatedInfo.setText("Last Updated: " + updatedOn);
                Log.i("time string", updatedOn);

                setWeatherIcon(weatherData.getJSONArray("weather")
                                .getJSONObject(0).getInt("id"),
                        weatherData.getJSONObject("sys").getLong("sunrise") * 1000,
                        weatherData.getJSONObject("sys").getLong("sunset") * 1000);

                progressBar.setVisibility(View.INVISIBLE);
            } catch(Exception e) {
                Log.e("onPostExecute error", "One or more fields not found in JSON data");
            }
        }
    }

    private void setWeatherIcon(int id, long sunrise, long sunset){
        String weatherCode = "wi_owm_";
        long currentTime = new Date().getTime();
        if(currentTime>=sunrise && currentTime<sunset) {
            weatherCode += "day_";
        } else {
            weatherCode += "night_";
        }
        weatherCode += id;
        int stringId = getResources().getIdentifier(weatherCode, "string", this.getPackageName());
        Log.i("string id value", Integer.toString(stringId));
        Log.i("weather code", weatherCode);
        tvWeatherIcon.setText(getString(stringId));
    }
}
