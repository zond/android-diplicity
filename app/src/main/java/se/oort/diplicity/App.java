package se.oort.diplicity;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.os.Build;


import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.CrashlyticsInitProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.fabric.sdk.android.services.common.Crash;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;

public class App extends MultiDexApplication {

    public static final StringBuffer errorLog = new StringBuffer();

    public App() {
        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                    Log.wtf("Diplicity", paramThrowable.getMessage(), paramThrowable);
                    System.exit(2); //Prevents the service/app from freezing
                }
            });
        }
    }

    public static String millisToDuration(long millis) {
        return minutesToDuration((int) ((millis / (long) 1000) / (long) 60));
    }

    public static long getDeadlineWarningMinutes(Context context) {
        try {
            return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.deadline_warning_minutes_key), "60"));
        } catch (NumberFormatException e) {
            return 0l;
        }
    }

    public static boolean getDeadlineWarningDebug(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.deadline_warning_debug_pref_key), false);
    }

    public static Member getMemberByUser(Game game, String userId) {
        for (Member m : game.Members) {
            if (m.User.Id.equals(userId)) {
                return m;
            }
        }
        return null;
    }

    public static Member getMemberByNation(Game game, String nation) {
        for (Member m : game.Members) {
            if (m.Nation.equals(nation)) {
                return m;
            }
        }
        return null;
    }

    public static void firebaseCrashReport(String msg) {
        errorLog.append(new Date().toString() + ": " + msg + "\n");
        if (!BuildConfig.DEBUG) {
            Crashlytics.log(msg);
        }
        Log.e("Diplicity", msg);
    }

    public static void firebaseCrashReport(String msg, Throwable e) {
        errorLog.append(new Date().toString() + ": " + msg + "\n" + e.getMessage() + "\n");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.flush();
        printWriter.close();
        errorLog.append(stringWriter.toString() + "\n");
        if (!BuildConfig.DEBUG) {
            Crashlytics.log(msg);
            Crashlytics.logException(e);
        }
        Log.e("Diplicity", msg, e);
    }

    public static String minutesToDuration(int mins) {
        long days = mins / (60 * 24);
        long hours = (mins - (60 * 24 * days)) / 60;
        long minutes = mins - (60 * 24 * days) - (60 * hours);
        List<String> timeLabelList = new ArrayList<String>();
        if (days > 0) {
            timeLabelList.add("" + days + "d");
        }
        if (hours > 0) {
            timeLabelList.add("" + hours + "h");
        }
        if (minutes > 0) {
            timeLabelList.add("" + minutes + "m");
        }
        StringBuilder timeLabel = new StringBuilder();
        for (int i = 0; i < timeLabelList.size(); i++) {
            timeLabel.append(timeLabelList.get(i));
            if (i < timeLabelList.size() - 1) {
                timeLabel.append(", ");
            }
        }
        return timeLabel.toString();
    }

}
