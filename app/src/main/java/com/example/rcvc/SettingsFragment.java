package com.example.rcvc;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences pref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // sets that text box doesn't autocorrect
        /*
        EditTextPreference hostURLedit = findPreference("host_url");
        EditTextPreference jitsiURLedit = findPreference("jitsi_url");
        if (hostURLedit != null) {
            hostURLedit.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        }
        if (jitsiURLedit != null) {
            jitsiURLedit.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        }

         */
        EditTextPreference port = findPreference("host_port");
        if (port != null) {
            port.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // Try if the preference is one of our custom Preferences
        DialogFragment dialogFragment = null;
        if (preference instanceof MotorPortDialogPreference) {
            // Create a new instance of TimePreferenceDialogFragment with the key of the related
            // Preference
            dialogFragment = MotorPortDialogFragment
                    .newInstance(preference.getKey());
        }

        // If it was one of our cutom Preferences, show its dialog
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getParentFragmentManager(),
                    "android.support.v7.preference" +
                            ".PreferenceFragment.DIALOG");
        }
        // Could not be handled here. Try with the super method.
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference p) {
        if (p.getKey().equals("host_url") || p.getKey().equals("jitsi_url")) {
            Log.d("treeclick", "poop " + p.getKey());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View dialogView = inflater.inflate(R.layout.preference_url, null);
            TextView textView = dialogView.findViewById(R.id.url_fragment_title);
            EditText editText = dialogView.findViewById(R.id.input_url);

            String defVal = "";
            if (p.getKey().equals("host_url")) {
                textView.setText(R.string.settings_title_host_url);
                defVal = "avatar.mintclub.org";
            } else {
                textView.setText(R.string.settings_title_jitsi_url);
                defVal = "meet.mintclub.org";
            }

            editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            String currentVal = pref.getString(p.getKey(), defVal);
            Log.d("treeclick", "a: " + currentVal);
            editText.setText(currentVal);

            builder.setView(dialogView)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        SharedPreferences.Editor editor = pref.edit();
                        String url = editText.getText().toString();
                        editor.putString(p.getKey(), url);
                        editor.apply();
                    })
                    .setNegativeButton(R.string.dialog_close, (dialog, which) -> {
                        dialog.cancel();
                    });

            AlertDialog d = builder.create();

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    handleText();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    handleText();
                }

                private void handleText() {
                    final Button okButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
                    String text = editText.getText().toString();
                    if (Patterns.WEB_URL.matcher(text).matches()) {
                        okButton.setEnabled(true);
                    } else {
                        okButton.setEnabled(false);
                    }
                }
            });

            d.show();
            //d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        return true;
    }
}
