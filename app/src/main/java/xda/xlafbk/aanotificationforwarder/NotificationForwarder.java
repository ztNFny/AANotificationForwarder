package xda.xlafbk.aanotificationforwarder;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
    private static Set<String> ignoreNotificationTitle = new HashSet<>();
    private static boolean debugLogging;

    private static boolean forwardWithoutAndroidAuto;
    private static boolean ignoreGroupSummaryNotifications;
    private Context context;
    private AutoConnection autoConnectionListener;
    private final String TAG = "AANotificationForwarder";

    @Override
    public void onCreate() {
        context = getApplicationContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        appsToForward = preferences.getStringSet(context.getString(R.string.pref_appsToForward), new HashSet<>());
        appsToDismiss = preferences.getStringSet(context.getString(R.string.pref_appsAutoDismiss), new HashSet<>());
        ignoreNotificationTitle = Set.of(preferences.getString(context.getString(R.string.pref_ignoreNotificationTitle), "").split(","));
        ignoreGroupSummaryNotifications = preferences.getBoolean(context.getString(R.string.pref_ignoreGroupSummaryNotifications), getResources().getBoolean(R.bool.pref_default_ignoreGroupSummaryNotifications));
        forwardWithoutAndroidAuto = preferences.getBoolean(context.getString(R.string.pref_forwardWithoutAndroidAuto), getResources().getBoolean(R.bool.pref_default_forwardWithoutAndroidAuto));
        debugLogging = preferences.getBoolean(context.getString(R.string.pref_debugLogging), getResources().getBoolean(R.bool.pref_default_debugLogging));

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

    public static void setIgnoreNotificationTitle(String newValue) {
        ignoreNotificationTitle = Set.of(newValue.split(","));
    }

    public static void setDebugLogging(boolean newValue) {
        debugLogging = newValue;
    }

    public static void setForwardWithoutAndroidAuto(boolean newValue) {
        forwardWithoutAndroidAuto = newValue;
    }

    public static void setIgnoreGroupSummaryNotifications(boolean newValue) {
        ignoreGroupSummaryNotifications = newValue;
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
        if (!autoConnectionListener.isConnected() && !forwardWithoutAndroidAuto) {
            if (debugLogging) Log.d(TAG, "Ignoring notification as Android Auto is not connected");
            return;
        }
        // FLAG_GROUP_SUMMARY notifications
        if (ignoreGroupSummaryNotifications && (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            if (debugLogging) Log.d(TAG, "Ignoring FLAG_GROUP_SUMMARY notification");
            return;
        }
        // Package whitelisted?
        if (!appsToForward.contains(sbn.getPackageName())) {
            if (debugLogging) Log.d(TAG, "Ignoring notification from non-forwarded app");
            return;
        }
        // Has this already been forwarded?
        String sbnId = sbn.getKey() + "_" + sbn.getNotification().when;
        if (forwardedNotifications.contains(sbnId)) {
            if (debugLogging) Log.d(TAG, "Already notified");
            return;
        }
        forwardedNotifications.add(sbnId);
        if (debugLogging) Log.d(TAG, "Notification will be forwarded: " + sbnId);

        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;

        String title = bundle.getCharSequence(Notification.EXTRA_TITLE, "").toString();
        String text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT, "").toString();
        if ("".equals(text)) {
            text = bundle.getCharSequence(Notification.EXTRA_TEXT, "").toString();
        }

        if (debugLogging) {
            for (String b : new String[] { Notification.EXTRA_SUB_TEXT, Notification.EXTRA_SUMMARY_TEXT, Notification.EXTRA_TEMPLATE, Notification.EXTRA_TEXT, Notification.EXTRA_TEXT_LINES, Notification.EXTRA_TITLE, Notification.EXTRA_TITLE_BIG, Notification.EXTRA_VERIFICATION_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_INFO_TEXT }) {
                Log.d(TAG, "Bundle content - \"" + b + "\" : \"" + bundle.getCharSequence(b, "<null>").toString() + "\"");
            }
        }

        if ("".equals(title) && "".equals(text)) {
            if (debugLogging) Log.d(TAG, "No text or title in notification");
            return;
        }

        for (String ignoreString : ignoreNotificationTitle) {
            if (!ignoreString.isBlank() && title.contains(ignoreString)) {
                if (debugLogging) Log.d(TAG, "Ignore notification as it contains \"" + ignoreString + "\"");
                return;
            }
        }

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
