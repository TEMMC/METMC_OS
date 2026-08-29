package com.metmc.os.boot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class MetmcBootService extends Service {

    private static final String CHANNEL_ID = "metmc_boot";
    private static final int NOTIFICATION_ID = 6001;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("METMC OS")
                        .setContentText("METMC OS background process is running")
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)
                        .setOngoing(true)
                        .build();

        startForeground(
                NOTIFICATION_ID,
                notification
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        /*
         * METMC OS boot process.
         *
         * The service intentionally starts without
         * opening an Activity. This allows Android to
         * finish booting normally while METMC remains
         * available in the background.
         */

        return START_STICKY;
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "METMC OS",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "METMC OS background system process"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
