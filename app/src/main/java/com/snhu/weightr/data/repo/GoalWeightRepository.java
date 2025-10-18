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

    public void setGoal(@NonNull Long userId, double currentGoal, double goalStart, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            GoalWeightEntity existing = goalWeightDao.getForUser(userId);
            if (existing == null) {
                existing = new GoalWeightEntity();
                existing.userId = userId;
            }
            existing.currentGoal = currentGoal;
            existing.goalStart = goalStart;
            goalWeightDao.upsert(existing);
            if (callback != null) callback.run();
        });
    }

    public void deleteGoal(@NonNull Long userId, @Nullable Runnable callback) {
        DbExecutor.get().execute(() -> {
            goalWeightDao.deleteForUser(userId);
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