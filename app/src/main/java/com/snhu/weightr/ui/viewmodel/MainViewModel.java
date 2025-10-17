package com.snhu.weightr.ui.viewmodel;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.repo.WeightRepository;
import com.snhu.weightr.data.session.SessionStore;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> goalWeight = new MutableLiveData<>();
    private final MutableLiveData<Long> userId = new MutableLiveData<>(-2L);
    private final MutableLiveData<String> userName = new MutableLiveData<>("");

    @Nullable private WeightRepository weightRepository;

    /**
     * Initializes the ViewModel. Call this once from MainActivity after creation.
     */
    public void init(Context context) {
        if (weightRepository != null) return; // already initialized

        Context appCtx = context.getApplicationContext();
        userId.setValue(SessionStore.get(appCtx).userId());
        userName.setValue(SessionStore.get(appCtx).username());

        // Build repository from your WeightrDb singleton
        weightRepository = new WeightRepository(WeightrDb.get(appCtx).dailyWeightDao());

        refreshData();
    }

    /**
     * Refreshes the data (runs off main thread).
     */
    public void refreshData() {
        if (weightRepository == null || userId.getValue() == null) {
            currentWeight.setValue(null);
            goalWeight.setValue(null);
            return;
        }

        weightRepository.getLatestWeight(userId.getValue(), latest -> {
            Double weight = (latest != null) ? latest.weight : null;
            currentWeight.postValue(weight);
            // TODO: Fetch goal weight similarly when ready
        });
    }

    public LiveData<Double> getCurrentWeight() { return currentWeight; }
    public LiveData<Double> getGoalWeight() { return goalWeight; }
    public LiveData<Long> getUserId() { return userId; }
    public LiveData<String> getUserName() { return userName; }
}