package com.snhu.weightr.ui.viewmodel;

import static com.snhu.weightr.util.Utils.approximatelyEqual;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.repo.DailyWeightRepository;
import com.snhu.weightr.data.repo.GoalWeightRepository;
import com.snhu.weightr.data.session.SessionStore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainViewModel extends ViewModel {

    /*
        Stores chart points.
     */
    public static final class WeightChartPoint {
        public final long epochDay;
        public final float weight;

        public WeightChartPoint(long epochDay, float weight) {
            this.epochDay = epochDay;
            this.weight = weight;
        }
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final MutableLiveData<List<DailyWeightEntity>> history = new MutableLiveData<>();
    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> previousWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> goalWeight = new MutableLiveData<>();
    private final MutableLiveData<Double> goalStartWeight = new MutableLiveData<>();
    private final MutableLiveData<Long> userId = new MutableLiveData<>(-2L);
    private final MutableLiveData<String> userName = new MutableLiveData<>("");
    private final MutableLiveData<String> motivationText = new MutableLiveData<>("");


    private static final String TAG = MainViewModel.class.getName();

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
        Long uid = userId.getValue();
        if (goalWeightRepository == null || weightRepository == null || uid == null || uid < 0) {
            currentWeight.postValue(null);
            previousWeight.postValue(null);
            goalWeight.postValue(null);
            goalStartWeight.postValue(null);
            motivationText.postValue("");
            return;
        }

        goalWeightRepository.getGoalWeight(uid, goal -> {
            Double goalWeight = (goal != null) ? goal.currentGoal : null;
            Double goalStartWeight = (goal != null) ? goal.goalStart : null;
            this.goalWeight.postValue(goalWeight);
            this.goalStartWeight.postValue(goalStartWeight);

            weightRepository.get2LatestWeights(uid, latest2 -> {
                int size = latest2 == null ? 0 : latest2.size();

                DailyWeightEntity latest = size > 0 ? latest2.get(0) : null;
                DailyWeightEntity previous = size > 1 ? latest2.get(1) : null;

                currentWeight.postValue(latest != null ? latest.weight : null);
                previousWeight.postValue(previous != null ? previous.weight : null);

                if (latest == null || previous == null || goalWeight == null || goalWeight <= 0) {
                    motivationText.postValue("");
                    return;
                }

                String msg = computeMotivation(latest.weight, previous.weight, goalWeight);
                motivationText.postValue(msg);
            });
        });

        loadHistory();
    }

    /**
     * Sets the goal weight for current user.
     */
    public void setGoalWeight(double goal) {
        double goalStart = currentWeight.getValue() != null ? currentWeight.getValue() : -1.0;
        Long uid = userId.getValue();
        if (goalWeightRepository == null || uid == null || approximatelyEqual(goalStart, -1.0)) {
            Log.e(TAG, "resetGoalWeight: no goalWeightRepository or uid or invalid goalStart");
            return;
        }

        goalWeightRepository.setGoal(uid, goal, goalStart, () -> {
            goalWeight.postValue(goal);
            goalStartWeight.postValue(goalStart);
        });
    }

    /**
     * Resets the goal weight for current user.
     */
    public void deleteGoalWeight() {
        Long uid = userId.getValue();
        if (goalWeightRepository == null || uid == null) {
            Log.e(TAG, "resetGoalWeight: no goalWeightRepository or uid");
            goalWeight.setValue(null);
            goalStartWeight.setValue(null);
            return;
        }

        goalWeightRepository.deleteGoal(uid, () -> {
            goalWeight.postValue(null);
            goalStartWeight.postValue(null);
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

    public LiveData<String> getMotivationText() {
        return motivationText;
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<List<DailyWeightEntity>> getHistory() {
        return history;
    }

    public void loadHistory() {
        Long userIdValue = userId.getValue();
        if (weightRepository == null || userIdValue == null) {
            history.setValue(java.util.Collections.emptyList());
            return;
        }
        weightRepository.listWeightsForUser(userIdValue, list ->
                history.postValue(new java.util.ArrayList<>(list))
        );
    }


    // Provides transformed historical data in a format suitable for use in chart
    public LiveData<List<WeightChartPoint>> getWeightPoints() {
        final long msPerDay = 86400000L;
        return Transformations.map(getHistory(), history -> {
            if (history == null) return Collections.emptyList();
            List<WeightChartPoint> out = new ArrayList<>(history.size());
            for (DailyWeightEntity e : history) {
                try {
                    Date d = sdf.parse(e.date);
                    if (d == null) continue;
                    out.add(new WeightChartPoint(d.getTime() / msPerDay, (float) e.weight));
                } catch (ParseException ex) {
                    Log.e(TAG, "Unable to parse date: " + e.date, ex);
                }
            }
            out.sort(Comparator.comparingLong(p -> p.epochDay));
            return out;
        });
    }


    private String computeMotivation(double curr, double prev, double goal) {
        double prevDist = Math.abs(prev - goal);
        double currDist = Math.abs(curr - goal);

        // >0 means improved (closer)
        double progressDelta = prevDist - currDist;

        // Plateau
        if (approximatelyEqual(progressDelta, 0)) {
            return "Plateaus happen. Keep going, as it's just a normal part of the process.";
        }

        // Strayed from the goal
        if (progressDelta < 0) {
            return "It's okay to slip sometimes. Keep going! You've got this!";
        }

        // Got closer to the goal
        // 5 pounds is a significant change. We assume that users know what they are doing.
        if (progressDelta >= 5.0) {
            return "Huge progress! You're moving fast towards your goal. Keep it up!";
        }
        if (progressDelta >= 1.0) {
            return "Nice progress. Keep it up!";
        }
        return "Good direction. Small steps add up.";
    }

}