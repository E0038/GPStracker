package org.e38.sergi.gpstracker.logic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by sergi on 4/1/16.
 */
public class AndroidUtils {
    public static boolean isNetWorkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
