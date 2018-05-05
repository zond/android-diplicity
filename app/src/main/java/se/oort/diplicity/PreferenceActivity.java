package se.oort.diplicity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Single;
import se.oort.diplicity.apigen.FCMNotificationConfig;
import se.oort.diplicity.apigen.FCMToken;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserConfig;

public class PreferenceActivity extends RetrofitActivity {

    private static class FragmentAndConfig {
        Fragment fragment;
        SingleContainer<UserConfig> config;
        public FragmentAndConfig(Fragment f, SingleContainer<UserConfig> c) {
            this.fragment = f;
            this.config = c;
        }
    }

    private static Pattern COLOR_REG = Pattern.compile("^(\\w+/)?(\\w+/)?(#[a-fA-F0-9]{6,6}|[a-fA-F0-9]{8,8})$");
    private static Map<Context, FragmentAndConfig> configs = new ConcurrentHashMap<>();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Fragment()).commit();
    }

    public void colorsFromClipboard(final View v) {
        final FragmentAndConfig f = configs.get(v.getContext());
        if (f != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                if (clip.getItemCount() > 0) {
                    f.config.Properties.Colors = new ArrayList<String>(Arrays.asList(clip.getItemAt(0).coerceToText(v.getContext()).toString().split("\n")));
                    f.fragment.retrofitActivity().handleReq(
                            f.fragment.retrofitActivity().userConfigService.UserConfigUpdate(f.config.Properties, f.fragment.retrofitActivity().getLoggedInUser().Id),
                            new Sendable<SingleContainer<UserConfig>>() {
                                @Override
                                public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                    Toast.makeText(v.getContext(), R.string.colors_copied_from_clipboard, Toast.LENGTH_LONG).show();
                                    f.fragment.populateColorOverrides(f.config);
                                }
                            },
                            getResources().getString(R.string.updating_settings));
                } else {
                    Toast.makeText(v.getContext(), R.string.clipboard_is_empty, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(v.getContext(), R.string.clipboard_is_empty, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void colorsToClipboard(View v) {
        FragmentAndConfig f = configs.get(v.getContext());
        if (f != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getResources().getString(R.string.color_scheme), TextUtils.join("\n", f.config.Properties.Colors));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(v.getContext(), R.string.colors_copied_to_clipboard, Toast.LENGTH_LONG).show();

        }
    }

    public static class Fragment extends PreferenceFragment {

        protected RetrofitActivity retrofitActivity() {
            return (RetrofitActivity) getActivity();
        }

        @Override
        public void onPause() {
            configs.remove(retrofitActivity());
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (retrofitActivity().getLoggedInUser() != null) {
                retrofitActivity().handleReq(
                        retrofitActivity().userConfigService.UserConfigLoad(retrofitActivity().getLoggedInUser().Id),
                        new Sendable<SingleContainer<UserConfig>>() {
                            @Override
                            public void send(final SingleContainer<UserConfig> userConfigSingleContainer) {
                                configs.put(retrofitActivity(), new FragmentAndConfig(Fragment.this, userConfigSingleContainer));

                                final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

                                findPreference(getResources().getString(R.string.app_version_pref_key)).setSummary("" + BuildConfig.VERSION_CODE);

                                final CheckBoxPreference emailPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.email_notifications_pref_key));
                                emailPreference.setChecked(userConfigSingleContainer.Properties.MailConfig.Enabled);
                                emailPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                    @Override
                                    public boolean onPreferenceChange(Preference preference, Object o) {
                                        if (retrofitActivity().getLoggedInUser() != null) {
                                            userConfigSingleContainer.Properties.MailConfig.Enabled = (Boolean) o;
                                            retrofitActivity().handleReq(
                                                    retrofitActivity().userConfigService.UserConfigUpdate(userConfigSingleContainer.Properties, retrofitActivity().getLoggedInUser().Id),
                                                    new Sendable<SingleContainer<UserConfig>>() {
                                                        @Override
                                                        public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                                        }
                                                    },
                                                    getResources().getString(R.string.updating_settings));
                                        }
                                        return true;
                                    }
                                });

                                final CheckBoxPreference pushPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.push_notifications_pref_key));
                                if (FirebaseInstanceId.getInstance().getToken() != null) {
                                    FCMToken pushToken = retrofitActivity().getFCMToken(userConfigSingleContainer.Properties);
                                    pushPreference.setChecked(pushToken != null && !pushToken.Disabled);
                                    pushPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                        @Override
                                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                                            retrofitActivity().updateFCMPushOption(userConfigSingleContainer.Properties, (Boolean) newValue, "User action");
                                            return true;
                                        }
                                    });
                                    pushPreference.setEnabled(true);
                                } else {
                                    pushPreference.setEnabled(false);
                                    pushPreference.setSummary(getResources().getString(R.string.dysfunctional_fcm_service));
                                }

                                final EditTextPreference deadlineWarningPref = (EditTextPreference) findPreference(getResources().getString(R.string.deadline_warning_minutes_key));
                                deadlineWarningPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                    @Override
                                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                                        try {
                                            long value = Long.parseLong("" + newValue);
                                            if (value < 0) {
                                                throw new NumberFormatException();
                                            }
                                            prefs.edit().putString(getResources().getString(R.string.deadline_warning_minutes_key), "" + value).apply();
                                            deadlineWarningPref.setSummary("" + newValue);
                                            Alarm.resetAllAlarms(retrofitActivity());
                                            return true;
                                        } catch (NumberFormatException e) {
                                            Toast.makeText(retrofitActivity(), getResources().getString(R.string.must_be_positive_integer), Toast.LENGTH_SHORT).show();
                                            return false;
                                        }
                                    }
                                });
                                deadlineWarningPref.setSummary("" + App.getDeadlineWarningMinutes(retrofitActivity()));

                                final EditTextPreference fakeIDPref = (EditTextPreference) findPreference(getResources().getString(R.string.local_development_mode_fake_id_pref_key));
                                final Preference.OnPreferenceChangeListener fakeIDChanged = new Preference.OnPreferenceChangeListener() {
                                    @Override
                                    public boolean onPreferenceChange(Preference preference, Object o) {
                                        fakeIDPref.setSummary((String) o);
                                        prefs.edit().putString(AUTH_TOKEN_PREF_KEY, "").apply();
                                        retrofitActivity().performLogin();
                                        return true;
                                    }
                                };
                                if (fakeIDPref != null) {
                                    fakeIDPref.setOnPreferenceChangeListener(fakeIDChanged);
                                }

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
                                final CheckBoxPreference zoomButtonPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.zoom_buttons_pref_key));
                                zoomButtonPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                    @Override
                                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                                        if ((Boolean) newValue) {
                                            prefs.edit().putBoolean(RetrofitActivity.ZOOM_BUTTONS_PREF_KEY, true).apply();
                                        } else {
                                            prefs.edit().putBoolean(RetrofitActivity.ZOOM_BUTTONS_PREF_KEY, false).apply();
                                        }
                                        return true;
                                    }
                                });

                                populateColorOverrides(userConfigSingleContainer);



                                if (!prefs.getBoolean(getResources().getString(R.string.local_development_mode_pref_key), false)) {
                                    removeFakeIDPref();
                                } else {
                                    findPreference(getResources().getString(R.string.local_development_mode_fake_id_pref_key))
                                            .setSummary(prefs.getString(getResources().getString(R.string.local_development_mode_fake_id_pref_key), ""));
                                }
                            }
                        }, getResources().getString(R.string.loading_settings));
            }
        }

        protected void populateColorOverrides(final SingleContainer<UserConfig> configContainer) {
            UserConfig config = configContainer.Properties;
            PreferenceCategory parent = ((PreferenceCategory) findPreference("interface_prefs"));
            Set<Preference> toRemove = new HashSet<Preference>();
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                if (parent.getPreference(i).getKey().indexOf("colorOverride") == 0) {
                    toRemove.add(parent.getPreference(i));
                }
            }
            for (Preference p : toRemove) {
                parent.removePreference(p);
            }
            if (config.Colors == null) {
                config.Colors = new ArrayList<String>();
            }
            for (int i = 0; i < config.Colors.size(); i++) {
                addColorOverride(configContainer, i);
            }
            addColorOverride(configContainer, config.Colors.size());
        }

        private void addColorOverride(final SingleContainer<UserConfig> configContainer, final int i) {
            final UserConfig config = configContainer.Properties;

            String initialValue = "";
            int initialColor = -1;
            if (i < config.Colors.size()) {
                Matcher matcher = COLOR_REG.matcher(config.Colors.get(i));
                if (matcher.find()) {
                    initialValue = config.Colors.get(i);
                    try {
                        initialColor = Color.parseColor(matcher.group(3));
                    } catch (IllegalArgumentException e) {
                    }
                }
            }

            final int finalColor = initialColor;
            final EditTextPreference colorPref = new EditTextPreference(getActivity()) {
                @Override
                protected View onCreateView(ViewGroup parent) {
                    View rval = super.onCreateView(parent);
                    if (finalColor != -1) {
                        rval.setBackgroundColor(finalColor);
                    }
                    return rval;
                }
            };
            final Preference.OnPreferenceChangeListener colorChanged = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String newColor = (String) o;
                    if (!newColor.equals("")) {
                        if (!COLOR_REG.matcher(newColor).find()) {
                            if (newColor.indexOf("#") != 0) {
                                newColor = "#" + newColor;
                            }
                            if (!COLOR_REG.matcher(newColor).find()) {
                                Toast.makeText(retrofitActivity(), R.string.colors_must_be_valid_hex_codes, Toast.LENGTH_LONG).show();
                                return false;
                            }
                        }
                    }
                    boolean update = false;
                    if (i < config.Colors.size()) {
                        if (newColor.equals("")) {
                            config.Colors.remove(i);
                        } else {
                            config.Colors.set(i, newColor);
                        }
                        update = true;
                    } else if (!newColor.equals("")) {
                        config.Colors.add(newColor);
                        update = true;
                    }
                    if (update) {
                        retrofitActivity().handleReq(
                                retrofitActivity().userConfigService.UserConfigUpdate(config, retrofitActivity().getLoggedInUser().Id),
                                new Sendable<SingleContainer<UserConfig>>() {
                                    @Override
                                    public void send(SingleContainer<UserConfig> userConfigSingleContainer) {
                                        configContainer.Properties = userConfigSingleContainer.Properties;
                                        populateColorOverrides(configContainer);
                                    }
                                },
                                getResources().getString(R.string.updating_settings));
                    }
                    return false;
                }
            };
            colorPref.setKey("colorOverride" + i);
            if (i < config.Colors.size()) {
                colorPref.setTitle(getActivity().getResources().getString(R.string.color_override));
            } else {
                colorPref.setTitle(getActivity().getResources().getString(R.string.new_color_override));
            }
            colorPref.setSummary(initialValue);
            colorPref.setDefaultValue(initialValue);
            colorPref.setPersistent(false);
            colorPref.setOnPreferenceChangeListener(colorChanged);
            ((PreferenceCategory) findPreference("interface_prefs")).addPreference(colorPref);
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
