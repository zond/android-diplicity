package se.oort.diplicity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.Ticker;
import se.oort.diplicity.apigen.TickerUnserializer;

public class MessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    public static Set<RetrofitActivity> messageSubscribers = Collections.synchronizedSet(new HashSet<RetrofitActivity>());

    public static class DiplicityJSON {
        String type;
        se.oort.diplicity.apigen.Message message;
        PhaseMeta phaseMeta;
        String gameID;
    }

    public static DiplicityJSON decodeDataPayload(String diplicityJSON) {
        try {
            byte[] compressedJSON = Base64.decode(diplicityJSON, Base64.DEFAULT);

            Inflater decompresser = new Inflater();
            decompresser.setInput(compressedJSON, 0, compressedJSON.length);

            byte[] result = new byte[8192];
            int resultLength = decompresser.inflate(result);
            decompresser.end();

            byte[] actualResult = Arrays.copyOfRange(result, 0, resultLength);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Ticker.class, new TickerUnserializer())
                    .create();

            return gson.fromJson(new String(actualResult, "UTF-8"), DiplicityJSON.class);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeExecutionException(e);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("Diplicity", "MessagingService called with " + decodeDataPayload(remoteMessage.getData().get("DiplicityJSON")));
    }

}
