package com.snhu.weightr.ui.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.snhu.weightr.R;

import java.util.ArrayList;
import java.util.Map;

public final class NotificationsDialog extends DialogFragment {

    private ActivityResultLauncher<String[]> requestPerms;
    private ArrayList<String> perms;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View root = getLayoutInflater().inflate(R.layout.dialog_notifications_permissions, null, false);

        setRequiredPermsForDevice();

        // Register permission launcher
        requestPerms = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onPermissionsResult
        );

        Button skip = root.findViewById(R.id.skip_permission_button);
        Button ask = root.findViewById(R.id.request_permission_button);

        skip.setOnClickListener(v -> {
            sendResult(false);
            dismiss();
        });

        ask.setOnClickListener(v -> {
            // Filter only not-yet-granted ones to avoid redundant prompts
            java.util.ArrayList<String> need = new java.util.ArrayList<>();
            for (String p : perms) {
                if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                    need.add(p);
                }
            }
            if (need.isEmpty()) {
                sendResult(true);
                dismiss();
            } else {
                requestPerms.launch(need.toArray(new String[0]));
            }
        });

        return new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();
    }

    private void onPermissionsResult(Map<String, Boolean> results) {
        boolean allGranted = true;
        for (Boolean granted : results.values()) {
            if (granted == null || !granted) {
                allGranted = false;
                break;
            }
        }
        sendResult(allGranted);
        dismiss();
    }

    private void sendResult(boolean granted) {
        Bundle b = new Bundle();
        b.putBoolean("granted", granted);
        getParentFragmentManager().setFragmentResult("notifications_perm_result", b);
    }

    private void setRequiredPermsForDevice() {
        perms = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    }


}
