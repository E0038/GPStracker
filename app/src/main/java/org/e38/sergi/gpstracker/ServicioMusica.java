package org.e38.sergi.gpstracker;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by sergi on 3/10/16.
 */
public class ServicioMusica extends Service {
    MediaPlayer reproductor = new MediaPlayer();

    public ServicioMusica() {
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Servicio creado",
                Toast.LENGTH_SHORT).show();
        reproductor = MediaPlayer.create(this, R.raw.audio);
    }


    @Override
    public int onStartCommand(Intent intenc, int flags, int idArranque) {
        Toast.makeText(this, "Servicio arrancado " + idArranque,
                Toast.LENGTH_SHORT).show();
        reproductor.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Servicio detenido",
                Toast.LENGTH_SHORT).show();
        reproductor.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
