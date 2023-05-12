package xda.xlafbk.aanotificationforwarder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public final static String channelIdImportant = "aaNotificationForwarderImportant";
    private AutoConnectionDetector.OnCarConnectionStateListener autoConnectionListener;
    private AutoConnectionDetector autoDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        super.onCreate(savedInstanceState);

        // GUI
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
        try {
            ((TextView)findViewById(R.id.versioninfo)).setText(getString(R.string.about_text, context.getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
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
        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName())) {
            findViewById(R.id.permissionError).setVisibility(View.VISIBLE);
            findViewById(R.id.permissionError).setOnClickListener(v -> startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        } else {
            findViewById(R.id.permissionError).setVisibility(View.GONE);
        }
    }

    private class AutoConnectionMain implements AutoConnectionDetector.OnCarConnectionStateListener {
        @Override
        public void onCarConnected() {
            updateView(true);
        }

        @Override
        public void onCarDisconnected() {
            updateView(false);
        }
    }
    
    private void updateView(boolean isConnected) {
        findViewById(R.id.connected).setVisibility(isConnected ? View.VISIBLE : View.GONE);
        findViewById(R.id.disconnected).setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }
}
