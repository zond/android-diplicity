package se.oort.diplicity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Date;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.Ticker;
import se.oort.diplicity.apigen.TickerUnserializer;

public class GameUnserializer implements JsonDeserializer<Game> {
    private Gson baseGson;
    private RetrofitActivity retrofitActivity;
    public GameUnserializer(RetrofitActivity retrofitActivity) {
        this.retrofitActivity = retrofitActivity;
        baseGson = new GsonBuilder()
                .registerTypeAdapter(Ticker.class, new TickerUnserializer())
                .create();
    }
    private boolean alarmOn(Context context, Game game, Member member, Alarm.Alert alert) {
        if (game.Finished) {
            return false;
        }
        if (!game.Started) {
            return false;
        }
        if (member.NewestPhaseState == null) {
            return false;
        }
        if (member.NewestPhaseState.ReadyToResolve) {
            return false;
        }
        if (App.getDeadlineWarningMinutes(retrofitActivity) == 0) {
            return false;
        }
        if (alert.alertAt(context).getTime() < new Date().getTime()) {
            return false;
        }
        return true;
    }
    private void turnOnAlarm(Game game, Member member) {
        Alarm.Alert.fromGame(game, member).turnOn(retrofitActivity);
    }
    private void turnOffAlarm(Game game, Member member) {
        Alarm.Alert.fromGame(game, member).turnOff(retrofitActivity);
    }
    private void manageAlarms(Context context, Game game, Member member) {
        Alarm.Alert alert = Alarm.Alert.fromGame(game, member);
        if (alarmOn(context, game, member, alert)) {
            turnOnAlarm(game, member);
        } else {
            turnOffAlarm(game, member);
        }
    }
    public Game deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Game game = baseGson.fromJson(jsonObject, Game.class);
        Member member = retrofitActivity.getLoggedInMember(game);
        if (member != null && game.PhaseLengthMinutes > 0) {
            manageAlarms(retrofitActivity, game, member);
        }
        return game;
    }
}
