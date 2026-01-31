package com.snhu.weightr.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.snhu.weightr.data.db.entity.DailyWeightEntity;

import java.util.List;

@Dao
public interface DailyWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailyWeightEntity entry);

    @Insert
    void insert(DailyWeightEntity entry);

    @Query("SELECT * FROM daily_weights WHERE user_id = :userId ORDER BY date DESC")
    LiveData<List<DailyWeightEntity>> listForUser(long userId);

    @Query("SELECT * FROM daily_weights WHERE id = :id LIMIT 1")
    DailyWeightEntity getById(long id);

    @Delete
    void deleteWeights(DailyWeightEntity... weights);
}