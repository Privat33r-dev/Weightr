package com.snhu.weightr.data.repo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.snhu.weightr.data.db.entity.GoalWeight;
import com.snhu.weightr.data.db.security.CypherUtility;

import java.util.HashMap;
import java.util.Map;

public final class GoalWeightRepository {

    private static final String TAG = "GoalWeightRepository";

    private final DocumentReference goalDocRef;
    private final CypherUtility cypherUtility;

    public GoalWeightRepository(@NonNull String uid, @NonNull CypherUtility cypherUtility) {
        this.goalDocRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("settings")
                .document("goal");
        this.cypherUtility = cypherUtility;
    }

    public void setGoal(double currentGoal, double goalStart) {
        String encCurrent = encrypt(currentGoal);
        String encStart = encrypt(goalStart);

        if (encCurrent.isEmpty() || encStart.isEmpty()) {
            Log.e(TAG, "Encryption failed for goal weight");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("encryptedCurrentGoal", encCurrent);
        data.put("encryptedGoalStart", encStart);

        goalDocRef.set(data)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to set encrypted goal", e));
    }

    public void deleteGoal() {
        goalDocRef.delete()
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete goal", e));
    }

    public LiveData<GoalWeight> getGoalWeightLive() {
        return new GoalLiveData(goalDocRef, cypherUtility);
    }

    private String encrypt(double value) {
        try {
            return cypherUtility.encrypt(String.valueOf(value));
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return "";
        }
    }

    /**
     * Internal LiveData wrapper for real-time Goal data with decryption
     */
    private static class GoalLiveData extends LiveData<GoalWeight> {
        private ListenerRegistration registration;
        private final DocumentReference docRef;
        private final CypherUtility cypherUtility;

        GoalLiveData(DocumentReference docRef, CypherUtility cypherUtility) {
            this.docRef = docRef;
            this.cypherUtility = cypherUtility;
            setValue(null);
        }

        @Override
        protected void onActive() {
            super.onActive();

            docRef.get().addOnSuccessListener(this::processSnapshot).addOnFailureListener(e -> {
                Log.e(TAG, "Initial goal fetch failed", e);
                setValue(null);
            });

            // Attach real-time listener
            registration = docRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Goal listen error", error);
                    setValue(null);
                    return;
                }
                processSnapshot(snapshot);
            });
        }

        private void processSnapshot(@Nullable DocumentSnapshot snapshot) {
            if (snapshot == null || !snapshot.exists()) {
                setValue(null);
                return;
            }

            String encCurrent = snapshot.getString("encryptedCurrentGoal");
            String encStart = snapshot.getString("encryptedGoalStart");

            if (encCurrent == null || encStart == null) {
                Log.w(TAG, "Missing encrypted fields – emitting null");
                setValue(null);
                return;
            }

            GoalWeight entity = new GoalWeight();

            try {
                entity.currentGoal = Double.parseDouble(cypherUtility.decrypt(encCurrent));
                entity.goalStart = Double.parseDouble(cypherUtility.decrypt(encStart));
                setValue(entity);
            } catch (Exception ex) {
                Log.e(TAG, "Decryption failed – emitting null", ex);
                setValue(null);
            }
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            Log.d(TAG, "GoalLiveData onInactive – removing listener");
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        }
    }
}