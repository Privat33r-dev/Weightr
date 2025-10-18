package com.snhu.weightr.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "goal_weight",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "user_id", unique = true)}
)
public final class GoalWeightEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /* Target weight */
    @ColumnInfo(name = "current_goal")
    public double currentGoal;

    /* Starting weight when goal was set */
    @ColumnInfo(name = "goal_start")
    public double goalStart;
}