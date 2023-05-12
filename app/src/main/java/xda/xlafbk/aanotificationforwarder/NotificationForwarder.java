package xda.xlafbk.aanotificationforwarder;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class NotificationForwarder extends NotificationListenerService {
    private final List<String> forwardedNotifications = new ArrayList<>(10);
    private static Set<String> appsToForward = new HashSet<>();
    private static Set<String> appsToDismiss = new HashSet<>();
    private Context context;
    private AutoConnection autoConnectionListener;

    @Override
    public void onCreate() {
        context = getApplicationContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        appsToForward = preferences.getStringSet(context.getString(R.string.pref_appsToForward), new HashSet<>());
        appsToDismiss = preferences.getStringSet(context.getString(R.string.pref_appsAutoDismiss), new HashSet<>());

        // subscribe to Android Auto connection state
        autoConnectionListener = new AutoConnection();
        AutoConnectionDetector autoDetector = new AutoConnectionDetector(this);
        autoDetector.setListener(autoConnectionListener);
        autoDetector.registerCarConnectionReceiver();
    }

    public static void setAppsToForward(Set<String> newValue) {
        appsToForward = newValue;
    }

    public static void setAppsToDismiss(Set<String> newValue) {
        appsToDismiss = newValue;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // connected to Android Auto?
        if (!autoConnectionListener.isConnected()) {
            return;
        }
        // Package whitelisted?
        if (!appsToForward.contains(sbn.getPackageName())) {
            return;
        }
        // Has this already been forwarded?
        String sbnId = sbn.getKey() + sbn.getNotification().when;
        if (forwardedNotifications.contains(sbnId)) {
            return;
        }
        forwardedNotifications.add(sbnId);

        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;

        String title = bundle.getCharSequence("android.title").toString();
        String text = bundle.containsKey("android.bigText") ? bundle.getCharSequence("android.bigText").toString() : bundle.getCharSequence("android.text", "").toString();

        // Get the best notification icon (large, small, default) and return it as bitmap
        Bitmap notificationIcon = NotificationHelper.getNotificationIconBitmap(context, notification);

        NotificationHelper.sendCarNotification(context, title, text, null, notificationIcon, new Random().nextInt(100000));

        // cancel the original apps notification
        if (appsToDismiss.contains(sbn.getPackageName())) {
            cancelNotification(sbn.getKey());
        }
    }

    public static class AutoConnection implements AutoConnectionDetector.OnCarConnectionStateListener {
        private boolean isConnected = false;

        @Override
        public void onCarConnected() {
            isConnected = true;
        }

        @Override
        public void onCarDisconnected() {
            isConnected = false;
        }

        public boolean isConnected() {
            return isConnected;
        }
    }
}
