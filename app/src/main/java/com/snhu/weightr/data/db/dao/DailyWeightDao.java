package com.snhu.weightr.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.snhu.weightr.data.db.entity.DailyWeightEntity;

import java.util.List;

@Dao
public interface DailyWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailyWeightEntity entry);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(DailyWeightEntity entry);

    @Query("SELECT * FROM daily_weights WHERE user_id = :userId ORDER BY date DESC")
    List<DailyWeightEntity> listForUser(long userId);

    @Query("SELECT * FROM daily_weights WHERE id = :id LIMIT 1")
    DailyWeightEntity getById(long id);

    @Query("DELETE FROM daily_weights WHERE id = :id")
    void deleteById(long id);

}