package se.oort.diplicity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class PreferenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Fragment()).commit();
    }

    public class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onResume() {
            super.onResume();
            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                    for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                        Preference singlePref = preferenceGroup.getPreference(j);
                        updatePreference(singlePref, singlePref.getKey());
                    }
                } else {
                    updatePreference(preference, preference.getKey());
                }
            }
            if (!((CheckBoxPreference) findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE)).isChecked() &&
                    findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID) != null) {
                getPreferenceScreen().removePreference(findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID));
            }
        }


        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreference(findPreference(key), key);
            if (key.equals(RetrofitActivity.LOCAL_DEVELOPMENT_MODE)) {
                if (sharedPreferences.getBoolean(RetrofitActivity.LOCAL_DEVELOPMENT_MODE, false)) {
                    sharedPreferences.edit().putString(RetrofitActivity.API_URL_KEY, RetrofitActivity.LOCAL_DEVELOPMENT_URL).apply();
                    EditTextPreference fakeIDPref = new EditTextPreference(getActivity());
                    fakeIDPref.setKey(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID);
                    fakeIDPref.setTitle(PreferenceActivity.this.getResources().getString(R.string.local_development_fake_id));
                    fakeIDPref.setSummary(sharedPreferences.getString(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID, ""));
                    getPreferenceScreen().addPreference(fakeIDPref);
                } else {
                    sharedPreferences.edit().putString(RetrofitActivity.API_URL_KEY, RetrofitActivity.DEFAULT_URL).apply();
                    if (findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID) != null) {
                        getPreferenceScreen().removePreference(findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID));
                    }
                }
            }
        }

        private void updatePreference(Preference preference, String key) {
            if (preference == null) return;
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
                return;
            }
            SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
            if (!(preference instanceof CheckBoxPreference)) {
                preference.setSummary(sharedPrefs.getString(key, ""));
            }
        }
    }
}
