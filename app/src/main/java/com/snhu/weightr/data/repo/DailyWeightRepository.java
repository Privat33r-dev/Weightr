package com.snhu.weightr.data.repo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.snhu.weightr.data.db.entity.DailyWeight;
import com.snhu.weightr.data.db.security.CypherUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DailyWeightRepository {
    private static final String TAG = "DailyWeightRepository";

    private final CollectionReference weightsRef;
    private final CypherUtility cypherUtility;

    public DailyWeightRepository(@NonNull String uid, @NonNull CypherUtility cypherUtility) {
        this.weightsRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("daily_weights");
        this.cypherUtility = cypherUtility;
    }

    public interface ErrorCallback {
        void onError(Exception e);
    }

    /**
     * Full history (DESC by date) with decrypted weights
     */
    public LiveData<List<DailyWeight>> getHistoryLive() {
        return new FirestoreHistoryLiveData(weightsRef.orderBy(FieldPath.documentId(), Query.Direction.DESCENDING), cypherUtility);
    }


    /**
     * Logs a new weight entry; fail if entry exists.
     *
     * @param weight    Weight value to log
     * @param date      Date of the entry in ISO 8601 format
     * @param onSuccess Optional callback on success (default: no-op)
     * @param onError   Optional callback on error (default: no-op)
     */
    public void logWeight(double weight,
                          @NonNull String date,
                          @Nullable Runnable onSuccess,
                          @Nullable ErrorCallback onError) {
        String encrypted = encryptWeight(weight);
        if (encrypted.isEmpty()) {
            if (onError != null) onError.onError(new Exception("Encryption failed"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("encryptedWeight", encrypted);

        weightsRef.document(date).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (onError != null) onError.onError(task.getException());
                return;
            }
            DocumentSnapshot snap = task.getResult();
            if (snap != null && snap.exists()) {
                if (onError != null) {
                    onError.onError(new IllegalArgumentException("Entry with this date already exists"));
                }
                return;
            }
            weightsRef.document(date).set(data)
                    .addOnSuccessListener(aVoid -> {
                        if (onSuccess != null) onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onError != null) onError.onError(e);
                    });
        });
    }

    /**
     * Update an entry; supports changing the date (atomic transaction)
     */
    public void updateWeight(@NonNull String oldDate,
                             double newWeight,
                             @NonNull String newDate,
                             @Nullable Runnable onSuccess,
                             @Nullable ErrorCallback onError) {
        String encrypted = encryptWeight(newWeight);
        if (encrypted.isEmpty()) {
            if (onError != null) onError.onError(new Exception("Encryption failed"));
            return;
        }

        Map<String, Object> data = Collections.singletonMap("encryptedWeight", encrypted);

        if (oldDate.equals(newDate)) {
            // Simple overwrite
            weightsRef.document(newDate).set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        if (onSuccess != null) onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onError != null) onError.onError(e);
                    });
        } else {
            // Transaction: delete old + check/create new
            FirebaseFirestore.getInstance().runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.delete(weightsRef.document(oldDate));

                DocumentSnapshot newSnap = transaction.get(weightsRef.document(newDate));
                if (newSnap.exists()) {
                    throw new FirebaseFirestoreException("Target date already exists",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS);
                }

                transaction.set(weightsRef.document(newDate), data);
                return null;
            }).addOnSuccessListener(unused -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onError != null) onError.onError(e);
            });
        }
    }

    /**
     * Delete one or more entries (by date from entity)
     */
    public void deleteWeights(@NonNull DailyWeight... weights) {
        for (DailyWeight w : weights) {
            weightsRef.document(w.date).delete().addOnFailureListener(e ->
                    Log.e(TAG, "Failed to delete entry for date " + w.date, e));
        }
    }

    private String encryptWeight(double weight) {
        try {
            return cypherUtility.encrypt(String.valueOf(weight));
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return "";
        }
    }

    /**
     * Internal LiveData wrapper for real-time history with decryption
     */
    private static class FirestoreHistoryLiveData extends LiveData<List<DailyWeight>> {
        private ListenerRegistration registration;
        private final Query query;
        private final CypherUtility cypherUtility;

        FirestoreHistoryLiveData(Query query, CypherUtility cypherUtility) {
            this.query = query;
            this.cypherUtility = cypherUtility;
        }

        @Override
        protected void onActive() {
            super.onActive();
            registration = query.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Firestore listen failed", error);
                    setValue(Collections.emptyList());
                    return;
                }
                if (snapshots == null) {
                    setValue(Collections.emptyList());
                    return;
                }

                List<DailyWeight> list = new ArrayList<>(snapshots.size());
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String enc = doc.getString("encryptedWeight");
                    String date = doc.getId();
                    if (enc == null || date.isEmpty()) continue;

                    DailyWeight entity = new DailyWeight();
                    entity.date = date;
                    entity.encryptedWeight = enc;

                    try {
                        String decrypted = cypherUtility.decrypt(enc);
                        entity.weight = Double.parseDouble(decrypted);
                    } catch (Exception ex) {
                        entity.weight = 0.0;
                        Log.e(TAG, "Decryption failed for date " + date, ex);
                    }

                    list.add(entity);
                }
                setValue(list);
            });
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        }
    }
}
