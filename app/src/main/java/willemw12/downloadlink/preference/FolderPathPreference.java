package willemw12.downloadlink.preference;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * A subclass of {@link EditTextPreference}.
 * However, the preference dialog is not displayed automatically, when the preference receives a click..
 * To display the preference dialog, call getDialog().show().
 */
public class FolderPathPreference extends EditTextPreference {

    //public static final String ROOT_PATH_SUMMARY = "";
    public static final String ROOT_PATH_SUMMARY = "(top folder)";

    public FolderPathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Hide the edit text dialog initially
        getDialog().hide();

        // Hide the keyboard initially
        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    // Hide the keyboard initially
    /** @hide */
    //@Override
    protected boolean needInputMethod() {
        // We want the input method to hide, when dialog is displayed
        return false;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Gray out root path summary hint
        boolean isRootFolder = ROOT_PATH_SUMMARY.equals(getSummary());
        ((TextView) view.findViewById(android.R.id.summary)).setEnabled(!isRootFolder);
    }
}

