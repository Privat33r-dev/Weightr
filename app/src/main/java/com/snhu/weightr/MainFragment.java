package com.snhu.weightr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.weightr.databinding.FragmentMainBinding;
import com.snhu.weightr.ui.dialogs.DialogGoalEntry;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.Utils;

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
}