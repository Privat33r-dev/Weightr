package com.snhu.weightr.data.repo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.snhu.weightr.data.db.dao.DailyWeightDao;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.db.security.CypherUtility;
import com.snhu.weightr.data.repo.util.DbExecutor;

import java.util.List;

/**
 * Manages weight data operations.
 * Thin layer over DailyWeightDao for weight logging and retrieval.
 */
public final class DailyWeightRepository {
    private final String TAG = "DailyWeightRepository";

    private final DailyWeightDao weightDao;
    private final CypherUtility cypherUtility;

    /**
     * Constructs a repository with the provided DAO.
     *
     * @param weightDao DAO for accessing daily weight data
     */
    public DailyWeightRepository(@NonNull DailyWeightDao weightDao, CypherUtility cypherUtility) {
        this.weightDao = weightDao;
        this.cypherUtility = cypherUtility;
    }

    /**
     * Callback interface for handling errors during database operations.
     */
    public interface ErrorCallback {
        void onError(Exception e);
    }

    /**
     * Logs a new weight entry for a user.
     *
     * @param userId    ID of the user
     * @param weight    Weight value to log
     * @param date      Date of the entry in ISO 8601 format
     * @param onSuccess Optional callback on success (default: no-op)
     * @param onError   Optional callback on error (default: no-op)
     */
    public void logWeight(@NonNull Long userId,
                          double weight,
                          @NonNull String date,
                          @Nullable Runnable onSuccess,
                          @Nullable ErrorCallback onError) {
        DbExecutor.get().execute(() -> {
            try {
                DailyWeightEntity e = new DailyWeightEntity();
                e.userId = userId;
                e.weight = weight;
                e.encryptedWeight = encryptWeight(weight);
                e.date = date;
                weightDao.insert(e); // will fail if entry exists
                if (onSuccess != null) onSuccess.run();
            } catch (android.database.sqlite.SQLiteConstraintException err) {
                if (onError != null) onError.onError(
                        new IllegalArgumentException("Entry with this date already exists", err)
                );
            } catch (Exception ex) {
                if (onError != null) onError.onError(ex);
            }
        });
    }

    /**
     * Returns a LiveData of all weight entries for a user, sorted by date descending.
     *
     * @param userId ID of the user
     * @return LiveData containing the current list (may be empty)
     */
    public LiveData<List<DailyWeightEntity>> getHistoryLive(@NonNull Long userId) {
        return weightDao.listForUser(userId);
    }

    /**
     * Updates the weight value for a specific entry.
     *
     * @param id        ID of the weight entry to update
     * @param newWeight Updated weight value
     * @param newDate   Updated date value
     * @param callback  Optional callback for completion (default: no-op)
     */
    public void updateWeightById(@NonNull Long id, double newWeight, String newDate, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            DailyWeightEntity entry = weightDao.getById(id);
            if (entry != null) {
                entry.weight = newWeight;
                entry.encryptedWeight = encryptWeight(newWeight);
                entry.date = newDate;
                weightDao.upsert(entry);
                if (callback != null) callback.run();
            }
        });
    }

    /**
     * Deletes specified weight entries.
     *
     * @param weights Weight entries to delete
     */
    public void deleteWeights(@NonNull DailyWeightEntity... weights) {
        DbExecutor.get().execute(() -> weightDao.deleteWeights(weights));
    }

    private String encryptWeight(Double weight) {
        try {
            return cypherUtility.encrypt(String.valueOf(weight));
        } catch (Exception e) {
            Log.e(TAG, "encryptWeight: run into error during encryption", e);
        }
        return "";
    }
}