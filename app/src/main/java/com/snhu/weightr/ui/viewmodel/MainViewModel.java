package com.snhu.weightr.ui.viewmodel;

import static com.snhu.weightr.util.Utils.approximatelyEqual;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.db.entity.GoalWeightEntity;
import com.snhu.weightr.data.repo.DailyWeightRepository;
import com.snhu.weightr.data.repo.GoalWeightRepository;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.data.settings.SettingsStore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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

    private LiveData<List<DailyWeightEntity>> rawHistoryLive; // DESC from DB
    private LiveData<GoalWeightEntity> goalEntityLive;

    private final MutableLiveData<Boolean> sortDescending = new MutableLiveData<>(true);

    private LiveData<List<DailyWeightEntity>> sortedHistory;

    private LiveData<Double> currentWeight;

    // Derived from goalEntityLive
    private LiveData<Double> goalWeight;
    private LiveData<Double> goalStartWeight;

    // Mediator is needed for consistent updates (analogous to observer)
    private final MediatorLiveData<String> motivationText = new MediatorLiveData<>();

    private final MutableLiveData<Long> userId = new MutableLiveData<>(-2L);
    private final MutableLiveData<String> userName = new MutableLiveData<>("");

    private static final String TAG = MainViewModel.class.getName();

    @Nullable
    private DailyWeightRepository weightRepository;
    @Nullable
    private GoalWeightRepository goalWeightRepository;

    public void init(Context context) {
        if (weightRepository != null || goalWeightRepository != null) return;

        Context appCtx = context.getApplicationContext();
        userId.setValue(SessionStore.get(appCtx).userId());
        userName.setValue(SessionStore.get(appCtx).username());

        weightRepository = new DailyWeightRepository(WeightrDb.get(appCtx).dailyWeightDao());
        goalWeightRepository = new GoalWeightRepository(WeightrDb.get(appCtx).goalWeightDao());

        // Load initial sort preference
        sortDescending.setValue(SettingsStore.get(appCtx).isSortDescending());

        Long uid = userId.getValue();
        if (uid != null && uid > 0) {
            rawHistoryLive = weightRepository.getHistoryLive(uid);
            goalEntityLive = goalWeightRepository.getGoalWeightLive(uid);
        } else {
            rawHistoryLive = new MutableLiveData<>(Collections.emptyList());
            goalEntityLive = new MutableLiveData<>(null);
        }

        // Create derived LiveData after sources are set
        sortedHistory = Transformations.switchMap(sortDescending, desc ->
                Transformations.map(rawHistoryLive, list -> {
                    if (list == null) return Collections.emptyList();
                    if (desc) {
                        return list; // Raw DESC from DB
                    } else {
                        List<DailyWeightEntity> copy = new ArrayList<>(list);
                        sortByDate(copy, desc);
                        return copy;
                    }
                })
        );

        currentWeight = Transformations.map(rawHistoryLive, list ->
                list.isEmpty() ? null : list.get(0).weight
        );

        goalWeight = Transformations.map(goalEntityLive, entity ->
                entity != null ? entity.currentGoal : null
        );

        goalStartWeight = Transformations.map(goalEntityLive, entity ->
                entity != null ? entity.goalStart : null
        );

        motivationText.addSource(rawHistoryLive, historyList -> updateMotivation(historyList, goalEntityLive.getValue()));
        motivationText.addSource(goalEntityLive, goalEntity -> updateMotivation(rawHistoryLive.getValue(), goalEntity));
    }

    private void updateMotivation(List<DailyWeightEntity> historyList, GoalWeightEntity goalEntity) {
        if (historyList == null || historyList.size() < 2 || goalEntity == null || goalEntity.currentGoal <= 0) {
            motivationText.setValue("");
            return;
        }

        double curr = historyList.get(0).weight;
        double prev = historyList.get(1).weight;
        double goal = goalEntity.currentGoal;

        String motivationMessage = computeMotivation(curr, prev, goal);

        motivationText.setValue(motivationMessage);
    }

    @NonNull
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

    public void setGoalWeight(double goal) {
        double goalStart = currentWeight.getValue() != null ? currentWeight.getValue() : -1.0;
        Long uid = userId.getValue();
        if (goalWeightRepository == null || uid == null || approximatelyEqual(goalStart, -1.0)) {
            Log.e(TAG, "setGoalWeight: invalid state");
            return;
        }

        goalWeightRepository.setGoal(uid, goal, goalStart);
    }

    public void deleteGoalWeight() {
        Long uid = userId.getValue();
        if (goalWeightRepository == null || uid == null) {
            Log.e(TAG, "deleteGoalWeight: invalid state");
            return;
        }

        goalWeightRepository.deleteGoal(uid);
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

    public LiveData<List<DailyWeightEntity>> getHistory() {
        return sortedHistory;
    }

    public void setSortDescending(boolean descending) {
        sortDescending.setValue(descending);
    }

    public void deleteWeight(@NonNull DailyWeightEntity item) {
        if (weightRepository == null) {
            Log.e(TAG, "deleteWeight: no weightRepository");
            return;
        }
        weightRepository.deleteWeights(item);
    }

    public LiveData<List<WeightChartPoint>> getWeightPoints() {
        final long msPerDay = 86400000L;
        return Transformations.map(rawHistoryLive, history -> {
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

    /**
     * Sort the list by date in-place using a Quicksort algorithm.
     * Original Quicksort algorithm was published by Tony Hoare in 1961.
     *
     * @param list       The list to sort.
     * @param descending True for newest first (descending order), false for oldest first (ascending order).
     */
    private static void sortByDate(List<DailyWeightEntity> list, boolean descending) {
        if (list == null || list.size() <= 1) return;
        quickSort(list, 0, list.size() - 1, descending);
    }

    private static void quickSort(List<DailyWeightEntity> list, int low, int high, boolean descending) {
        if (low >= high) return;
        int pivotIndex = partition(list, low, high, descending);
        quickSort(list, low, pivotIndex - 1, descending);
        quickSort(list, pivotIndex + 1, high, descending);
    }

    private static int partition(List<DailyWeightEntity> list, int low, int high, boolean descending) {
        // Randomized pivot to avoid worst-case scenarios
        Random rand = new Random();
        int pivotIndex = low + rand.nextInt(high - low + 1);
        swap(list, pivotIndex, high);

        String pivotDate = list.get(high).date;

        int i = low - 1;
        for (int j = low; j < high; j++) {
            String currentDate = list.get(j).date;
            int cmp = currentDate.compareTo(pivotDate);
            if (descending) cmp = -cmp;
            if (cmp < 0) {
                i++;
                swap(list, i, j);
            }
        }

        swap(list, i + 1, high);
        return i + 1;
    }

    private static void swap(List<DailyWeightEntity> list, int i, int j) {
        DailyWeightEntity temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}