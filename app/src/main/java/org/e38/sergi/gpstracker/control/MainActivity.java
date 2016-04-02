package org.e38.sergi.gpstracker.control;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.e38.sergi.gpstracker.R;
import org.e38.sergi.gpstracker.ServicioMusica;
import org.e38.sergi.gpstracker.logic.TrackerBdHelper;
import org.e38.sergi.gpstracker.model.BusPoint;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_Matricula = "Matricula";
    EditText editTextTokenUUID;
    private String matricula;
    private Button bttArrancar;
    private Button bttDetener;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextTokenUUID = (EditText) findViewById(R.id.editTextUUID);

        bttArrancar = (Button) findViewById(R.id.boton_arrancar);
        bttDetener = (Button) findViewById(R.id.boton_detener);

//        //DEBUG:
//        Intent intent = new Intent(MainActivity.this, GpsTrackerService.class);
//        intent.putExtra(GpsTrackerService.KEY_MATRICULA, "B567898BBB");
//        startService(new Intent(MainActivity.this,
//                ServicioMusica.class));
//        startService(intent);

        new BusPoint("foo", "var", 120, 23, System.currentTimeMillis()).toMap();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.contains(KEY_Matricula)) {
            bttArrancar.setEnabled(true);
            bttDetener.setEnabled(true);
            matricula = settings.getString(KEY_Matricula, null);
        }
        findViewById(R.id.buttonConf).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextTokenUUID.getText() != null && !editTextTokenUUID.getText().toString().isEmpty()) {
                    String tokenUuid = editTextTokenUUID.getText().toString();
                    SQLiteDatabase db = new TrackerBdHelper(MainActivity.this, null).getWritableDatabase();

                    Cursor cursor = db.rawQuery("SELECT matricula FROM " + TrackerBdHelper.tabla_matriculas + " WHERE TOKEN_UUID=?", new String[]{tokenUuid});
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        matricula = cursor.getString(0);
                        bttArrancar.setEnabled(true);
                        bttDetener.setEnabled(true);
                        v.setEnabled(false);
                        ((TextView) findViewById(R.id.textMatricula)).setText(matricula);
                    } else {
                        Toast.makeText(MainActivity.this, "incorrect token", Toast.LENGTH_SHORT).show();
                        bttArrancar.setEnabled(false);
                        bttDetener.setEnabled(false);
                    }
                    cursor.close();
                    db.close();
                }
            }
        });

        bttArrancar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GpsTrackerService.class);
                intent.putExtra(GpsTrackerService.KEY_MATRICULA, matricula);
                if (Build.VERSION.SDK_INT >= 23) { //aunque esten en el manifest a partir de la api 23 algunos permisos requieren preguntar al usario
                    if (!checkLocalization()) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionInfo.PROTECTION_DANGEROUS);
                    }
                    if (!checkNetwork23()) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, PermissionInfo.PROTECTION_DANGEROUS);
                    }
                }
                startService(intent);
                startService(new Intent(MainActivity.this,
                        ServicioMusica.class));
            }
        });

        bttDetener.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GpsTrackerService.class);
                intent.putExtra(GpsTrackerService.KEY_MATRICULA, matricula);

                //TODO change service
                stopService(new Intent(MainActivity.this,
                        ServicioMusica.class));
                stopService(intent);
                findViewById(R.id.buttonConf).setEnabled(true);
                bttDetener.setEnabled(false);
                bttArrancar.setEnabled(false);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkNetwork23() {
        return checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkLocalization() {
        return (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

}

