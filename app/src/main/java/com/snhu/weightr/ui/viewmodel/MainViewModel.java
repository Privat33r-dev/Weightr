package com.snhu.weightr.ui.viewmodel;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.repo.DailyWeightRepository;
import com.snhu.weightr.data.repo.GoalWeightRepository;
import com.snhu.weightr.data.session.SessionStore;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> goalWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> goalStartWeight = new MutableLiveData<>();
    private final MutableLiveData<Long> userId = new MutableLiveData<>(-2L);
    private final MutableLiveData<String> userName = new MutableLiveData<>("");

    @Nullable
    private DailyWeightRepository weightRepository;
    @Nullable
    private GoalWeightRepository goalWeightRepository;

    /**
     * Initializes the ViewModel. Call this once from MainActivity after creation.
     */
    public void init(Context context) {
        if (weightRepository != null || goalWeightRepository != null) return;

        Context appCtx = context.getApplicationContext();
        userId.setValue(SessionStore.get(appCtx).userId());
        userName.setValue(SessionStore.get(appCtx).username());

        weightRepository = new DailyWeightRepository(WeightrDb.get(appCtx).dailyWeightDao());
        goalWeightRepository = new GoalWeightRepository(WeightrDb.get(appCtx).goalWeightDao());

        refreshData();
    }

    /**
     * Refreshes the data (runs off main thread).
     */
    public void refreshData() {
        if (goalWeightRepository == null ||
                weightRepository == null || userId.getValue() == null ||
                userId.getValue() < 0) {
            currentWeight.setValue(null);
            goalWeight.setValue(null);
            return;
        }

        weightRepository.getLatestWeight(userId.getValue(), latest -> {
            Double weight = (latest != null) ? latest.weight : null;
            currentWeight.postValue(weight);
        });

        goalWeightRepository.getGoalWeight(userId.getValue(), goal -> {
            Double goalWeight = (goal != null) ? goal.currentGoal : null;
            Double goalStartWeight = (goal != null) ? goal.goalStart : null;
            this.goalWeight.postValue(goalWeight);
            this.goalStartWeight.postValue(goalStartWeight);
        });
    }

    /**
     * Sets the goal weight for the user.
     */
    public void setGoalWeight(double goal) {
        Double goalStart = currentWeight.getValue();
        long uid = userId.getValue();
        if (goalWeightRepository == null || uid == -1 ) {
            // TODO: graceful exception handling
            return;
        }

        goalWeightRepository.setGoal(userId.getValue(), goal, goalStart, () -> {
            goalWeight.postValue(goal);
            goalStartWeight.postValue(goalStart);
        });
    }

    public LiveData<Double> getCurrentWeight() {
        return currentWeight;
    }

    public LiveData<Double> getGoalWeight() {
        return goalWeight;
    }

    public LiveData<Double> getGoalStartWeight() {
        return goalStartWeight;
    }

    public LiveData<Long> getUserId() {
        return userId;
    }

    public LiveData<String> getUserName() {
        return userName;
    }
}