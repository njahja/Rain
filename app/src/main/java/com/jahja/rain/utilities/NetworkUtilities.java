package com.jahja.rain.utilities;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Nick on 8/7/2018.
 */

public class NetworkUtilities {

    final static String CURRENT_WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    final static String FORECAST_WEATHER_URL = "http://api.openweathermap.org/data/2.5/forecast";
    final static String API_KEY = "ae51a269b4fa4fd4ba0ac596705f74fc";
    static String units = "metric";
    final static int cnt = 8; //data every 3 hours * 8 = 24 hours in a day

    final static String PARAM_LAT = "lat";
    final static String PARAM_LON = "lon";
    final static String PARAM_UNITS = "units";
    final static String PARAM_CNT = "cnt";
    final static String PARAM_API_ID = "APPID";

    public static URL buildUrl(Double[] coordinates) {
        Uri builtUri = Uri.parse(CURRENT_WEATHER_URL).buildUpon()
                .appendQueryParameter(PARAM_LAT, Double.toString(coordinates[0]))
                .appendQueryParameter(PARAM_LON, Double.toString(coordinates[1]))
                .appendQueryParameter(PARAM_UNITS, units)
                .appendQueryParameter(PARAM_API_ID, API_KEY)
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * Taken from T02.06-Exercise-AddPolish
     *
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

}
