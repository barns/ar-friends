package uk.co.barnaby_taylor.ar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by kajetan on 22/03/15.
 */
public class HTTPClientAR {

        public void sendRequest(String s) {
            new HttpAsyncTask().execute(s);
        }

        public static String GET(String url){
            InputStream inputStream = null;
            String result = "";
            try {

                // create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                // make GET request to the given URL
                HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

                // receive response as inputStream
                inputStream = httpResponse.getEntity().getContent();

                // convert inputstream to string
                if(inputStream != null)
                    result = convertInputStreamToString(inputStream);

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

            return result;
        }

        private static String convertInputStreamToString(InputStream inputStream) throws IOException{
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while((line = bufferedReader.readLine()) != null)
                result += line;

            inputStream.close();
            return result;

        }

        private class HttpAsyncTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... urls) {
                String output = GET(urls[0]);
                Log.d("InputStream", output);

                ArrayList<Person> persons = new ArrayList<>();

                try {
                    JSONObject obj = new JSONObject(output);
                    JSONArray arr = obj.getJSONArray("content");
                    for (int i = 0; i < arr.length(); i++) {
                        String fb_name = arr.getJSONObject(i).getString("fb_name");
                        String latitude = arr.getJSONObject(i).getString("latitude");
                        String longitude = arr.getJSONObject(i).getString("longitude");
                        String altitude = arr.getJSONObject(i).getString("altitude");

                        Person user = new Person(fb_name);
                        Location loc = new Location("manual");
                        loc.setLatitude(Double.parseDouble(latitude));
                        loc.setLongitude(Double.parseDouble(longitude));
                        loc.setAltitude(Double.parseDouble(altitude));
                        user.setLocation(loc);

                        persons.add(user);
                    }
                } catch (Exception e) {
                    Log.d("InputStream", "JSON Fail");
                    return output;
                }

                try {
                    OverlayView.users = new LinkedBlockingQueue<>();
                    for (Person user : persons) {
                        OverlayView.users.put(user);
                    }
                } catch (Exception e) {
                    Log.d("InputStream", "Blocking Queue Fail");
                }

                return output;
            }
        }
    }
