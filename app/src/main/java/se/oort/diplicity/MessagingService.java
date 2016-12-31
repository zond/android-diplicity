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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RunnableFuture;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.Ticker;
import se.oort.diplicity.apigen.TickerUnserializer;

public class MessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private Handler handler = new Handler();

    public static final String FCM_NOTIFY_ACTION = "se.oort.diplicity.FCMNotify";

    public static Set<RetrofitActivity> messageSubscribers = Collections.synchronizedSet(new HashSet<RetrofitActivity>());

    public static class DiplicityJSON {
        public String type;
        public se.oort.diplicity.apigen.Message message;
        public PhaseMeta phaseMeta;
        public String gameID;
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
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                DiplicityJSON diplicityJSON = decodeDataPayload(remoteMessage.getData().get("DiplicityJSON"));
                Log.d("Diplicity", "Received a " + diplicityJSON.type);
                for (RetrofitActivity subscriber : messageSubscribers) {
                    boolean consumed = subscriber.consumeDiplicityJSON(diplicityJSON);
                    if (consumed) {
                        Log.d("Diplicity", "" + subscriber + " consumed this notification");
                        return;
                    } else {
                        Log.d("Diplicity", "" + subscriber + " didn't consume this notification, checking if anyone else wants to");
                    }
                }
                Log.d("Diplicity", "Nobody consumed this notification, popping up a regular notification");
                sendNotification(remoteMessage);
            }
        });
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        Intent intent = new Intent(FCM_NOTIFY_ACTION);
        intent.putExtra(FCMReceiver.DIPLICITY_JSON_EXTRA, remoteMessage.getData().get(FCMReceiver.DIPLICITY_JSON_EXTRA));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_otto)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

}
