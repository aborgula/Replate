package com.example.replate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "login_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "📩 Message received from: " + remoteMessage.getFrom());

        // Sprawdź typ powiadomienia
        String notificationType = remoteMessage.getData().get("type");

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(TAG, "📩 Notification Title: " + title);
            Log.d(TAG, "📩 Notification Body: " + body);
            Log.d(TAG, "📩 Notification Type: " + notificationType);

            // Możesz dodać różne ikony/dźwięki w zależności od typu
            if ("new_parcel".equals(notificationType)) {
                String parcelId = remoteMessage.getData().get("parcelId");
                showParcelNotification(title, body, parcelId);
            } else {
                showNotification(title, body);
            }
        }
    }

    private void showParcelNotification(String title, String body, String parcelId) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Powiadomienia o paczkach",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

         Intent intent = new Intent(this, ParcelActivity.class);
         intent.putExtra("parcelId", parcelId);
         PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(parcelId.hashCode(), builder.build());
        Log.d(TAG, "✅ Parcel notification displayed!");
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Utwórz kanał powiadomień (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Powiadomienia logowania",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Powiadomienia o nowych logowaniach");
            notificationManager.createNotificationChannel(channel);
        }

        // Zbuduj powiadomienie
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Możesz zmienić ikonę
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
        Log.d(TAG, "✅ Notification displayed!");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "🔑 New FCM token: " + token);
    }
}
