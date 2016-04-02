package org.e38.sergi.gpstracker.model;

import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sergi on 4/1/16.
 * Container clase for json requests
 */
public class BusPoint implements Serializable {
    public static final String MOCK_FECHA_PROCESSO = "2016-04-01T00:00:00+02:00";
    public static final int MOCK_ID_LOC = 3;
    private String matricula, provider;
    private double longitut, latitut;
    private long time;

    public BusPoint() {
    }

    public BusPoint(String matricula, String provider, double longitut, double latitut, long time) {
        this.matricula = matricula;
        this.provider = provider;
        this.longitut = longitut;
        this.latitut = latitut;
        this.time = time;
    }

    public static BusPoint extractInstance(Map<String, Object> map) {
        return null;
    }

    public String getMatricula() {
        return matricula;
    }

    public BusPoint setMatricula(String matricula) {
        this.matricula = matricula;
        return this;
    }

    public String getProvider() {
        return provider;
    }

    public BusPoint setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public double getLongitut() {
        return longitut;
    }

    public BusPoint setLongitut(double longitut) {
        this.longitut = longitut;
        return this;
    }

    public double getLatitut() {
        return latitut;
    }

    public BusPoint setLatitut(double latitut) {
        this.latitut = latitut;
        return this;
    }

    public long getTime() {
        return time;
    }

    public BusPoint setTime(long time) {
        this.time = time;
        return this;
    }

    public Map<String, Object> toMap() {
        //'{"fechaProceso":"2016-04-01T00:00:00+02:00","idLoc":3,"latitut":120,"logitut":30,"matricula":"23456",
        // "provider":"gps","reciptId":16,"time":999999}'

        Map<String, Object> map = new HashMap<>();
        map.put("fechaProceso", MOCK_FECHA_PROCESSO);//da igual lo que pongamos se ignorara pero la api requiere que exista el campo
        map.put("idLoc", MOCK_ID_LOC);//se ignora
        map.put("matricula", matricula);
        map.put("provider", provider);
        map.put("logitut", longitut);
        map.put("latitut", latitut);
        map.put("time", time);

//        try {
//            for (Field field : getClass().getDeclaredFields()) {
//                map.put(field.getName(), field.get(this));
//            }
//        } catch (IllegalAccessException e) {
//            Log.e(getClass().getName(), "acces error", e);
//        }
        return map;
    }
}
