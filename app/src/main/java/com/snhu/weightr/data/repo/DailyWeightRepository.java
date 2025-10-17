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

    public DailyWeightRepository(@NonNull DailyWeightDao weightDao) {
        this.weightDao = weightDao;
    }

    /**
     * Logs a new weight entry for a user.
     *
     * @param userId ID of the user
     * @param weight Weight value to log
     * @param date Date of the entry in ISO 8601 format
     * @param callback Optional callback for completion (default: no-op)
     * @throws IllegalStateException if entry already exists for the date
     */
    public void logWeight(@NonNull Long userId, double weight, @NonNull String date, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            DailyWeightEntity existing = weightDao.listForUser(userId).stream()
                    .filter(e -> e.date.equals(date))
                    .findFirst().orElse(null);
            if (existing != null) {
//                Toast.makeText(requireContext(), R.string.already_logged, Toast.LENGTH_SHORT).show();
                return;
//                throw new IllegalStateException("Weight already logged for this date");
            }

            DailyWeightEntity entry = new DailyWeightEntity();
            entry.userId = userId;
            entry.weight = weight;
            entry.date = date;
            weightDao.upsert(entry);
            if (callback != null) callback.run();
        });
    }

    /**
     * Retrieves all weight entries for a user, sorted by date descending.
     *
     * @param userId   ID of the user
     * @param callback Callback invoked on completion with the list (may be empty)
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
     * @param id ID of the weight entry to update
     * @param newWeight Updated weight value
     * @param callback Optional callback for completion (default: no-op)
     */
    public void updateWeight(@NonNull Long id, double newWeight, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            DailyWeightEntity entry = weightDao.getById(id);
            if (entry != null) {
                entry.weight = newWeight;
                weightDao.upsert(entry);
                if (callback != null) callback.run();
            }
        });
    }

    /**
     * Deletes a specific weight entry.
     *
     * @param id ID of the weight entry to delete
     * @param callback Optional callback for completion (default: no-op)
     */
    public void deleteWeight(@NonNull Long id, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            weightDao.deleteById(id);
            if (callback != null) callback.run();
        });
    }

    /**
     * Retrieves the latest weight entry for a user asynchronously.
     *
     * @param userId   ID of the user
     * @param callback Callback invoked with the latest DailyWeightEntity (or null if none)
     */
    public void getLatestWeight(@NonNull Long userId, @NonNull Consumer<DailyWeightEntity> callback) {
        DbExecutor.get().execute(() -> {
            List<DailyWeightEntity> list = weightDao.listForUser(userId);
            DailyWeightEntity latest = list.isEmpty() ? null : list.get(0);
            callback.accept(latest);
        });
    }
}