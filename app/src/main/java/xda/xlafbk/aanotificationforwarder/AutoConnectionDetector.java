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

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

// Source: https://stackoverflow.com/a/75292070
public class AutoConnectionDetector {
    private final Context context;
    private static final String TAG = "AutoConnectionDetector";
    private static final String CAR_CONNECTION_STATE = "CarConnectionState";
    private final CarConnectionBroadcastReceiver carConnectionReceiver = new CarConnectionBroadcastReceiver();
    private final CarConnectionQueryHandler carConnectionQueryHandler;
    private static final int CONNECTION_TYPE_NOT_CONNECTED = 0;
    private static final int QUERY_TOKEN = 42;
    private static final String CAR_CONNECTION_AUTHORITY = "androidx.car.app.connection";
    // auto app on your phone will send broadcast with this action when connection state changes
    private final String ACTION_CAR_CONNECTION_UPDATED = "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED";
    private static final Uri PROJECTION_HOST_URI = new Uri.Builder().scheme("content").authority(CAR_CONNECTION_AUTHORITY).build();

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

    public void registerCarConnectionReceiver() {
        ContextCompat.registerReceiver(context, carConnectionReceiver, new IntentFilter(ACTION_CAR_CONNECTION_UPDATED), ContextCompat.RECEIVER_EXPORTED);
        queryForState();
        Log.i(TAG, "registerCarConnectionReceiver: ");
    }

    public void unRegisterCarConnectionReceiver() {
        context.unregisterReceiver(carConnectionReceiver);
        Log.i(TAG, "unRegisterCarConnectionReceiver: ");
    }

    public AutoConnectionDetector(Context context) {
        this.context = context;
        carConnectionQueryHandler = new CarConnectionQueryHandler(context.getContentResolver());
    }

    public void queryForState() {
        carConnectionQueryHandler.startQuery(QUERY_TOKEN, null, PROJECTION_HOST_URI, new String[]{CAR_CONNECTION_STATE}, null, null, null);
        Log.i(TAG, "queryForState");
    }

    static void notifyCarConnected() {
        if (listeners != null) listeners.forEach(OnCarConnectionStateListener::onCarConnected);
    }

    static void notifyCarDisconnected() {
        if (listeners != null) listeners.forEach(OnCarConnectionStateListener::onCarDisconnected);
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
                Log.w(TAG, "Null response from content provider, treating as disconnected");
                notifyCarDisconnected();
                return;
            }
            int col = response.getColumnIndex(CAR_CONNECTION_STATE);
            if (col < 0 || !response.moveToNext()) {
                Log.w(TAG, "Invalid response from content provider, treating as disconnected");
                notifyCarDisconnected();
                return;
            }
            if (response.getInt(col) == CONNECTION_TYPE_NOT_CONNECTED) {
                Log.i(TAG, "Android Auto disconnected");
                notifyCarDisconnected();
            } else {
                Log.i(TAG, "Android Auto connected");
                notifyCarConnected();
            }
        }
    }
}
