package com.snhu.weightr.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.snhu.weightr.R;

public final class LocalNotifier {
    private static final String CH_ID = "alerts";

    public static void ensureChannel(@NonNull Context ctx) {
        NotificationChannel ch = new NotificationChannel(CH_ID, "Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(ch);
    }

    public static void notify(@NonNull Context ctx, int id, @NonNull String title, @NonNull String text) {
        ensureChannel(ctx);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // your icon
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
            return;
        }
        NotificationManagerCompat.from(ctx).notify(id, b.build());
    }

    // Disallow instantiation (make a class static)
    private LocalNotifier() {
    }
}
