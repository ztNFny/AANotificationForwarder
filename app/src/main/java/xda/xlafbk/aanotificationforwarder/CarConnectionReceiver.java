package xda.xlafbk.aanotificationforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Statically registered receiver for androidx.car.app.connection.action.CAR_CONNECTION_UPDATED.
 * Android delivers this even when the app process is dead, waking it up.
 * Replaces the dynamic BroadcastReceiver that was previously registered inside AutoConnectionDetector.
 */
public class CarConnectionReceiver extends BroadcastReceiver {
    private static final String TAG = "CarConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "CAR_CONNECTION_UPDATED received");
        new AutoConnectionDetector(context).queryForState();
    }
}
