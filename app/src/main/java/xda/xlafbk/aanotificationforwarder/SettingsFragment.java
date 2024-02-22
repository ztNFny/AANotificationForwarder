package xda.xlafbk.aanotificationforwarder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class SettingsFragment extends PreferenceFragmentCompat {
    TreeMap<String, String> installedApps;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        CheckBoxPreference statusNotificationAccess = findPreference(getString(R.string.pref_status_notificationaccess));
        statusNotificationAccess.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
                preference.setIcon(R.drawable.notification);
                preference.setSummary(R.string.status_notificationaccess_true);
                preference.setSelectable(false);
            } else {
                preference.setIcon(R.drawable.notification_important);
                preference.setSummary(R.string.status_notificationaccess_false);
                preference.setSelectable(true);
                preference.setOnPreferenceClickListener(v -> {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    return true;
                });
            }
            return true;
        });

        CheckBoxPreference statusConnection = findPreference(getString(R.string.pref_status_connection));
        statusConnection.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
                preference.setIcon(R.drawable.car_ok);
                preference.setSummary(R.string.status_connection_true);
            } else {
                preference.setIcon(R.drawable.car);
                preference.setSummary(R.string.status_connection_false);
            }
            return true;
        });

        installedApps = getInstalledApps(requireContext());

        MultiSelectListPreference appsToForward = findPreference(getString(R.string.pref_appsToForward));
        appsToForward.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setAppsToForward((Set<String>)newValue);
            populateAppsToAutoDismissPref((Set<String>)newValue);
            return true;
        });
        setMultiSelectListPreferenceValues(appsToForward, installedApps);
        populateAppsToAutoDismissPref(appsToForward.getValues());

        EditTextPreference ignoreNotificationTitle = findPreference(getString(R.string.pref_ignoreNotificationTitle));
        ignoreNotificationTitle.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setIgnoreNotificationTitle((String) newValue);
            return true;
        });
        CheckBoxPreference ignoreGroupSummaryNotifications = findPreference(getString(R.string.pref_ignoreGroupSummaryNotifications));
        ignoreGroupSummaryNotifications.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setIgnoreGroupSummaryNotifications((Boolean) newValue);
            return true;
        });

        CheckBoxPreference forwardWithoutAndroidAuto = findPreference(getString(R.string.pref_forwardWithoutAndroidAuto));
        forwardWithoutAndroidAuto.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setForwardWithoutAndroidAuto((Boolean) newValue);
            return true;
        });
        CheckBoxPreference debugLogging = findPreference(getString(R.string.pref_debugLogging));
        debugLogging.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setDebugLogging((Boolean) newValue);
            return true;
        });
    }

    private void populateAppsToAutoDismissPref(Set<String> values) {
        MultiSelectListPreference appsToAutoDismiss = findPreference(getString(R.string.pref_appsAutoDismiss));
        TreeMap<String, String> selectedApps = getAppTitlesForPackageNames(installedApps, values);
        appsToAutoDismiss.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setAppsToDismiss((Set<String>)newValue);
            return true;
        });
        setMultiSelectListPreferenceValues(appsToAutoDismiss, selectedApps);
    }

    private void setMultiSelectListPreferenceValues(MultiSelectListPreference preference, TreeMap<String, String> map) {
        preference.setEntries(map.keySet().toArray(new CharSequence[0])); // Package Names
        preference.setEntryValues(map.values().toArray(new CharSequence[0])); // App Titles
    }

    private static TreeMap<String, String> getInstalledApps(Context context) {
        List<PackageInfo> apps = context.getPackageManager().getInstalledPackages(0);
        PackageManager packageManager = context.getApplicationContext().getPackageManager();

        TreeMap<String, String> appsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        apps.forEach((app) -> {
            String label = packageManager.getApplicationLabel(app.applicationInfo).toString();
            if (!label.startsWith(app.packageName)) {
                appsMap.put(label, app.packageName);
            }
        });
        return appsMap;
    }

    private static TreeMap<String, String> getAppTitlesForPackageNames(TreeMap<String, String> installedApps, Set<String> appPckNames) {
        TreeMap<String, String> appsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        installedApps.forEach((label, pckName) -> {
            if (appPckNames.contains(pckName)) {
                appsMap.put(label, pckName);
            }
        });
        return appsMap;
    }

    protected void updateCheckboxPreference(int id, boolean newValue) {
        CheckBoxPreference statusConnection = findPreference(getString(id));
        statusConnection.setChecked(newValue);
        statusConnection.callChangeListener(newValue);
    }
}