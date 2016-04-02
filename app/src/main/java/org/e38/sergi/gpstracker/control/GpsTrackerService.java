package org.e38.sergi.gpstracker.control;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.e38.sergi.gpstracker.logic.AndroidUtils;
import org.e38.sergi.gpstracker.logic.TrackerBdHelper;
import org.e38.sergi.gpstracker.model.BusPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * modelo tabla externa:
 * CREATE TABLE L_BUSES
 * (
 * ID_LOC NUMBER(9) PRIMARY KEY NOT NULL,
 * LATITUT NUMBER(10,6) NULL,
 * LOGITUT NUMBER(10,6) NULL,
 * TIME NUMBER(15) NULL,
 * MATRICULA VARCHAR2(30) NULL,
 * FECHA_PROCESO DATE DEFAULT SYSDATE NULL,
 * PROVIDER VARCHAR2(30) DEFAULT NULL,
 * RECIPT_ID NUMBER(9) DEFAULT NULL
 * <p>
 * );
 * <p>
 * CREATE INDEX "L_Buses_matricula_index" ON SAZNAR.L_Buses (matricula);
 * COMMENT ON TABLE SAZNAR.L_Buses IS 'buses localizaciones';
 * <p>
 * CREATE OR REPLACE TRIGGER t_L_bus
 * BEFORE INSERT ON L_Buses
 * FOR EACH ROW
 * BEGIN
 * :NEW.id_loc := S_L_BUS.nextval;
 * :NEW.FECHA_PROCESO := SYSDATE;
 * END;
 */
public class GpsTrackerService extends Service {
    public static final String KEY_MATRICULA = "MATRICULA";
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int RESPONSE_OK = 200;
    public static final int RESPONE_NO_CONTENT = 204;
    private static final String webServiceHost = "192.168.1.11";
    private static final int webServicePort = 39284;
    private static final String remotePath = "/BussesWebExternal/webresources/api.lbuses";
    private static final String webServiceURL = "http://" + webServiceHost + ":" + webServicePort + remotePath;
    private final Object locker = new Object();
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Long lastUploadTry = null;
    private SQLiteDatabase localBd;
    private String matricula;
    private TrackerBdHelper bdHelper;

    public GpsTrackerService() {

    }

    @Override
    public void onCreate() {
        bdHelper = new TrackerBdHelper(this, null);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        localBd = bdHelper.getWritableDatabase();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        matricula = intent.getStringExtra(KEY_MATRICULA);
        locationListener = new ServiceListener();
        if (matricula == null) {
            Toast.makeText(this, "Matricula invalida ", Toast.LENGTH_SHORT).show();
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
//            //DEBUG
//            Location location = new Location(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
//            location.setLongitude(location.getLatitude() + 1);
////            location.setLatitude(10);
//            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
//
        } else {
            Toast.makeText(this, "no gps permission", Toast.LENGTH_LONG).show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
        locationManager = null; //force gc

        ServiceListener listener = (ServiceListener) locationListener;
        listener.stopUpload = true;
        locationListener = null;
        localBd.close();
        localBd = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean checkHostAcces() {
        boolean ck;
        if (ck = AndroidUtils.isNetWorkAvailable(this)) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("http://" + String.valueOf(webServiceHost) + ":" + String.valueOf(webServicePort)).openConnection();
                connection.setRequestProperty("User-Agent", "Firefox 40.0: Mozilla/5.0 (X11; Linux x86_64; rv:40.0) Gecko/20100101 Firefox/40.0Linux");
                connection.setRequestProperty("Connection", "close");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                //android no deja realizar opereciones de red en el hilo principal aunque sea un service lanzara execption: NetworkOnMainThread
                ck = new AsyncTask<HttpURLConnection, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(HttpURLConnection... params) {
                        try {
                            params[0].connect();
                            return params[0].getResponseCode() == RESPONSE_OK;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                }.execute(connection).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                Log.e(getClass().getName(), "error checking conection", e);
                ck = false;
            }
        }
        return ck;
    }

    private class ServiceListener implements LocationListener {
        private final List<String> PROVIDERS = Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER);
        boolean stopUpload = false;
        private String provider = LocationManager.GPS_PROVIDER;//default is gps
        private boolean isOutOfService = false;

        public ServiceListener() {
            synchronized (locker) {
                bdHelper.cleanObsolete();
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            int id;
            synchronized (locker) {
                Toast.makeText(GpsTrackerService.this, "Localization channged", Toast.LENGTH_LONG).show();
                localBd.execSQL("INSERT INTO " + TrackerBdHelper.table_gps_track_SCD +
                                "(latitut, logitut, TIME, TIPO_LOCALIZACION, matricula, isSend) " +
                                " VALUES (?,?,?,?,?,?)",
                        new Object[]{location.getLatitude(), location.getLongitude(), System.currentTimeMillis(),
                                provider != null && PROVIDERS.contains(provider) ? provider : "unknown provider",
                                matricula, 0});
                Cursor cursor = localBd.rawQuery("SELECT id FROM H_localizacion WHERE matricula = ? ORDER BY id DESC LIMIT 1", new String[]{matricula});
                cursor.moveToFirst();
                id = cursor.getInt(0);
                cursor.close();
            }
            tryToUploadChanges(location, id);
        }

        private void tryToUploadChanges(Location location, int id) {
            synchronized (locker) {
                if (checkHostAcces()) {
                    BusPoint busPoint = new BusPoint(matricula, provider, location.getLongitude(), location.getLatitude(), System.currentTimeMillis());
                    JSONObject jsonObject = new JSONObject(busPoint.toMap());
                    try {
                        jsonObject.put("reciptId", id);
                    } catch (JSONException e) {
                        Log.e(GpsTrackerService.class.getName(), "uploadError", e);
                    }
                    try {
                        if (putInWebService(jsonObject)) {
                            localBd.execSQL("UPDATE H_localizacion SET isSend = 1 WHERE id = ? ", new Object[]{id});
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(getClass().getName(), "Host not reachable");
                    Toast.makeText(GpsTrackerService.this, "Host not reachable", Toast.LENGTH_LONG).show();
                }
            }
        }

        private Boolean putInWebService(final JSONObject jsonObject) throws ExecutionException, InterruptedException {
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpPost post = new HttpPost(webServiceURL);
                        post.setHeader("content-type", "application/json");

                        StringEntity entity = new StringEntity(jsonObject.toString());
                        post.setEntity(entity);

                        HttpResponse resp = httpClient.execute(post);
                        if (!(resp.getStatusLine().getStatusCode() == RESPONSE_OK || resp.getStatusLine().getStatusCode() == RESPONE_NO_CONTENT)) {
                            throw new IOException("not OK response code");
                        }
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }
            }.execute().get();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    isOutOfService = false;
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    isOutOfService = true;
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            this.provider = provider;
            // isOutOfService = false;
        }

        @Override
        public void onProviderDisabled(String provider) {
            this.provider = provider;
        }

    }
}