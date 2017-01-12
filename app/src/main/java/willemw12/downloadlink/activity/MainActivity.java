package willemw12.downloadlink.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import willemw12.downloadlink.R;
import willemw12.downloadlink.preference.FolderPathPreference;
import willemw12.downloadlink.service.DownloadService;

import static willemw12.downloadlink.activity.SettingsActivity.PREF_DOWNLOAD_PATH_KEY;

/**
 * Displays the main settings screen
 */
//public class MainActivity extends AppCompatPreferenceActivity {
public class MainActivity extends AppCompatActivity {

    public static final String ACTION_EDIT_AND_DOWNLOAD = "action_edit_and_download_id";
    public static final String ACTION_PASTE_AND_DOWNLOAD = "action_paste_and_download_id";
    public static final String ACTION_SHARE_CLIPBOARD = "action_share_clipboard_id";

    public static final String INTENT_ACTION_APP_SHORTCUT = "willemw12.intent.action.APP_SHORTCUT";
    public static final String INTENT_ACTION_DOWNLOAD = "willemw12.intent.action.DOWNLOAD";
    public static final String INTENT_ACTION_NOTIFY = "willemw12.intent.action.NOTIFY";

    public static final char HORIZONTAL_ELLIPSIS_CHAR = '\u2026';
    public static final int MAX_LABEL_LENGTH = 128;

    // NOTE: adb logcat -s DownloadLink:* -s DownloadLinkService:* *:S
    //private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG = "DownloadLink";

    private static final String LINK_TEXT_DIALOG_FRAGMENT_TAG = "willemw12:LinkTextDialogFragment";

    private final int REQUEST_PICK_DIRECTORY = 1;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    // NOTE Or allocate objects in onCreate() ...
    private final List<String> linkTexts = new ArrayList<String>();

    // NOTE Cannot display the link, that is going to be shared, in a ShareActionProvider view
    //private ShareActionProvider shareActionProvider;
    //private final Intent shareIntent = new Intent();

    private MainPreferenceFragment mainPreferenceFragment;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    private String intentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mainPreferenceFragment = new MainPreferenceFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, mainPreferenceFragment)
                .commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        // Only show the FAB button when the app is launched normally (directly by the user)
        floatingActionButton.hide();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionEditAndDownload();
            }
        });

        // Handle preference dependency between two sets of preferences (pref_main.xml and pref_settings.xml)
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (PREF_DOWNLOAD_PATH_KEY.equals(key) || SettingsActivity.PREF_SHOW_FULL_DOWNLOAD_PATH_KEY.equals(key)) {
                    String pathSummary = getDownloadPathSummary(prefs);
                    Preference pref = mainPreferenceFragment.findPreference(PREF_DOWNLOAD_PATH_KEY);
                    if (pref != null) {
                        pref.setSummary(pathSummary);
                    }
                }
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        Intent intent = getIntent();
        String action = intent.getAction();
        String mimeType = intent.getType();

        intentAction = action;

        try {
            if (Intent.ACTION_MAIN.equals(intentAction) || INTENT_ACTION_APP_SHORTCUT.equals(intentAction)) {
                // Only show the FAB button when the app is launched normally (directly by the user)
                floatingActionButton.show();
            }
            if (Intent.ACTION_SEND.equals(action)) {
                // "Share" / "Send to" action

                Log.i(TAG, "Intent action: ACTION_SEND, Intent MIME type: " + mimeType);
                //if (mimeType == null) { return; }

                //if (!"text/plain".equals(mimeType)) { return; }

                //if (!shareTexts.isEmpty()) { ... }
                linkTexts.clear();
                String linkText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (linkText == null) {
                    linkText = intent.getStringExtra(Intent.EXTRA_STREAM);
                }
                if (linkText != null) {
                    linkTexts.add(linkText);
                }

                //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                boolean showSettings = sharedPrefs.getBoolean(SettingsActivity.PREF_SHOW_SETTINGS_KEY, true);
                if (showSettings) {
                    Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_INDEFINITE)
                            //.setAction("Continue download", new View.OnClickListener() {
                            .setAction(R.string.msg_yes, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    handleLinkTexts();
                                }
                            })
                            .setText(R.string.msg_continue_download)
                            .show();
                    //snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
                    //snackbar.show();
                } else {
                    // NOTE: It is not possible to hide the activity completely from the user.
                    //       Also, it is not allowed to share an intent directly to a service.
                    handleLinkTexts();
                }
                //return;

            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                // "Share" / "Send to" action

                Log.i(TAG, "Intent action: ACTION_SEND_MULTIPLE, Intent MIME type: " + mimeType);
                //if (mimeType == null) { return; }

                //if (!"text/plain".equals(mimeType)) { return; }

                //if (!shareTexts.isEmpty()) { ... }
                linkTexts.clear();
                ArrayList<String> stringArrayList = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
                if (stringArrayList != null) {
                    for (String linkText : stringArrayList) {
                        linkTexts.add(linkText);
                    }
                }
                if (linkTexts.isEmpty()) {
                    ArrayList<Parcelable> parcebleArrayList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (parcebleArrayList != null) {
                        for (Parcelable parcelable : parcebleArrayList) {
                            String linkText = parcelable.toString();
                            linkTexts.add(linkText);
                        }
                    }
                }
                handleLinkTexts();
                //return;

            //} else if (Intent.ACTION_VIEW.equals(action)) {
            //    // "Open with" action
            //
            //    Log.i(TAG, "Intent action: ACTION_VIEW, Intent MIME type: " + mimeType);
            //    //if (mimeType == null) { return; }
            //
            //    //if (!shareTexts.isEmpty()) { ... }
            //    linkTexts.clear();
            //    String linkText = intent.getDataString();
            //    //linkText = intent.getStringExtra(Intent.EXTRA_TEXT);
            //    //linkText = intent.getStringExtra(Intent.EXTRA_STREAM);
            //    if (linkText != null) {
            //        linkTexts.add(linkText);
            //    }
            //    handleLinkTexts();
            //    //return;

            } else if (INTENT_ACTION_APP_SHORTCUT.equals(action)) {
                // "App Shortcut" action

                String id = intent.getData().toString();
                //Log.i(TAG, "Intent action: " + action + ", Intent MIME type: " + mimeType + ", App shortcut id: " + id);
                Log.i(TAG, "Intent action: " + action + ", App shortcut id: " + id);

                if (ACTION_EDIT_AND_DOWNLOAD.equals(id)) {
                    actionEditAndDownload();
                } else if (ACTION_PASTE_AND_DOWNLOAD.equals(id)) {
                    actionPasteAndDownload();
                } else if (ACTION_SHARE_CLIPBOARD.equals(id)) {
                    actionShareClipboard();
                }
                //return;

            //} else if (Intent.ACTION_MAIN.equals(action)) {
            //    //return;
            }

        } catch (Exception exc) {
            Log.i(TAG, "Error: " + exc.getMessage());
            Toast.makeText(this, getString(R.string.msg_error) + ": " + exc.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            //return;
        }

        //Log.d(TAG, "onCreate: Unknown intent: '" + action + "', mimeType: '" + mimeType + "'");
        //throw new UnsupportedOperationException("Unknown intent: '" + action + "', mimeType: '" + mimeType + "'");
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "onPostCreate");
        super.onPostCreate(savedInstanceState);

        Preference pref = mainPreferenceFragment.findPreference(PREF_DOWNLOAD_PATH_KEY);
        if (pref != null) {
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    chooseDownloadLocation();
                    return true;
                }
            });
        }
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//
//        if (hasFocus) {
//            // NOTE: "findViewById(R.id.menu_main)" returns null
//            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
//
//            //// Enable clipboard paste menu actions (depending on MIME type)
//            //MenuItem item = ((Toolbar) findViewById(R.id.toolbar)).getMenu().findItem(R.id.action_paste_and_download);
//            //if (!cm.hasPrimaryClip()) {
//            //    //item.setEnabled(false);
//            //    item.setVisible(false);
//            //} else {
//            //    ClipDescription clipDescription = cm.getPrimaryClipDescription();
//            //    boolean hasMimeType = clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
//            //            clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) ||
//            //            clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) ||
//            //            clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT);
//            //    //item.setEnabled(hasMimeType);
//            //    item.setVisible(hasMimeType);
//            //}
//
//            // Enable clipboard paste menu actions (not depending on MIME type)
//            MenuItem item = ((Toolbar) findViewById(R.id.toolbar)).getMenu().findItem(R.id.action_share_clipboard);
//            item.setVisible(cm.hasPrimaryClip());
//            item = ((Toolbar) findViewById(R.id.toolbar)).getMenu().findItem(R.id.action_paste_and_download);
//            item.setVisible(cm.hasPrimaryClip());
//
//            // Display link in share list
//            //NOTE Overwrites linkTexts when receiving a download link (Intent.ACTION_SEND in onCreate())
//            ////extractLinksFromClipboard(true);
//            //
//            //String shareIntentLinkLabel = linkTexts.size() == 1 ? " '" + getLinkLabel(linkTexts.get(0)) + "'" : " links";
//            String shareIntentLinkLabel = linkTexts.size() == 1 ? " '" + getLinkLabel(linkTexts.get(0)) + "'" : "";
//            item.setTitle(String.format(getString(R.string.action_paste_and_download_title_format), shareIntentLinkLabel));
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.d(TAG, "onCreateOptionsMenu");

        // Inflate the menu. This adds items to the tool bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (Intent.ACTION_MAIN.equals(intentAction) || INTENT_ACTION_APP_SHORTCUT.equals(intentAction)) {
            // When the app is launched normally (directly by the user), this icon is moved to the FAB button
            // NOTE: "findViewById(R.id.menu_main)" returns null
            MenuItem item = ((Toolbar) findViewById(R.id.toolbar)).getMenu().findItem(R.id.action_edit_and_download);
            item.setVisible(false);

            //item = menu.findItem(R.id.action_share_clipboard);
            ////shareActionProvider = (ShareActionProvider) item.getActionProvider();
            //shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            ////shareIntent.setType("*/*");
            //shareIntent.setType("text/plain");
            //shareActionProvider.setShareIntent(shareIntent);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected");

        // Handle tool bar item clicks
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_edit_and_download) {
            actionEditAndDownload();
            return true;
        } else if (id == R.id.action_paste_and_download) {
            actionPasteAndDownload();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_share_clipboard) {
            actionShareClipboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Log.d(TAG, "onRequestPermissionsResult");

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permissions denied. Close (hide) activity
            Log.i(TAG, "Error: Permission denied");
            Toast.makeText(this, getString(R.string.msg_error) + ": " + getString(R.string.msg_permission_denied), Toast.LENGTH_LONG).show();
            finishActivity();
            return;
        }

        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                handleLinkTexts();
                return;
            default:
                Log.d(TAG, "onRequestPermissionsResult: Unknown requestCode: " + requestCode);
                break;
        }
    }

    @Override
    protected void onStop() {
        //Log.d(TAG, "onStop");
        super.onStop();

        intentAction = null;
    }

    @Override
    protected void onDestroy() {
        //Log.d(TAG, "onDestroy");
        super.onDestroy();

        if (onSharedPreferenceChangeListener != null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            onSharedPreferenceChangeListener = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MainPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            //setHasOptionsMenu(true);

            // Set initial value
            Preference pref = findPreference(PREF_DOWNLOAD_PATH_KEY);
            if (pref != null) {
                // NOTE: Don't enable the general listener for this preference.
                //       MainActivity.onSharedPreferenceChangeListener handles this preference specifically.
                // Bind the summary preferences to their values
                //PreferencesUtil.bindPreferenceSummaryToValue(pref);

                String pathSummary = getDownloadPathSummary(getPreferenceManager().getSharedPreferences());
                pref.setSummary(pathSummary);
            }

            ////Preference pref = findPreference(PREF_DOWNLOAD_PATH_KEY);
            ////pref.setSummary(...
            ////pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        }
    }

    private void finishActivity() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean prefCloseWhenFinished = sharedPrefs.getBoolean(SettingsActivity.PREF_CLOSE_WHEN_FINISHED_KEY, false);
        if (prefCloseWhenFinished  && !Intent.ACTION_MAIN.equals(intentAction)) {
            finish();
        }
    }


    // Handle settings

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.d(TAG, "onActivityResult");

        switch (requestCode) {
            case REQUEST_PICK_DIRECTORY:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        return;
                    }

                    Uri uri = data.getData();
                    if (uri == null) {
                        return;
                    }
                    if (!"com.android.externalstorage.documents".equals(uri.getAuthority())) {
                        Log.d(TAG, "onActivityResult: Not an external storage document");
                        return;
                    }

                    //String treeDocId;
                    //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    //    treeDocId = DocumentsContract.getTreeDocumentId(uri);
                    //} else {
                    //    treeDocId = uri.getLastPathSegment();
                    //}
                    String treeDocId = DocumentsContract.getTreeDocumentId(uri);
                    if (treeDocId == null) {
                        return;
                    }

                    String[] split = treeDocId.split(":");
                    String type = split[0];
                    String path = (split.length > 1) ? split[1] : "";

                    // NOTE: The Download Manager does not provide access to any secondary storage
                    if (!"primary".equalsIgnoreCase(type)) {
                        Log.i(TAG, String.format("Error: Storage type '%1$s' is not 'primary'", type));
                        Toast.makeText(this, String.format(getString(R.string.msg_unknown_doc_type), type), Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Store the preference summary
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PREF_DOWNLOAD_PATH_KEY, path).apply();

                    // Update the preference dialog's EditText input value
                    FolderPathPreference pref = (FolderPathPreference) mainPreferenceFragment.findPreference(PREF_DOWNLOAD_PATH_KEY);
                    if (pref != null) {
                        //pref.setText(path);
                        pref.getEditText().setText(path);
                    }

                    //// Update the preference summary view and the dialog edit text view by restarting the activity
                    ////recreate();

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    showTextEditDialog();
                    showTextEditDialog();
                }
                return;
            default:
                Log.d(TAG, "onActivityResult: Unknown requestCode: " + requestCode);
                break;
        }
    }

    private void chooseDownloadLocation() {
        //Log.d(TAG, "chooseDownloadLocation");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_PICK_DIRECTORY);
        } else {
            showTextEditDialog();
        }
    }

    private static String getDownloadPathSummary(SharedPreferences prefs) {
        boolean showFullPath = prefs.getBoolean(SettingsActivity.PREF_SHOW_FULL_DOWNLOAD_PATH_KEY, false);
        String pathName = prefs.getString(PREF_DOWNLOAD_PATH_KEY, SettingsActivity.PREF_DOWNLOAD_PATH_DEFAULT);
        String pathSummary = getDestinationInExternalPublicDir(pathName, showFullPath);
        if ("".equals(pathSummary)) {
            pathSummary = FolderPathPreference.ROOT_PATH_SUMMARY;
        }
        return pathSummary;
    }

    private static String getDestinationInExternalPublicDir(String path, boolean returnAbsolute) {
        if (path == null) {
            return null;
        }

        // NOTE: The download path is always relative to the external storage root
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (returnAbsolute) {
            String rootPath = Environment.getExternalStoragePublicDirectory("").toString();
            path = "".equals(path) ? rootPath : rootPath + "/" + path;
        }
        return path;
    }

    private void showTextEditDialog() {
        //Log.d(TAG, "showTextEditDialog");

        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //boolean hidePathEditText = sharedPrefs.getBoolean(SettingsActivity.PREF_HIDE_PATH_EDIT_TEXT_KEY , false);
        //if (! hidePathEditText) {
        FolderPathPreference pref = (FolderPathPreference) mainPreferenceFragment.findPreference(PREF_DOWNLOAD_PATH_KEY);
        if (pref != null) {
            pref.getDialog().show();
        }
        //}
    }


    // Handle menu actions

    // Truncate text (from the clipboard) to be displayed
    private static String getLinkLabel(String text) {
        if (text == null || text.length() <= MAX_LABEL_LENGTH) {
            return text;
        }
        //return text.substring(0, MAX_LABEL_LENGTH - 1) + HORIZONTAL_ELLIPSIS_CHAR;
        return text.substring(0, MAX_LABEL_LENGTH / 2 - 1) + HORIZONTAL_ELLIPSIS_CHAR + text.substring(text.length() - MAX_LABEL_LENGTH / 2);
    }

    private void extractLinksFromClipboard(boolean onlySingleTextLink) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (!cm.hasPrimaryClip()) {
                return;
            }
            ClipData clipData = cm.getPrimaryClip();
            int itemCount = clipData.getItemCount();

            linkTexts.clear();

            // Check clipboard texts
            for (int i = 0; i < itemCount; i++) {
                //String linkText = clipData.getItemAt(i).getText().toString();
                ClipData.Item item = clipData.getItemAt(i);
                if (item != null) {
                    // NOTE: Not "item.getHtmlText()"
                    CharSequence linkText = item.getText();
                    if (linkText != null) {
                        linkTexts.add(linkText.toString());
                        if (onlySingleTextLink && linkTexts.size() == 1) {
                            return;
                        }
                    }
                }
            }
            //if (linkTexts.isEmpty()) {
            //    // Check clipboard URIs
            //    ContentResolver cr = getContentResolver();
            //    for (int i = 0; i < itemCount; i++) {
            //        //String linkText = (String) clipData.getItemAt(i).getUri().toString();
            //        ClipData.Item item = clipData.getItemAt(i);
            //        if (item != null) {
            //            //String linkText = (String) item.getUri().toString();
            //            Uri uri = item.getUri();
            //            if (uri != null) {
            //                //String mimeType = cr.getType(uri);
            //                //if (mimeType == null || mimeType.equals(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            //                //
            //                //String scheme = uri.getScheme();
            //                //if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            //                //
            //                String linkText = (String) uri.toString();
            //                if (linkText != null) {
            //                    linkTexts.add(linkText);
            //                }
            //                //}
            //            }
            //        }
            //    }
            //}
            //if (linkTexts.size().isEmpty()) {
            //    // Check clippboard intents
            //    for (int i = 0; i < itemCount; i++) {
            //        //String linkText = (String) clipData.getItemAt(i).getIntent().toString();
            //        ClipData.Item item = clipData.getItemAt(i);
            //        if (item != null) {
            //            Intent intent = item.getIntent();
            //            String linkText = intent.getStringExtra(Intent.EXTRA_TEXT);
            //            if (linkText != null) {
            //                linkTexts.add(linkText);
            //                continue;
            //            }
            //            linkText = intent.getStringExtra(Intent.EXTRA_STREAM);
            //            if (linkText != null) {
            //                linkTexts.add(linkText);
            //                continue;
            //            }
            //            linkText = intent.getDataString();
            //            if (linkText != null) {
            //                linkTexts.add(linkText);
            //                continue;
            //            }
            //            ArrayList<String> stringArrayList = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
            //            if (stringArrayList != null) {
            //                for (String linkTextItem : stringArrayList) {
            //                    linkTexts.add(linkTextItem);
            //                }
            //                if (!linkTexts.isEmpty()) {
            //                    continue;
            //                }
            //            }
            //            ArrayList<Parcelable> parcebleArrayList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            //            if (parcebleArrayList != null) {
            //                for (Parcelable parcelable : parcebleArrayList) {
            //                    linkText = parcelable.toString();
            //                    linkTexts.add(linkText);
            //                }
            //            }
            //            //if (!linkTexts.isEmpty()) {
            //            //    continue;
            //            //}
            //        }
            //    }
            //}

        } catch (Exception exc) {
            Log.i(TAG, "Error: " + exc.getMessage());
            Toast.makeText(this, getString(R.string.msg_error) + ": " + exc.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            //throw exc;
        }
    }

    private void actionEditAndDownload() {
        DialogFragment dialog = new LinkTextDialogFragment();
        dialog.show(getFragmentManager(), LINK_TEXT_DIALOG_FRAGMENT_TAG);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            getSystemService(ShortcutManager.class).reportShortcutUsed(ACTION_EDIT_AND_DOWNLOAD);
        }
    }

    private void actionPasteAndDownload() {
        extractLinksFromClipboard(false);
        handleLinkTexts();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            getSystemService(ShortcutManager.class).reportShortcutUsed(ACTION_PASTE_AND_DOWNLOAD);
        }
    }

    private void actionShareClipboard() {
        //ALTERNATIVE Copy clipboard parcelables to chooser intent parcelables and set intent MIME type to "*/*"

        extractLinksFromClipboard(true);
        //handleShareClipboard();
        if (linkTexts.isEmpty()) {
            Log.i(TAG, "Error: No link text");
            Toast.makeText(this, getString(R.string.msg_error) + ": " + getString(R.string.msg_no_link_text), Toast.LENGTH_LONG).show();
        } else {
            Intent shareIntent = new Intent();
            //shareIntent.setType("*/*");
            shareIntent.setType("text/plain");

            String shareIntentLinkLabel;
            if (linkTexts.size() == 1) {
                // Display link in share list
                String linkText = linkTexts.get(0);
                Log.i(TAG, "Link text: " + linkText);
                shareIntentLinkLabel = " '" + getLinkLabel(linkText) + "'";

                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, linkText);
            } else /* if (linkTexts.size() > 1) */ {
                //shareIntentLinkLabel = " links";
                shareIntentLinkLabel = "";

                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                ArrayList<Uri> uriList = new ArrayList<Uri>();
                for (int i = 0; i < linkTexts.size(); i++) {
                    String linkText = linkTexts.get(i);
                    Log.i(TAG, "Link text: " + linkText);
                    uriList.add(Uri.parse(linkText));
                }
                //shareIntent.putParcelableArrayListExtra(Intent.EXTRA_TEXT, uriList);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
            }

            //// Only needs to be set when the intent content has changed
            //shareActionProvider.setShareIntent(shareIntent);

            startActivity(Intent.createChooser(shareIntent, String.format(getString(R.string.intent_share_clipboard_title), shareIntentLinkLabel)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                getSystemService(ShortcutManager.class).reportShortcutUsed(ACTION_SHARE_CLIPBOARD);
            }
        }

        // Close (hide) activity
        finishActivity();
    }

    public static class LinkTextDialogFragment extends DialogFragment {

        protected EditText editText;

        //public static LinkTextDialogFragment newInstance(int title) {
        //    LinkTextDialogFragment fragment = new LinkTextDialogFragment();
        //    Bundle args = new Bundle();
        //    args.putInt("title", title);
        //    fragment.setArguments(args);
        //    return fragment;
        //}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //int title = getArguments().getInt("title");

            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_edit_and_download, null);
            editText = (EditText) view.findViewById(R.id.dialog_link_text);

            //// NOTE: Show keyboard does not always work, depending on device and screen orientation
            /// Show the keyboard initially
            //ditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            //   @Override
            //   public void onFocusChange(View v, boolean hasFocus) {
            //       if (hasFocus) {
            //           getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            //       }
            //   }
            //});

            try {
                //ClipboardManager cm = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                if (cm.hasPrimaryClip()) {
                    ClipData clipData = cm.getPrimaryClip();
                    int itemCount = clipData.getItemCount();
                    if (itemCount > 0) {
                        //String linkText = clipData.getItemAt(0).getText().toString();
                        ClipData.Item item = clipData.getItemAt(0);
                        if (item != null) {
                            // NOTE: Not "item.getHtmlText().toString()"
                            CharSequence linkText = item.getText();
                            if (linkText != null) {
                                editText.setText(linkText.toString());
                            }
                        }
                    }
                }
            } catch (Exception exc) {
                Log.i(TAG, "Error: " + exc.getMessage());
                //Toast.makeText(getContext(), getString(R.string.msg_error) + ": " + exc.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Toast.makeText(getActivity(), getString(R.string.msg_error) + ": " + exc.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                //throw exc;
            }

            /// Show the keyboard initially
            // NOTE: Some devices always display the keyboard
            //if (!"".equals(editText.getText().toString())) {
            Editable text = editText.getText();
            if (text != null && !"".equals(text.toString())) {
                editText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(editText, 0);
                    }
                }, 50);
            }

            return new AlertDialog.Builder(getActivity())
                    //.setTitle(title)
                    .setTitle(getString(R.string.dialog_edit_link_title))
                    .setView(view)
                    .setPositiveButton(R.string.label_download,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // NOTE: Not "EditText editText = (EditText) getActivity().findViewById(R.id.dialog_link_text);"
                                    // NOTE: Not "EditText editText = (EditText) getView().findViewById(R.id.dialog_link_text);"
                                    EditText editText = (EditText) view.findViewById(R.id.dialog_link_text);
                                    CharSequence text = editText.getText();
                                    if (text != null) {
                                        String linkText = text.toString();
                                        if (!"".equals(linkText)) {
                                            //((MainActivity)getActivity()).handleLinkText(linkText);
                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.linkTexts.clear();
                                            activity.linkTexts.add(linkText);
                                            activity.handleLinkTexts();
                                        }
                                    }
                                }
                            }
                    )
                    .setNegativeButton(R.string.label_cancel,
                            //new DialogInterface.OnClickListener() {
                            //    public void onClick(DialogInterface dialog, int whichButton) {
                            //        ...
                            //    }
                            //}
                            null
                    )
                    .create();
        }
    }


    // Handle downloads

    private void handleLinkTexts() {
        //Log.d(TAG, "handleLinkText");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
            return;
        }

        //try {
        if (!linkTexts.isEmpty()) {
            for (String linkText : linkTexts) {
                downloadLink(linkText);
            }
        } else {
            // Display an error message to the user
            downloadLink(null);
        }
        //} catch (ExecutionException ignored) {
        //} finally {
        linkTexts.clear();

        // Close (hide) activity
        finishActivity();
        //}
    }

    private void downloadLink(String linkText) {
        //Log.d(TAG, "downloadLink");

        try {
            if (linkText == null) {
                Log.i(TAG, "Error: No link text");
                Toast.makeText(this, getString(R.string.msg_error) + ": " + getString(R.string.msg_no_link_text), Toast.LENGTH_LONG).show();
                return;
            }
            Log.i(TAG, "Link text: " + linkText);

            String mediaState = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(mediaState)) {
                Log.i(TAG, "Error: Primary shared media: " + mediaState);
                Toast.makeText(this, getString(R.string.msg_error) + ": " + getString(R.string.msg_primary_shared_media) + ": " + mediaState, Toast.LENGTH_LONG).show();
                //return;
            }

            Uri uri = Uri.parse(linkText);
            String fileName = uri.getLastPathSegment() != null ? uri.getLastPathSegment() : uri.getHost();
            //if (fileName == null) { ... }

            DownloadManager.Request request = new DownloadManager.Request(uri);
            //request.setVisibleInDownloadsUi(true);
            request.allowScanningByMediaScanner();
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            //String pathName = Environment.DIRECTORY_DOWNLOADS;
            String pathName = getDestinationInExternalPublicDir(sharedPrefs.getString(PREF_DOWNLOAD_PATH_KEY, SettingsActivity.PREF_DOWNLOAD_PATH_DEFAULT).trim(), false);
            request.setDestinationInExternalPublicDir(pathName, fileName);
            boolean allowOverRoaming = sharedPrefs.getBoolean(SettingsActivity.PREF_ALLOW_OVER_ROAMING_KEY, true);
            request.setAllowedOverRoaming(allowOverRoaming);
            boolean allowOverMetered = sharedPrefs.getBoolean(SettingsActivity.PREF_ALLOW_OVER_METERED_KEY, true);
            request.setAllowedOverMetered(allowOverMetered);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {    // Build.VERSION_CODES.NOUGAT
                boolean requiresCharging = sharedPrefs.getBoolean(SettingsActivity.PREF_REQUIRES_CHARGING_KEY, true);
                request.setRequiresCharging(requiresCharging);
                boolean requiresDeviceIdle = sharedPrefs.getBoolean(SettingsActivity.PREF_REQUIRES_DEVICE_IDLE_KEY, true);
                request.setRequiresDeviceIdle(requiresDeviceIdle);
            }
            boolean showNotifications = sharedPrefs.getBoolean(SettingsActivity.PREF_SHOW_NOTIFICATIONS_KEY, true);
            if (showNotifications) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            } else {
                //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            }

            DownloadManager dm = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = dm.enqueue(request);

            String fullFileName = "".equals(pathName) ? fileName : pathName + "/" + fileName;
            //Log.i(TAG, String.format("Downloading to '%1$s' on shared device storage", fullFileName));
            Log.i(TAG, String.format("Downloading to '%1$s'", fullFileName));
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.msg_downloading), fullFileName), Toast.LENGTH_LONG).show();

            boolean prefWifiLock = sharedPrefs.getBoolean(SettingsActivity.PREF_WIFI_LOCK_KEY, false);
            if (prefWifiLock) {
                lockWifi(downloadId);
            }

        } catch (Exception exc) {
            Log.i(TAG, "Error: " + exc.getMessage());
            Toast.makeText(this, getString(R.string.msg_error) + ": " + exc.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void lockWifi(long downloadId) {
        if (isConnectedToWifi()) {
            Intent intent = new Intent(INTENT_ACTION_DOWNLOAD, null, this, DownloadService.class);
            intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
            startService(intent);
        }
    }


    // Util

    public boolean isConnectedToWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && activeNetwork.isConnectedOrConnecting();
    }
}

