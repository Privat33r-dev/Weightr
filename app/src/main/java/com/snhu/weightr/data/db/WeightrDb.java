package com.snhu.weightr.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.snhu.weightr.data.db.dao.DailyWeightDao;
import com.snhu.weightr.data.db.dao.GoalWeightDao;
import com.snhu.weightr.data.db.dao.UserDao;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.db.entity.GoalWeightEntity;
import com.snhu.weightr.data.db.entity.UserEntity;

@Database(
        entities = {UserEntity.class, GoalWeightEntity.class, DailyWeightEntity.class},
        version = 1,
        exportSchema = true
)
public abstract class WeightrDb extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract GoalWeightDao goalWeightDao();

    public abstract DailyWeightDao dailyWeightDao();

    private static volatile WeightrDb instance;

    @NonNull
    public static WeightrDb get(@NonNull final Context context) {
        WeightrDb db = instance;
        if (db != null) return db;
        synchronized (WeightrDb.class) {
            if (instance != null) return instance;
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    WeightrDb.class,
                    "weightr.db"
            ).build();
            return instance;
        }
    }
}