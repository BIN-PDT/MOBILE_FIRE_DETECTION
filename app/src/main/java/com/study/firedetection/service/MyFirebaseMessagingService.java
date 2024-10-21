package com.study.firedetection.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.study.firedetection.LoginActivity;
import com.study.firedetection.R;

import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public static final String NOTIFICATION_INTENT_ACTION = "DEVICE_VIEW";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // SHOW NOTIFICATION.
        Map<String, String> messageData = message.getData();
        String title = messageData.get("title");
        String body = messageData.get("body");
        String deviceId = messageData.get("device_id");
        String deviceName = messageData.get("device_name");
        this.showNotification(title, body, deviceId, deviceName);
    }

    private void showNotification(String title, String body, String deviceId, String deviceName) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setAction(NOTIFICATION_INTENT_ACTION);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("deviceName", deviceName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, getString(R.string.channel_id))
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.icon_notification)
                .setColor(getColor(R.color.fire))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
}