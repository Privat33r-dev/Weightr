package com.snhu.weightr.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
@Entity(
        tableName = "daily_weights",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("user_id"),
                @Index(value = {"user_id","date"}, unique = true)
        }
)
public final class DailyWeightEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    public double weight;

    /** ISO-8601 calendar date (YYYY-MM-DD). */
    @NonNull
    public String date = "";
}
