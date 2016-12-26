package se.oort.diplicity;

import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.os.Bundle;

import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserConfig;

public class PreferenceActivity extends RetrofitActivity {

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
        public void onResume() {
            super.onResume();
            retrofitActivity().handleReq(
                    retrofitActivity().userConfigService.UserConfigLoad(App.loggedInUser.Id),
                    new Sendable<SingleContainer<UserConfig>>() {
                        @Override
                        public void send(final SingleContainer<UserConfig> userConfigSingleContainer) {
                            final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

                            final CheckBoxPreference emailPreference = (CheckBoxPreference) findPreference("email_notifications");
                            emailPreference.setChecked(userConfigSingleContainer.Properties.MailConfig.Enabled);
                            emailPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    userConfigSingleContainer.Properties.MailConfig.Enabled = (Boolean) o;
                                    retrofitActivity().handleReq(
                                            retrofitActivity().userConfigService.UserConfigUpdate(userConfigSingleContainer.Properties, App.loggedInUser.Id),
                                            new Sendable<SingleContainer<UserConfig>>() {
                                                @Override
                                                public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                                }
                                            },
                                            getResources().getString(R.string.updating_settings));
                                    return true;
                                }
                            });

                            final EditTextPreference fakeIDPref = (EditTextPreference) findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID);
                            fakeIDPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    fakeIDPref.setSummary((String) o);
                                    return true;
                                }
                            });

                            final CheckBoxPreference localDevPreference = (CheckBoxPreference) findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE);
                            localDevPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    if ((Boolean) o) {
                                        prefs.edit().putString(RetrofitActivity.API_URL_KEY, RetrofitActivity.LOCAL_DEVELOPMENT_URL).apply();
                                        final EditTextPreference fakeIDPref = new EditTextPreference(getActivity());
                                        fakeIDPref.setKey(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID);
                                        fakeIDPref.setTitle(getActivity().getResources().getString(R.string.local_development_fake_id));
                                        fakeIDPref.setSummary(prefs.getString(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID, ""));
                                        fakeIDPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                            @Override
                                            public boolean onPreferenceChange(Preference preference, Object o) {
                                                fakeIDPref.setSummary((String) o);
                                                return true;
                                            }
                                        });
                                        ((PreferenceCategory) findPreference("development_prefs")).addPreference(fakeIDPref);
                                    } else {
                                        prefs.edit().putString(RetrofitActivity.API_URL_KEY, RetrofitActivity.DEFAULT_URL).apply();
                                        removeFakeIDPref();
                                    }
                                    return true;
                                }
                            });


                            if (!prefs.getBoolean(RetrofitActivity.LOCAL_DEVELOPMENT_MODE, false)) {
                                removeFakeIDPref();
                            } else {
                                findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID)
                                        .setSummary(prefs.getString(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID, ""));
                            }
                        }
                    }, getResources().getString(R.string.loading_settings));
        }

        private void removeFakeIDPref() {
            Preference fakeIDPref = findPreference(RetrofitActivity.LOCAL_DEVELOPMENT_MODE_FAKE_ID);
            if (fakeIDPref != null) {
                ((PreferenceCategory) getPreferenceScreen().findPreference("development_prefs")).removePreference(fakeIDPref);
            }
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

    }
}
