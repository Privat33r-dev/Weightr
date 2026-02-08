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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snhu.weightr.data.db.entity.DailyWeight;
import com.snhu.weightr.data.db.entity.GoalWeight;
import com.snhu.weightr.data.db.security.CypherUtility;
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

import javax.crypto.SecretKey;

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

    private LiveData<List<DailyWeight>> rawHistoryLive;
    private LiveData<GoalWeight> goalEntityLive;

    private final MutableLiveData<Boolean> sortDescending = new MutableLiveData<>(true);

    private LiveData<List<DailyWeight>> sortedHistory;

    private LiveData<Double> currentWeight;
    private LiveData<Double> goalWeight;
    private LiveData<Double> goalStartWeight;

    private final MediatorLiveData<String> motivationText = new MediatorLiveData<>();

    private final MutableLiveData<String> uid = new MutableLiveData<>("");

    private static final String TAG = MainViewModel.class.getName();

    @Nullable
    private DailyWeightRepository weightRepository;
    @Nullable
    private GoalWeightRepository goalWeightRepository;

    public void init(Context context) {
        if (weightRepository != null || goalWeightRepository != null) return;

        Context appCtx = context.getApplicationContext();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated");
        }
        String firebaseUid = currentUser.getUid();

        SecretKey encryptionKey = SessionStore.get(appCtx).encryptionKey();
        if (encryptionKey == null) {
            throw new IllegalStateException("No encryption key");
        }

        CypherUtility cypherUtility = new CypherUtility(encryptionKey);

        uid.setValue(firebaseUid);

        weightRepository = new DailyWeightRepository(firebaseUid, cypherUtility);
        goalWeightRepository = new GoalWeightRepository(firebaseUid, cypherUtility);

        sortDescending.setValue(SettingsStore.get(appCtx).isSortDescending());

        rawHistoryLive = weightRepository.getHistoryLive(); // already decrypted inside repo LiveData

        goalEntityLive = goalWeightRepository.getGoalWeightLive();

        sortedHistory = Transformations.switchMap(sortDescending, desc ->
                Transformations.map(rawHistoryLive, list -> {
                    if (list == null) return Collections.emptyList();
                    if (desc) return list;
                    // The list is presorted in DESC, so we can just reverse to get ASC sorting
                    List<DailyWeight> copy = new ArrayList<>(list);
                    Collections.reverse(copy);
                    return copy;
                }));

        currentWeight = Transformations.map(rawHistoryLive, list -> list.isEmpty() ? null : list.get(0).weight);

        goalWeight = Transformations.map(goalEntityLive, entity -> entity != null ? entity.currentGoal : null);
        goalStartWeight = Transformations.map(goalEntityLive, entity -> entity != null ? entity.goalStart : null);

        motivationText.addSource(rawHistoryLive, historyList -> updateMotivation(historyList, goalEntityLive.getValue()));
        motivationText.addSource(goalEntityLive, goalEntity -> updateMotivation(rawHistoryLive.getValue(), goalEntity));
    }

    private void updateMotivation(List<DailyWeight> historyList, GoalWeight goalEntity) {
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
        if (goalWeightRepository == null || approximatelyEqual(goalStart, -1.0)) {
            Log.e(TAG, "setGoalWeight: invalid state");
            return;
        }

        goalWeightRepository.setGoal(goal, goalStart);
    }

    public void deleteGoalWeight() {
        if (goalWeightRepository == null) {
            Log.e(TAG, "deleteGoalWeight: invalid state");
            return;
        }

        goalWeightRepository.deleteGoal();
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

    public LiveData<String> getMotivationText() {
        return motivationText;
    }

    public LiveData<List<DailyWeight>> getHistory() {
        return sortedHistory;
    }

    public void setSortDescending(boolean descending) {
        sortDescending.setValue(descending);
    }

    /**
     * Logs a new weight entry with callbacks.
     *
     * @param weight    Weight value to log
     * @param dateIso   Date of the entry in ISO 8601 format
     * @param onSuccess Optional callback on success
     * @param onError   Optional callback on error
     */
    public void logNewWeight(double weight, @NonNull String dateIso, @Nullable Runnable onSuccess,
                             @Nullable DailyWeightRepository.ErrorCallback onError) {
        if (weightRepository == null) {
            if (onError != null)
                onError.onError(new IllegalStateException("Repository not initialized"));
            return;
        }
        weightRepository.logWeight(weight, dateIso, onSuccess, onError);
    }


    /**
     * Updates an existing weight entry with callbacks.
     *
     * @param oldDate    Original date to identify entry to edit
     * @param newWeight  Updated weight value
     * @param newDateIso Updated date value
     * @param onSuccess  Optional callback for completion
     */
    public void updateExistingWeight(@NonNull String oldDate, double newWeight,
                                     @NonNull String newDateIso, @Nullable Runnable onSuccess) {
        if (weightRepository == null) return;
        weightRepository.updateWeight(oldDate, newWeight, newDateIso, onSuccess, null);
    }

    /**
     * Deletes a weight entry.
     *
     * @param item Weight entry to delete
     */
    public void deleteWeight(@NonNull DailyWeight item) {
        if (weightRepository == null) return;
        weightRepository.deleteWeights(item);
    }

    public LiveData<List<WeightChartPoint>> getWeightPoints() {
        final long msPerDay = 86400000L;
        return Transformations.map(rawHistoryLive, history -> {
            if (history == null) return Collections.emptyList();
            List<WeightChartPoint> out = new ArrayList<>(history.size());
            for (DailyWeight e : history) {
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

}