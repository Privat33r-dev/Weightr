package com.snhu.weightr.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snhu.weightr.data.db.dao.GoalWeightDao;
import com.snhu.weightr.data.db.entity.GoalWeightEntity;
import com.snhu.weightr.data.repo.util.DbExecutor;

import java.util.function.Consumer;

/**
 * Manages goal weight data operations.
 * Thin layer over GoalWeightDao for goal weight storage and retrieval.
 */
public final class GoalWeightRepository {

    private final GoalWeightDao goalWeightDao;

    public GoalWeightRepository(@NonNull GoalWeightDao goalWeightDao) {
        this.goalWeightDao = goalWeightDao;
    }

    /**
     * Sets or updates the goal weight for a user.
     *
     * @param userId ID of the user
     * @param weight Goal weight value
     * @param callback Optional callback for completion (default: no-op)
     */
    public void setGoalWeight(@NonNull Long userId, double weight, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            GoalWeightEntity entry = new GoalWeightEntity();
            entry.userId = userId;
            entry.weight = weight;
            long result = goalWeightDao.upsert(entry); // Replaces existing entry for userId
            if (callback != null) callback.run();
        });
    }

    /**
     * Retrieves the goal weight for a user.
     *
     * @param userId ID of the user
     * @return GoalWeightEntity or null if none
     */
    public void getGoalWeight(@NonNull Long userId, @NonNull Consumer<GoalWeightEntity> callback) {
        DbExecutor.get().execute(() -> {
            GoalWeightEntity goal = goalWeightDao.getForUser(userId);
            callback.accept(goal);
        });
    }
}