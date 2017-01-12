package willemw12.downloadlink.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import willemw12.downloadlink.R;
import willemw12.downloadlink.preference.util.PreferencesUtil;

/**
 * Displays the about screen
 */
public class AboutActivity extends AppCompatActivity {

    public static final String PREF_VERSION_KEY = "pref_version_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_about, new AboutPreferenceFragment())
                .commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // Show the Up/Left arrow button in the tool bar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        try {
            String versionName = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PREF_VERSION_KEY, versionName).apply();
        } catch (Exception ignored) {
        }
    }

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
    public static class AboutPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);

            // Bind the summary preferences to their values
            PreferencesUtil.bindPreferenceSummaryToValue(findPreference(PREF_VERSION_KEY));
        }
    }
}

