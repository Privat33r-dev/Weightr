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
    long upsert(DailyWeightEntity entry);

    @Query("SELECT * FROM daily_weights WHERE user_id = :userId ORDER BY date DESC")
    List<DailyWeightEntity> listForUser(long userId);

    @Query("DELETE FROM daily_weights WHERE user_id = :userId AND date = :isoDate")
    void deleteForDay(long userId, String isoDate);
}