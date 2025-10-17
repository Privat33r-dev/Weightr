package com.snhu.weightr.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.weightr.R;
import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.model.LoggedInUser;
import com.snhu.weightr.data.repo.WeightRepository;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.databinding.FragmentDialogWeightEntryBinding;
import com.snhu.weightr.ui.viewmodel.MainViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DialogFragment for logging a new weight entry.
 */
public class DialogWeightEntry extends DialogFragment {

    private FragmentDialogWeightEntryBinding binding;
    private MainViewModel viewModel;
    private WeightRepository weightRepository;

    public DialogWeightEntry() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = FragmentDialogWeightEntryBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        weightRepository = new WeightRepository(WeightrDb.get(requireContext()).dailyWeightDao());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(binding.getRoot());

        setupUI();

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }

    private void setupUI() {
        // Set current date (read-only)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        binding.dateText.setText(getString(R.string.current_date, currentDate));

        // Save button click
        binding.saveButton.setOnClickListener(v -> {
            Editable tmp = binding.weightInput.getText();
            String weightStr = (tmp != null) ? tmp.toString().trim() : "";
            if (TextUtils.isEmpty(weightStr) || !isValidWeight(weightStr)) {
                binding.weightLayout.setError(getString(R.string.error_invalid_weight));
                return;
            }

            saveWeightEntry(Double.parseDouble(weightStr), currentDate);
            dismiss();
        });

        // Cancel button click
        binding.cancelButton.setOnClickListener(v -> dismiss());
    }

    private boolean isValidWeight(String weightStr) {
        try {
            double weight = Double.parseDouble(weightStr);
            return weight > 0 && weight < 2000; // Reasonable weight range
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveWeightEntry(double weight, String date) {
        Long uid = viewModel.getUserId().getValue();
        if (uid == null || uid <= 0) {
            SessionStore.get(requireContext()).reset();
            Toast.makeText(requireContext(), R.string.error_no_user, Toast.LENGTH_SHORT).show();
            return;
        }
        weightRepository.logWeight(
                uid,
                weight, date, () -> {
                    viewModel.refreshData();
                });
        // TODO: exception handling
        Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show();
        dismiss();
    }
}