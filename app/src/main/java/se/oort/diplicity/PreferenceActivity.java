package se.oort.diplicity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;

import se.oort.diplicity.apigen.FCMNotificationConfig;
import se.oort.diplicity.apigen.FCMToken;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserConfig;

public class PreferenceActivity extends RetrofitActivity {

    public static String FCM_REPLACE_TOKEN_KEY = "fcm_replace_token";
    public static String APP_NAME = "android-diplicity";

    private static SecureRandom random = new SecureRandom();

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
                    retrofitActivity().userConfigService.UserConfigLoad(retrofitActivity().getLoggedInUser().Id),
                    new Sendable<SingleContainer<UserConfig>>() {
                        private FCMToken getToken(UserConfig config) {
                            if (config.FCMTokens == null) {
                                return null;
                            }
                            FCMToken pushToken = null;
                            for (FCMToken fcmToken : config.FCMTokens) {
                                if (APP_NAME.equals(fcmToken.App)) {
                                    pushToken = fcmToken;
                                    break;
                                }
                            }
                            return pushToken;
                        }
                        @Override
                        public void send(final SingleContainer<UserConfig> userConfigSingleContainer) {
                            final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

                            final CheckBoxPreference emailPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.email_notifications_pref_key));
                            emailPreference.setChecked(userConfigSingleContainer.Properties.MailConfig.Enabled);
                            emailPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    userConfigSingleContainer.Properties.MailConfig.Enabled = (Boolean) o;
                                    retrofitActivity().handleReq(
                                            retrofitActivity().userConfigService.UserConfigUpdate(userConfigSingleContainer.Properties, retrofitActivity().getLoggedInUser().Id),
                                            new Sendable<SingleContainer<UserConfig>>() {
                                                @Override
                                                public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                                }
                                            },
                                            getResources().getString(R.string.updating_settings));
                                    return true;
                                }
                            });

                            final CheckBoxPreference pushPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.push_notifications_pref_key));
                            if (FirebaseInstanceId.getInstance().getToken() != null) {
                                FCMToken pushToken = getToken(userConfigSingleContainer.Properties);
                                pushPreference.setChecked(pushToken != null && !pushToken.Disabled);
                                pushPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                    @Override
                                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                                        FCMToken pushToken = getToken(userConfigSingleContainer.Properties);
                                        if ((Boolean) newValue) {
                                            if (pushToken == null) {
                                                pushToken = new FCMToken();
                                                pushToken.Note = "Created by user action at " + new Date();
                                                if (userConfigSingleContainer.Properties.FCMTokens == null) {
                                                    userConfigSingleContainer.Properties.FCMTokens = new ArrayList<FCMToken>();
                                                }
                                                userConfigSingleContainer.Properties.FCMTokens.add(pushToken);
                                            } else if (pushToken.Disabled) {
                                                pushToken.Note = "Enabled by user action at " + new Date();
                                            }
                                            pushToken.Disabled = false;
                                            pushToken.Value = FirebaseInstanceId.getInstance().getToken();
                                            pushToken.App = APP_NAME;
                                            pushToken.ReplaceToken = new BigInteger(8 * 24, random).toString(32);
                                            pushToken.MessageConfig = new FCMNotificationConfig();
                                            pushToken.MessageConfig.ClickActionTemplate = MessagingService.FCM_NOTIFY_ACTION;
                                            pushToken.PhaseConfig = new FCMNotificationConfig();
                                            pushToken.PhaseConfig.ClickActionTemplate = MessagingService.FCM_NOTIFY_ACTION;
                                        } else {
                                            if (pushToken != null && (pushToken.Disabled == null || !pushToken.Disabled)) {
                                                pushToken.Disabled = true;
                                                pushToken.Note = "Disabled by user action at " + new Date();
                                            }
                                        }
                                        if (pushToken != null) {
                                            final FCMToken finalToken = pushToken;
                                            retrofitActivity().handleReq(
                                                    retrofitActivity().userConfigService.UserConfigUpdate(userConfigSingleContainer.Properties, retrofitActivity().getLoggedInUser().Id),
                                                    new Sendable<SingleContainer<UserConfig>>() {
                                                        @Override
                                                        public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(FCM_REPLACE_TOKEN_KEY, finalToken.ReplaceToken).apply();
                                                        }
                                                    }, getResources().getString(R.string.updating_settings));
                                        }
                                        return true;
                                    }
                                });
                                pushPreference.setEnabled(true);
                            } else {
                                pushPreference.setEnabled(false);
                                pushPreference.setSummary(getResources().getString(R.string.dysfunctional_fcm_service));
                            }

                            final EditTextPreference fakeIDPref = (EditTextPreference) findPreference(getResources().getString(R.string.local_development_mode_fake_id_pref_key));
                            final Preference.OnPreferenceChangeListener fakeIDChanged = new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    fakeIDPref.setSummary((String) o);
                                    PreferenceManager.getDefaultSharedPreferences(retrofitActivity()).edit().putString(AUTH_TOKEN_PREF_KEY, "").apply();
                                    retrofitActivity().performLogin();
                                    return true;
                                }
                            };
                            fakeIDPref.setOnPreferenceChangeListener(fakeIDChanged);

                            final CheckBoxPreference localDevPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.local_development_mode_pref_key));
                            localDevPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object o) {
                                    if ((Boolean) o) {
                                        prefs.edit().putString(RetrofitActivity.API_URL_PREF_KEY, RetrofitActivity.LOCAL_DEVELOPMENT_URL).apply();
                                        final EditTextPreference fakeIDPref = new EditTextPreference(getActivity());
                                        fakeIDPref.setKey(getResources().getString(R.string.local_development_mode_fake_id_pref_key));
                                        fakeIDPref.setTitle(getActivity().getResources().getString(R.string.local_development_fake_id));
                                        fakeIDPref.setSummary(prefs.getString(getResources().getString(R.string.local_development_mode_fake_id_pref_key), ""));
                                        fakeIDPref.setOnPreferenceChangeListener(fakeIDChanged);
                                        ((PreferenceCategory) findPreference("development_prefs")).addPreference(fakeIDPref);
                                    } else {
                                        prefs.edit().putString(RetrofitActivity.API_URL_PREF_KEY, RetrofitActivity.DEFAULT_URL).apply();
                                        removeFakeIDPref();
                                    }
                                    retrofitActivity().performLogin();
                                    return true;
                                }
                            });


                            if (!prefs.getBoolean(getResources().getString(R.string.local_development_mode_pref_key), false)) {
                                removeFakeIDPref();
                            } else {
                                findPreference(getResources().getString(R.string.local_development_mode_fake_id_pref_key))
                                        .setSummary(prefs.getString(getResources().getString(R.string.local_development_mode_fake_id_pref_key), ""));
                            }
                        }
                    }, getResources().getString(R.string.loading_settings));
        }

        private void removeFakeIDPref() {
            Preference fakeIDPref = findPreference(getResources().getString(R.string.local_development_mode_fake_id_pref_key));
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
