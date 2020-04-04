package se.oort.diplicity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;

import static se.oort.diplicity.R.menu.game;

public class Alarm extends BroadcastReceiver {

    public static final String GAME_ID_KEY = "game_id";
    public static final String GAME_DESC_KEY = "game_desc";
    public static final String GAME_DEADLINE_KEY = "game_deadline";
    public static final String DEADLINE_WARNING_ACTION = "se.oort.diplicity.DeadlineWarning";
    public static final String ALARM_PREFERENCES = "alarm_preferences";

    public static class Alert {
        Date deadlineAt;
        String desc;
        String id;
        public Date alertAt(Context context) {
            if (App.getDeadlineWarningDebug(context)) {
                return new Date(new Date().getTime() + 5 * 1000);
            } else {
                return new Date(deadlineAt.getTime() - (App.getDeadlineWarningMinutes(context) * 60 * 1000));
            }
        }
        public static Alert fromJSON(String s) {
            return new Gson().fromJson(s, Alert.class);
        }
        public static Alert fromGame(Game game, Member member) {
            Alert alert = new Alert();
            alert.deadlineAt = game.NewestPhaseMeta.get(0).NextDeadlineIn.deadlineAt();
            alert.desc = game.Desc;
            if (member.GameAlias != null && !member.GameAlias.equals("")) {
                alert.desc = member.GameAlias;
            }
            alert.id = game.ID;
            return alert;
        }
        public static Alert fromIntent(Intent intent) {
            Alert alert = new Alert();
            alert.deadlineAt = new Date(intent.getLongExtra(GAME_DEADLINE_KEY, 0));
            alert.desc = intent.getStringExtra(GAME_DESC_KEY);
            alert.id = intent.getStringExtra(GAME_ID_KEY);
            return alert;
        }
        public String toJSON() {
            return new Gson().toJson(this);
        }
        public String toString() {
            return "Alarm for " + desc + "/" + id + " with deadline at " + deadlineAt;
        }
        public Intent toIntent(Context context) {
            Intent intent = new Intent(context, Alarm.class);
            // Action and data are only set to allow the alarm manager to cancel other alarms for the same game.
            intent.setAction(Alarm.DEADLINE_WARNING_ACTION);
            intent.setData(Uri.parse("https://diplicity-engine.appspot.com/Game/" + id));
            // Extras are easier to extract, and will be used for the notification.
            intent.putExtra(Alarm.GAME_ID_KEY, id);
            intent.putExtra(Alarm.GAME_DESC_KEY, desc);
            intent.putExtra(Alarm.GAME_DEADLINE_KEY, deadlineAt.getTime());
            return intent;
        }
        public void turnOn(Context context) {
            long alertIn = alertAt(context).getTime() - new Date().getTime();
            if (alertIn > 0) {
                AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, toIntent(context), 0);

                alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + alertIn, alarmIntent);
                Alarm.getAlarmPreferences(context).edit().putString(id, toJSON()).apply();
                Log.d("Diplicity", "Turned on " + toString() + ", at " + new Date(new Date().getTime() + alertIn));
            }
        }
        public void turnOff(Context context) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, toIntent(context), 0);
            alarmMgr.cancel(alarmIntent);
            getAlarmPreferences(context).edit().remove(id).apply();
            Log.d("Diplicity", "Turned off " + toString());
        }
        public void notify(Context context) {
            Intent notificationIntent = new Intent(DEADLINE_WARNING_ACTION);
            notificationIntent.putExtra(GAME_ID_KEY, id);
            NotificationReceiveActivity.sendNotification(context,
                    notificationIntent,
                    context.getResources().getString(R.string.orders_needed),
                    context.getResources().getString(R.string.x_needs_orders_before_y, desc, deadlineAt.toString()));
            Log.d("Diplicity", "Created notification for " + this);

        }
    }

    public static void resetAllAlarms(Context context) {
        for (Object json : getAlarmPreferences(context).getAll().values()) {
            Alert alert = Alert.fromJSON("" + json);
            if (alert.alertAt(context).getTime() > new Date().getTime()) {
                alert.turnOn(context);
            } else {
                alert.turnOff(context);
            }
        }
    }

    public static SharedPreferences getAlarmPreferences(Context context) {
        return context.getSharedPreferences(ALARM_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Alert alert = Alert.fromIntent(intent);
        alert.notify(context);
        alert.turnOff(context);
    }
}
