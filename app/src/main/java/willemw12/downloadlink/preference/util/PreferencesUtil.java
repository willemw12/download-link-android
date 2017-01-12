package willemw12.downloadlink.preference.util;

import android.preference.Preference;
import android.preference.PreferenceManager;

/**
 * Updates preference summaries
 */
public class PreferencesUtil {

    public static Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference == null) {
                return false;
            }

            String stringValue = value.toString();
            //if (preference instanceof ListPreference) {
            //    ListPreference listPreference = (ListPreference) preference;
            //    int index = listPreference.findIndexOfValue(stringValue);
            //    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            //} else {
            preference.setSummary(stringValue);
            //}
            return true;
        }
    };

    public static void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null) {
            return;
        }

        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value
        bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }
}

