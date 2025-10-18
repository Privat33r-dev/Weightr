package com.snhu.weightr.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.snhu.weightr.data.db.entity.GoalWeightEntity;

@Dao
public interface GoalWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(GoalWeightEntity goal);


    @Query("DELETE FROM goal_weight WHERE user_id = :userId")
    void deleteForUser(long userId);

    @Query("SELECT * FROM goal_weight WHERE user_id = :userId LIMIT 1")
    GoalWeightEntity getForUser(long userId);
}
