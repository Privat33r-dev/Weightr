package com.snhu.weightr.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.snhu.weightr.data.db.dao.GoalWeightDao;
import com.snhu.weightr.data.db.entity.GoalWeightEntity;
import com.snhu.weightr.data.repo.util.DbExecutor;

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
     * Sets the goal weight for a user.
     *
     * @param userId      ID of the user
     * @param currentGoal new goal of the user
     * @param goalStart   new start of the goal of the user
     */
    public void setGoal(@NonNull Long userId, double currentGoal, double goalStart) {
        DbExecutor.get().execute(() -> {
            GoalWeightEntity existing = goalWeightDao.getForUser(userId);
            if (existing == null) {
                existing = new GoalWeightEntity();
                existing.userId = userId;
            }
            existing.currentGoal = currentGoal;
            existing.goalStart = goalStart;
            goalWeightDao.upsert(existing);
        });
    }

    /**
     * Deletes the goal weight for a user.
     *
     * @param userId   ID of the user
     */
    public void deleteGoal(@NonNull Long userId) {
        DbExecutor.get().execute(() -> {
            goalWeightDao.deleteForUser(userId);
        });
    }

    /**
     * Returns a LiveData of the goal weight entity for a specified user.
     *
     * @param userId ID of the user
     * @return LiveData containing the current GoalWeightEntity (or null if none)
     */
    public LiveData<GoalWeightEntity> getGoalWeightLive(@NonNull Long userId) {
        return goalWeightDao.getForUserLive(userId);
    }
}