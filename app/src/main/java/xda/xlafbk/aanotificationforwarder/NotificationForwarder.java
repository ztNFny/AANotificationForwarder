package xda.xlafbk.aanotificationforwarder;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.preference.PreferenceManager;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private long appStartTime;
    private AutoConnection autoConnectionListener;
    private FileLogger logger;

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

        // init logger
        logger = new FileLogger(context, debugLogging);
        
        // subscribe to Android Auto connection state
        autoConnectionListener = new AutoConnection();
        AutoConnectionDetector autoDetector = new AutoConnectionDetector(this);
        autoDetector.setListener(autoConnectionListener);
        autoDetector.registerCarConnectionReceiver();
        appStartTime = System.currentTimeMillis();
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
        logger.log("onListenerConnected");
        logger.log("Apps to forward: " + String.join(",", appsToForward));
        logger.log("Apps to dismiss: " + String.join(",", appsToDismiss));
        logger.log("Ignored Titles: " + String.join(",", ignoreNotificationTitle));
        super.onListenerConnected();
    }

    @Override
    public void onListenerDisconnected() {
        logger.log("onListenerDisconnected");
        super.onListenerDisconnected();
    }

    public String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return format.format(date);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;

        logger.log("================");
        //for (String b : new String[] { Notification.EXTRA_SUB_TEXT, Notification.EXTRA_SUMMARY_TEXT, Notification.EXTRA_TEMPLATE, Notification.EXTRA_TEXT, Notification.EXTRA_TEXT_LINES, Notification.EXTRA_TITLE, Notification.EXTRA_TITLE_BIG, Notification.EXTRA_VERIFICATION_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_INFO_TEXT }) {
        for (String b : new String[] { Notification.EXTRA_TITLE, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_TEXT }) {
            if (null != bundle.getCharSequence(b, null)) {
                logger.log("Bundle content - \"%s\" : \"%s\"", b, bundle.getCharSequence(b, "<null>").toString());
            }
        }
        logger.log("When: %s", convertTime(notification.when));
        logger.log("getPostTime: %s", convertTime(sbn.getPostTime()));
        logger.log("getPackageName: %s", sbn.getPackageName());
        logger.log("appStartTime: %s", convertTime(appStartTime));
        logger.log("getAaConnectionEstablishedTimestamp: %s", convertTime(autoConnectionListener.getAaConnectionEstablishedTimestamp()));

        // Has this already been forwarded?
        String sbnId = sbn.getKey() + "_" + sbn.getNotification().when;

        if (notification.when < appStartTime) {
            logger.log("Message posted before connection");
            return;
        }
        if (autoConnectionListener.getAaConnectionEstablishedTimestamp() > sbn.getNotification().when) {
            logger.log("Message posted before connection2");
            return;
        }
        if (forwardedNotifications.contains(sbnId)) {
            logger.log("Already notified");
            return;
        }
        forwardedNotifications.add(sbnId);
        // connected to Android Auto?
        if (!autoConnectionListener.isConnected() && !forwardWithoutAndroidAuto) {
            logger.log("Ignoring notification as Android Auto is not connected");
            return;
        }
        // FLAG_GROUP_SUMMARY notifications
        if (ignoreGroupSummaryNotifications && (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            logger.log("Ignoring FLAG_GROUP_SUMMARY notification");
            return;
        }
        // Package whitelisted?
        if (!appsToForward.contains(sbn.getPackageName())) {
            logger.log("Ignoring notification from non-forwarded app (%s)", sbn.getPackageName());
            return;
        }
        logger.log("Notification will be forwarded: %s", sbnId);


        String title = bundle.getCharSequence(Notification.EXTRA_TITLE, "").toString();
        String text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT, "").toString();
        if (text.isEmpty()) {
            text = bundle.getCharSequence(Notification.EXTRA_TEXT, "").toString();
        }

        if (title.isEmpty() && text.isEmpty()) {
            logger.log("No text or title in notification");
            return;
        }

        for (String ignoreString : ignoreNotificationTitle) {
            if (!ignoreString.isBlank() && title.contains(ignoreString)) {
                logger.log("Ignore notification as it contains \"%s\"" , ignoreString);
                return;
            }
        }

        // Get the best notification icon (large, small, default) and return it as bitmap
        Bitmap notificationIcon = NotificationHelper.getNotificationIconBitmap(context, notification);
        logger.log("Forwarding notification");

        NotificationHelper.sendCarNotification(context, title, text, null, notificationIcon, new Random().nextInt(100000));

        // cancel the original apps notification
        if (appsToDismiss.contains(sbn.getPackageName())) {
            cancelNotification(sbn.getKey());
        }
    }

    public static class AutoConnection implements AutoConnectionDetector.OnCarConnectionStateListener {
        private boolean isConnected = false;

        private long aaConnectionEstablishedTimestamp = 2000000000000L;

        @Override
        public void onCarConnected() {
            isConnected = true;
            aaConnectionEstablishedTimestamp = System.currentTimeMillis();
        }

        @Override
        public void onCarDisconnected() {
            isConnected = false;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public long getAaConnectionEstablishedTimestamp() {
            return aaConnectionEstablishedTimestamp;
        }
    }
}
