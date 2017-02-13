package se.oort.diplicity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

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
    private boolean alarmOff(Game game, Member member) {
        if (game.Finished) {
            return true;
        }
        if (!game.Started) {
            return true;
        }
        if (member.NewestPhaseState == null) {
            return true;
        }
        if (member.NewestPhaseState.ReadyToResolve) {
            return true;
        }
        return false;
    }
    private void turnOnAlarm(Game game) {

    }
    private void turnOffAlarm(Game game) {

    }
    private void manageAlarms(Game game, Member member) {
        if (alarmOff(game, member)) {
            turnOffAlarm(game);
        } else {
            turnOnAlarm(game);
        }
    }
    public Game deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Game game = baseGson.fromJson(jsonObject, Game.class);
        Member member = null;
        for (Member m : game.Members) {
            if (m.User.Id.equals(retrofitActivity.getLoggedInUser().Id)) {
                member = m;
                break;
            }
        }
        if (member != null && game.PhaseLengthMinutes > 0) {
            manageAlarms(game, member);
        }
        return game;
    }
}
