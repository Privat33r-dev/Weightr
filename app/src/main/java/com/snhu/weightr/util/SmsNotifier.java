package com.snhu.weightr.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.snhu.weightr.R;
import com.snhu.weightr.data.settings.SettingsStore;

public final class SmsNotifier {

    public interface Callback {
        void onComplete(boolean sent, @Nullable String error);
    }

    public static boolean isTelephonyAvailable(@NonNull Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean sendAlert(@NonNull Activity activity,
                                    @NonNull String body,
                                    @NonNull Callback cb) {
        // If user opted out or device can’t send SMS, fail fast
        if (!isTelephonyAvailable(activity)) {
            cb.onComplete(false, "No telephony");
            return false;
        }

        // If we have SEND_SMS and want silent send, use SmsManager:
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            cb.onComplete(false, "No SMS permissions");
            return false;
        }

        try {
            SmsManager sms = activity.getSystemService(SmsManager.class);
            TelephonyManager tMgr = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("HardwareIds") String mPhoneNumber = tMgr.getLine1Number();
            sms.sendTextMessage(mPhoneNumber, null, body, null, null);
            cb.onComplete(true, null);
            return true;
        } catch (Exception e) {
            cb.onComplete(false, e.getMessage());
        }
        return false;
    }

    private SmsNotifier() {
    }
}
