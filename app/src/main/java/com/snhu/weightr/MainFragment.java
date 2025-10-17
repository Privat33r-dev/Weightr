package com.snhu.weightr.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.weightr.R;
import com.snhu.weightr.databinding.FragmentMainBinding;
import com.snhu.weightr.ui.dialogs.DialogGoalEntry;
import com.snhu.weightr.ui.viewmodel.MainViewModel;

public final class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private MainViewModel viewModel;

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

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe current weight
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            binding.currentWeightValue.setText(weight != null
                    ? String.format("%.1f lbs", weight)
                    : getString(R.string.placeholder_weight));
        });

        // Observe goal weight
        viewModel.getGoalWeight().observe(getViewLifecycleOwner(), goal -> {
            binding.goalWeightValue.setText(goal != null
                    ? String.format("%.1f lbs", goal)
                    : getString(R.string.placeholder_goal));
            updateProgress(goal);
        });

        // Edit Goal button click
        binding.editGoalButton.setOnClickListener(v -> {
            DialogGoalEntry dialog = new DialogGoalEntry();
            dialog.show(getParentFragmentManager(), "GoalEntryDialog");
        });
    }

    private void updateProgress(Double goalWeight) {
        Double current = viewModel.getCurrentWeight().getValue();
        if (current != null && goalWeight != null && goalWeight > 0) {
            int progress = (int) ((current / goalWeight) * 100);
            binding.progressBar.setProgress(Math.min(progress, 100));
            binding.progressText.setText(String.format("%d%% to goal", progress));
        } else {
            // TODO: real visibility toggle
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.progressBar.setProgress(0);
            binding.progressText.setText(R.string.placeholder_progress);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}