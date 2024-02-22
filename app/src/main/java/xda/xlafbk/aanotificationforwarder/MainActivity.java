package xda.xlafbk.aanotificationforwarder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public final static String channelIdImportant = "aaNotificationForwarderImportant";
    private AutoConnectionDetector.OnCarConnectionStateListener autoConnectionListener;
    private AutoConnectionDetector autoDetector;
    private SettingsFragment sf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        super.onCreate(savedInstanceState);

        // GUI
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (savedInstanceState == null) {
            sf = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);
        }
        try {
            sf.findPreference(getString(R.string.pref_about_version)).setSummary(context.getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // version info isn't that important
        }

        // FAB
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> NotificationHelper.sendCarNotification(view.getContext(), getString(R.string.test_notification_title), getString(R.string.test_notification_message), null, null, new Random().nextInt(100)));

        checkPermissions();

        // Prepare notification channels
        NotificationChannel notificationChannel = new NotificationChannel(channelIdImportant, "Forwarded Notifications - Important", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        // subscribe to Android Auto connection state
        autoConnectionListener = new AutoConnectionMain();
        autoDetector = new AutoConnectionDetector(this);
        autoDetector.setListener(autoConnectionListener);
        autoDetector.registerCarConnectionReceiver();
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoDetector.removeListener(autoConnectionListener);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        }
        sf.updateCheckboxPreference(R.string.pref_status_notificationaccess, NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName()));
    }

    private class AutoConnectionMain implements AutoConnectionDetector.OnCarConnectionStateListener {
        @Override
        public void onCarConnected() {
            sf.updateCheckboxPreference(R.string.pref_status_connection, true);
        }

        @Override
        public void onCarDisconnected() {
            sf.updateCheckboxPreference(R.string.pref_status_connection, false);
        }
    }
}
