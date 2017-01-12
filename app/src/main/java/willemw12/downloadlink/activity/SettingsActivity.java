package willemw12.downloadlink.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import willemw12.downloadlink.R;

/**
 * Displays the settings screen
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREF_ALLOW_OVER_METERED_KEY = "pref_allow_over_metered_key";
    public static final String PREF_ALLOW_OVER_ROAMING_KEY = "pref_allow_over_roaming_key";
    public static final String PREF_CLOSE_WHEN_FINISHED_KEY = "pref_close_when_finished_key";
    public static final String PREF_DOWNLOAD_PATH_DEFAULT = "Download";
    public static final String PREF_DOWNLOAD_PATH_KEY = "pref_download_path_key";
    //public static final String PREF_HIDE_PATH_EDIT_TEXT_KEY = "pref_hide_path_edit_text_key";
    public static final String PREF_REQUIRES_CHARGING_KEY = "pref_requires_charging_key";
    public static final String PREF_REQUIRES_DEVICE_IDLE_KEY = "pref_requires_device_idle_key";
    public static final String PREF_SHOW_FULL_DOWNLOAD_PATH_KEY = "pref_show_full_download_path_key";
    public static final String PREF_SHOW_NOTIFICATIONS_KEY = "pref_show_notifications_key";
    public static final String PREF_SHOW_SETTINGS_KEY = "pref_show_settings_key";
    public static final String PREF_WIFI_LOCK_KEY = "pref_wifi_lock_key";

    private SettingsPreferenceFragment settingsPreferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        settingsPreferenceFragment = new SettingsPreferenceFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_settings, new SettingsPreferenceFragment())
                .commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // Show the Up/Left arrow button in the tool bar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // NOTE: findPreference() returns null.
    //@Override
    //protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    //    //Log.d(TAG, "onPostCreate");
    //    super.onPostCreate(savedInstanceState);
    //
    //    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
    //        settingsPreferenceFragment.findPreference(PREF_REQUIRES_CHARGING_KEY).setEnabled(false);
    //        settingsPreferenceFragment.findPreference(PREF_REQUIRES_DEVICE_IDLE_KEY).setEnabled(false);
    //    }
    //}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Clicked Up/Left arrow button in the tool bar
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Preference pref = findPreference(PREF_REQUIRES_CHARGING_KEY);
                if (pref != null) {
                    pref.setEnabled(false);
                }
                pref = findPreference(PREF_REQUIRES_DEVICE_IDLE_KEY);
                if (pref != null) {
                    pref.setEnabled(false);
                }
            }
        }
    }
}

