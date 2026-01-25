package com.snhu.weightr;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.snhu.weightr.databinding.FragmentMainBinding;
import com.snhu.weightr.ui.dialogs.DialogGoalEntry;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private LineChart chart;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final String TAG = "MainFragment";
    private static final float DAYS_IN_MONTH = 30f;
    private Float latestGoal;


    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = view.findViewById(R.id.weight_trend_chart);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe current weight
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            binding.editGoalButton.setEnabled(weight != null);
            binding.currentWeightValue.setText(weight != null
                    ? getString(R.string.weight_format, weight)
                    : getString(R.string.placeholder_weight));
            updateProgress();
        });

        // Observe goal weight
        viewModel.getGoalWeight().observe(getViewLifecycleOwner(), goal -> {
            binding.goalWeightValue.setText(goal != null
                    ? getString(R.string.weight_format, goal)
                    : getString(R.string.placeholder_goal));
            if (goal != null) {
                latestGoal = goal.floatValue();
                setGoalOnChart(goal.floatValue());
            }
            updateProgress();
        });

        // Observe goal start weight
        viewModel.getGoalStartWeight().observe(getViewLifecycleOwner(), goal -> {
            binding.goalWeightStartValue.setText(goal != null
                    ? getString(R.string.weight_format, goal)
                    : "");
            binding.goalWeightIcon.setVisibility(goal != null ? View.VISIBLE : View.GONE);
            updateProgress();
        });

        // Observe history changes and update chart accordingly
        viewModel.getWeightPoints().observe(getViewLifecycleOwner(), points -> {
            if (points == null || points.size() < 2) {
                chart.clear();
                return;
            }
            renderWeightChart(points);
            ensureGoalVisibility(latestGoal);
        });

        viewModel.getMotivationText().observe(getViewLifecycleOwner(), msg ->
                binding.motivationText.setText(!msg.isBlank() ? msg : getString(R.string.motivation_message))
        );

        // Edit Goal button click
        binding.editGoalButton.setOnClickListener(v -> {
            // We need user's current weight to set it as a starting point
            // So we can calculate how the user progresses
            if (viewModel.getCurrentWeight().getValue() == null) {
                Toast.makeText(requireContext(), R.string.set_weight_first, Toast.LENGTH_SHORT).show();
                return;
            }

            DialogGoalEntry dialog = new DialogGoalEntry();
            dialog.show(getParentFragmentManager(), "GoalEntryDialog");
        });
    }

    private void updateProgress() {
        Double goalWeight = viewModel.getGoalWeight().getValue();
        Double start = viewModel.getGoalStartWeight().getValue(); // Starting weight from goal
        Double current = viewModel.getCurrentWeight().getValue();
        if (current != null && goalWeight != null && start != null && !Utils.approximatelyEqual(start, goalWeight)) {
            double progress = ((current - start) / (goalWeight - start)) * 100;
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressText.setVisibility(View.VISIBLE);
            binding.progressBar.setProgress(Math.min((int) progress, 100));
            binding.progressText.setText(getString(R.string.to_goal, (int) progress));
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.progressText.setVisibility(View.GONE);
            binding.progressBar.setProgress(0);
            binding.progressText.setText(R.string.placeholder_progress);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void renderWeightChart(List<MainViewModel.WeightChartPoint> points) {
        List<Entry> entries = toEntries(points);
        LineDataSet set = new LineDataSet(entries, "Your Weight");
        chart.setData(new LineData(set));

        applyChartStyle(set);
        // We show last month for better UX, since users likely don't want to see
        // all their last X years history
        showLastMonthOnChart(entries);
        // Invalidate to actually redraw the chart
        chart.invalidate();
    }


    private List<Entry> toEntries(List<MainViewModel.WeightChartPoint> points) {
        List<Entry> entries = new ArrayList<>(points.size());
        for (MainViewModel.WeightChartPoint p : points) {
            entries.add(new Entry(p.epochDay, p.weight));
        }
        return entries;
    }

    private void applyChartStyle(LineDataSet set) {
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                // x is epochDay
                long ms = (long) value * 86400000L;
                return sdf.format(new Date(ms));
            }
        });
        chart.getXAxis().setGranularity(1f);

        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDrawGridBackground(false);

        int primary = resolveColor(requireContext(), com.google.android.material.R.attr.colorPrimaryVariant);
        set.setColor(primary);
        set.setCircleColor(primary);
        Description desc = chart.getDescription();
        desc.setText("");

        chart.setExtraOffsets(0f, 16f, 40f, 16f);
        chart.offsetLeftAndRight(10);

        set.setLineWidth(3f);
        set.setCircleRadius(5f);

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setHighlightPerDragEnabled(false);

        chart.getLineData().setValueTextSize(12f);

        YAxis y = chart.getAxisLeft();
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setXOffset(16f);
        y.setDrawAxisLine(false);
    }

    private static int resolveColor(Context c, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void setGoalOnChart(float goalWeight) {
        LimitLine goalLine = new LimitLine(goalWeight, "Goal");
        goalLine.setLineWidth(2f);
        goalLine.setLineColor(Color.YELLOW);
        goalLine.enableDashedLine(10f, 10f, 0f);
        goalLine.setTextSize(12f);
        goalLine.setTextColor(Color.YELLOW);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // deduplicate
        leftAxis.addLimitLine(goalLine);
        leftAxis.setDrawLimitLinesBehindData(true);

        ensureGoalVisibility(goalWeight);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void ensureGoalVisibility(Float goal) {
        if (goal == null || chart == null) return;
        YAxis axis = chart.getAxisLeft();
        LineData data = chart.getLineData();
        if (data == null || axis == null) return;

        // use offset to improve UX by avoiding overlaying/hidden elements
        final float offset = 5;
        float min = Math.min(data.getYMin(), goal) - offset;
        float max = Math.max(data.getYMax(), goal) + offset;

        axis.setAxisMinimum(min);
        axis.setAxisMaximum(max);
    }


    private void showLastMonthOnChart(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return;
        float latest_record = entries.get(entries.size() - 1).getX();
        chart.setVisibleXRangeMaximum(DAYS_IN_MONTH);
        chart.moveViewToX(latest_record);
        chart.invalidate();
    }

}