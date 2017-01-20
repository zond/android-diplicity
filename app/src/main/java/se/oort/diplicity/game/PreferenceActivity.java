package se.oort.diplicity.game;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import se.oort.diplicity.BuildConfig;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.apigen.FCMToken;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserConfig;

public class PreferenceActivity extends RetrofitActivity {

    public static final String GAME_ID_INTENT_KEY = "game_id_intent_key";

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Fragment()).commit();
    }

    public static class Fragment extends PreferenceFragment {

        private RetrofitActivity retrofitActivity() {
            return (RetrofitActivity) getActivity();
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.game_preferences);
            Preference gameIDPref = (Preference) findPreference(getResources().getString(R.string.game_id_pref_key));
            gameIDPref.setSummary(getActivity().getIntent().getStringExtra(GAME_ID_INTENT_KEY));
            gameIDPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(getResources().getString(R.string.game_id), getActivity().getIntent().getStringExtra(GAME_ID_INTENT_KEY));
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), R.string.game_id_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

    }
}
