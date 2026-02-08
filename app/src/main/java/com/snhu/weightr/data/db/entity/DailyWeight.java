package com.snhu.weightr.data.db.entity;

import androidx.annotation.NonNull;

public final class DailyWeight {
    // Filled after decryption
    public double weight = 0.0;

    // Encrypted fields stored in Firestore
    @NonNull
    public String encryptedWeight = "";

    // Fields stored directly in Firestore
    @NonNull
    public String date = "";

    public DailyWeight() {
    }
}