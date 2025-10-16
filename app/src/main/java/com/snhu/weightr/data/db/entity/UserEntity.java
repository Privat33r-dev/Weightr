package com.snhu.weightr.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** User record (stores hashed password only). */
@Entity(
        tableName = "users",
        indices = {@Index(value = "username", unique = true)}
)
public final class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String username = ""; // init to satisfy @NonNull analysis

    /** Format: iterations:saltB64:dkB64:alg */
    @NonNull
    @ColumnInfo(name = "password")
    public String passwordHash = "";
}
