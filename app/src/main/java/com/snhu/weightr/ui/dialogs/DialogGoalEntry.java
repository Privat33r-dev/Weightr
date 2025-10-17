package com.snhu.weightr.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.weightr.R;
import com.snhu.weightr.databinding.FragmentDialogGoalEntryBinding;
import com.snhu.weightr.ui.viewmodel.MainViewModel;

/**
 * DialogFragment for setting or updating the goal weight.
 */
public class DialogGoalEntry extends DialogFragment {

    private FragmentDialogGoalEntryBinding binding;
    private MainViewModel viewModel;

    public DialogGoalEntry() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = FragmentDialogGoalEntryBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(binding.getRoot());

        setupUI();

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupUI() {
        // Pre-fill with current goal if available
        Double currentGoal = viewModel.getGoalWeight().getValue();
        if (currentGoal != null) {
            binding.goalWeightInput.setText(getString(R.string.current_goal, currentGoal));
        }

        binding.saveGoalButton.setOnClickListener(v -> {
            assert binding.goalWeightInput.getText() != null;
            String goalStr = binding.goalWeightInput.getText().toString().trim();
            if (TextUtils.isEmpty(goalStr) || !isValidWeight(goalStr)) {
                binding.goalWeightLayout.setError(getString(R.string.error_invalid_goal));
                return;
            }

            double goal = Double.parseDouble(goalStr);
            viewModel.setGoalWeight(goal);
            Toast.makeText(requireContext(), R.string.goal_saved, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        binding.cancelGoalButton.setOnClickListener(v -> dismiss());
    }

    private boolean isValidWeight(String weightStr) {
        try {
            double weight = Double.parseDouble(weightStr);
            return weight > 0 && weight < 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}