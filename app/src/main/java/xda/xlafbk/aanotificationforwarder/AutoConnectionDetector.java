package xda.xlafbk.aanotificationforwarder;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// Source: https://stackoverflow.com/a/75292070
public class AutoConnectionDetector {
    private final Context context;
    private static String TAG = "AutoConnectionDetector";
    private final CarConnectionBroadcastReceiver carConnectionReceiver = new CarConnectionBroadcastReceiver();
    private final CarConnectionQueryHandler carConnectionQueryHandler;
    // columnName for provider to query on connection status
    private static final String CAR_CONNECTION_STATE = "CarConnectionState";

    // auto app on your phone will send broadcast with this action when connection state changes
    private final String ACTION_CAR_CONNECTION_UPDATED = "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED";

    // phone is not connected to car
    private static final int CONNECTION_TYPE_NOT_CONNECTED = 0;

    // phone is connected to Automotive OS
    private final int CONNECTION_TYPE_NATIVE = 1;

    // phone is connected to Android Auto
    private final int CONNECTION_TYPE_PROJECTION = 2;

    private final int QUERY_TOKEN = 42;

    private final String CAR_CONNECTION_AUTHORITY = "androidx.car.app.connection";

    private final Uri PROJECTION_HOST_URI = new Uri.Builder().scheme("content").authority(CAR_CONNECTION_AUTHORITY).build();

    public interface OnCarConnectionStateListener {
        void onCarConnected();

        void onCarDisconnected();
    }

    private static List<OnCarConnectionStateListener> listeners = new ArrayList<>();

    public void setListener(OnCarConnectionStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(OnCarConnectionStateListener listener) {
        listeners.remove(listener);
    }

    public AutoConnectionDetector(Context context) {
        this.context = context;
        carConnectionQueryHandler = new CarConnectionQueryHandler(context.getContentResolver());
    }

    public void registerCarConnectionReceiver() {
        context.registerReceiver(carConnectionReceiver, new IntentFilter(ACTION_CAR_CONNECTION_UPDATED));
        queryForState();
        Log.i(TAG, "registerCarConnectionReceiver: ");
    }

    public void unRegisterCarConnectionReceiver() {
        context.unregisterReceiver(carConnectionReceiver);
        Log.i(TAG, "unRegisterCarConnectionReceiver: ");
    }

    private void queryForState() {
        String[] projection = {CAR_CONNECTION_STATE};
        carConnectionQueryHandler.startQuery(
                QUERY_TOKEN,
                null,
                PROJECTION_HOST_URI,
                projection,
                null,
                null,
                null
        );
    }

    private static void notifyCarConnected() {
        if (listeners != null) {
            listeners.forEach(l -> { l.onCarConnected(); });
        }
    }

    private static void notifyCarDisconnected() {
        if (listeners != null) {
            listeners.forEach(l -> { l.onCarDisconnected(); });
        }
    }

    class CarConnectionBroadcastReceiver extends BroadcastReceiver {
        // query for connection state every time the receiver receives the broadcast
        @Override
        public void onReceive(Context context, Intent intent) {
            queryForState();
        }
    }

    private static class CarConnectionQueryHandler extends AsyncQueryHandler {
        public CarConnectionQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor response) {
            if (response == null) {
                Log.w(TAG, "Null response from content provider when checking connection to the car, treating as disconnected");
                notifyCarDisconnected();
                return;
            }
            int carConnectionTypeColumn = response.getColumnIndex(CAR_CONNECTION_STATE);
            if (carConnectionTypeColumn < 0) {
                Log.w(TAG, "Connection to car response is missing the connection type, treating as disconnected");
                notifyCarDisconnected();
                return;
            }
            if (!response.moveToNext()) {
                Log.w(TAG, "Connection to car response is empty, treating as disconnected");
                notifyCarDisconnected();
                return;
            }
            int connectionState = response.getInt(carConnectionTypeColumn);

            if (connectionState == CONNECTION_TYPE_NOT_CONNECTED) {
                Log.i(TAG, "Android Auto disconnected");
                notifyCarDisconnected();
            } else {
                Log.i(TAG, "Android Auto connected");
                Log.i(TAG, "onQueryComplete: " + connectionState);
                notifyCarConnected();
            }
        }
    }
}