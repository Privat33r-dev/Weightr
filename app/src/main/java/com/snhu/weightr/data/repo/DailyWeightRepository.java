package com.snhu.weightr.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snhu.weightr.data.db.dao.DailyWeightDao;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.repo.util.DbExecutor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Manages weight data operations.
 * Thin layer over DailyWeightDao for weight logging and retrieval.
 */
public final class DailyWeightRepository {

    private final DailyWeightDao weightDao;

    /**
     * Constructs a repository with the provided DAO.
     *
     * @param weightDao DAO for accessing daily weight data
     */
    public DailyWeightRepository(@NonNull DailyWeightDao weightDao) {
        this.weightDao = weightDao;
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
                e.date = date;
                weightDao.insert(e); // will insert or update; no exception
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
     * Retrieves all weight entries for a user, sorted by date descending.
     *
     * @param userId   ID of the user
     * @param callback Callback invoked on completion with the list as a parm (may be empty)
     */
    public void listWeightsForUser(@NonNull Long userId, @NonNull Consumer<List<DailyWeightEntity>> callback) {
        DbExecutor.get().execute(() -> {
            List<DailyWeightEntity> list = weightDao.listForUser(userId);
            callback.accept(list);
        });
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
                entry.date = newDate;
                weightDao.upsert(entry);
                if (callback != null) callback.run();
            }
        });
    }

    /**
     * Deletes a specific weight entry.
     *
     * @param weight   Weight entry to delete
     * @param callback Optional callback for completion (default: no-op)
     */
    public void deleteWeight(@NonNull DailyWeightEntity weight, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            weightDao.deleteWeights(weight);
            if (callback != null) callback.run();
        });
    }

    /**
     * Retrieves the latest 2 weight entries for a user.
     *
     * @param userId   ID of the user
     * @param callback Callback invoked with the List\<DailyWeightEntity\> containing 2 latest entities
     */
    public void get2LatestWeights(
            @NonNull Long userId,
            @NonNull Consumer<List<DailyWeightEntity>> callback
    ) {
        DbExecutor.get().execute(() -> {
            List<DailyWeightEntity> list = weightDao.listLast2ForUser(userId);
            callback.accept(list);
        });
    }

}