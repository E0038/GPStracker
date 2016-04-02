package org.e38.sergi.gpstracker.logic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import org.e38.sergi.gpstracker.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by sergi on 3/10/16.
 */
public class TrackerBdHelper extends SQLiteOpenHelper {
    public static final String BD_NAME = "GPS_TRACKER";
    public static final String table_gps_track_SCD = "H_localizacion";
    public static final String tabla_matriculas = "matriculas";
    public static final long MILIS_IN_DAY = 86400000L;
    private Context context;

    public TrackerBdHelper(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, BD_NAME, factory, context.getResources().getInteger(R.integer.BD_VERSION));
        this.context = context;
    }

    /**
     * elimina las loalizaciones obsoletas la no enviadas anteriores a un 4 dias y las enviadas anteriores a 2dias
     */
    public void cleanObsolete() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM H_localizacion WHERE isSend = 0 AND TIME <= ?", new Object[]{System.currentTimeMillis() - (MILIS_IN_DAY << 2)});
        db.execSQL("DELETE FROM H_localizacion WHERE isSend = 1 AND TIME <= ?", new Object[]{System.currentTimeMillis() - (MILIS_IN_DAY << 1)});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + table_gps_track_SCD
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT ,latitut INTEGER, logitut INTEGER, TIME INTEGER,TIPO_LOCALIZACION TEXT,matricula TEXT, isSend INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tabla_matriculas + " (matricula TEXT,TOKEN_UUID TEXT)");
        db.execSQL("INSERT INTO " + tabla_matriculas + " (matricula,TOKEN_UUID) VALUES ('B567898BBB',?)", new Object[]{"123456789"});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + table_gps_track_SCD);
        db.execSQL("DROP TABLE IF EXISTS " + tabla_matriculas);
        onCreate(db);
    }
}

