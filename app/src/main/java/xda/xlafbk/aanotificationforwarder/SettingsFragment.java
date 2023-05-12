package xda.xlafbk.aanotificationforwarder;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class SettingsFragment extends PreferenceFragmentCompat {
    TreeMap<String, String> installedApps;
    MultiSelectListPreference appsToForward;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        installedApps = getInstalledApps(requireContext());

        appsToForward = findPreference(getString(R.string.pref_appsToForward));
        appsToForward.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationForwarder.setAppsToForward((Set<String>)newValue);
            populateAppsToAutoDismissPref((Set<String>)newValue);
            return true;
        });
        setMultiSelectListPreferenceValues(appsToForward, installedApps);
        populateAppsToAutoDismissPref(appsToForward.getValues());
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
}