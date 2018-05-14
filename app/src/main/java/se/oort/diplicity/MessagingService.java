package se.oort.diplicity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.Ticker;
import se.oort.diplicity.apigen.TickerUnserializer;

// MessagingService receives messages from FCM when the app is turned on as the message
// arrives.
public class MessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private Handler handler = new Handler();

    public static final String FCM_NOTIFY_ACTION = "se.oort.diplicity.FCMNotify";

    public static Set<RetrofitActivity> messageSubscribers = new AbstractSet<RetrofitActivity>() {
        private Map<RetrofitActivity, Object> backend = new ConcurrentHashMap<RetrofitActivity, Object>();
        @Override
        public boolean add(RetrofitActivity i) {
            boolean rval = backend.containsKey(i);
            backend.put(i, new Object());
            return rval;
        }
        @Override
        public boolean remove(Object i) {
            boolean rval = backend.containsKey(i);
            backend.remove(i);
            return rval;
        }
        @Override
        public Iterator<RetrofitActivity> iterator() {
            return backend.keySet().iterator();
        }
        @Override
        public int size() {
            return backend.size();
        }
    };

    public static class DiplicityJSON {
        public String type;
        public se.oort.diplicity.apigen.Message message;
        public PhaseMeta phaseMeta;
        public String gameID;
    }

    public static DiplicityJSON decodeDataPayload(String diplicityJSON) {
        if (diplicityJSON == null) {
            return null;
        }
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
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                DiplicityJSON diplicityJSON = decodeDataPayload(remoteMessage.getData().get("DiplicityJSON"));
                Log.d("Diplicity", "Received a " + diplicityJSON.type);
                if (diplicityJSON != null) {
                    for (RetrofitActivity subscriber : messageSubscribers) {
                        boolean consumed = subscriber.consumeDiplicityJSON(diplicityJSON);
                        if (consumed) {
                            Log.d("Diplicity", "" + subscriber + " consumed this notification");
                            return;
                        } else {
                            Log.d("Diplicity", "" + subscriber + " didn't consume this notification, checking if anyone else wants to");
                        }
                    }
                    Log.d("Diplicity", "Nobody consumed this message, popping up a regular notification");

                    Intent intent = new Intent(FCM_NOTIFY_ACTION);
                    intent.putExtra(NotificationReceiveActivity.DIPLICITY_JSON_EXTRA, remoteMessage.getData().get("diplicityJSON"));

                    NotificationReceiveActivity.sendNotification(
                            MessagingService.this,
                            intent,
                            remoteMessage.getNotification().getTitle(),
                            remoteMessage.getNotification().getBody());
                }
            }
        });
    }

}
