package com.snhu.weightr.ui.dialogs;

import static com.snhu.weightr.util.Utils.isoUtc;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.snhu.weightr.R;
import com.snhu.weightr.databinding.DialogWeightEntryBinding;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DialogWeightEntry extends DialogFragment {

    private static final String ARG_WEIGHT = "weight";   // For edit: new weight
    private static final String ARG_DATE = "date";       // For edit: original date

    private DialogWeightEntryBinding binding;
    private MainViewModel viewModel;

    private boolean isEdit = false;
    private String originalDateIso = null;
    private String selectedDateIso;

    private static final String TAG = DialogWeightEntry.class.getName();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogWeightEntryBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        parseArgs(getArguments());

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext()).setView(binding.getRoot());
        setupUI();
        return b.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void parseArgs(@Nullable Bundle args) {
        if (args != null && args.containsKey(ARG_DATE)) {
            isEdit = true;
            originalDateIso = args.getString(ARG_DATE);
            double w = args.getDouble(ARG_WEIGHT, 0d);
            selectedDateIso = originalDateIso != null ? originalDateIso : isoUtc(new Date());

            if (w > 0 && binding != null) {
                binding.weightInput.setText(String.valueOf(w));
            }
        } else {
            isEdit = false;
            originalDateIso = null;
            selectedDateIso = isoUtc(new Date());
        }
    }

    private void setupUI() {
        binding.dateText.setText(getString(R.string.current_date, selectedDateIso));
        binding.dateText.setOnClickListener(v -> openDatePicker());

        binding.saveButton.setOnClickListener(v -> {
            Editable tmp = binding.weightInput.getText();
            String s = tmp != null ? tmp.toString().trim() : "";
            if (!isValidWeight(s)) {
                binding.weightLayout.setError(getString(R.string.error_invalid_weight));
                return;
            }
            binding.weightLayout.setError(null);
            double newWeight = Double.parseDouble(s);

            if (!isEdit) {
                create(newWeight, selectedDateIso);
            } else {
                edit(newWeight, selectedDateIso);
            }
        });

        binding.cancelButton.setOnClickListener(v -> dismiss());
    }

    private void create(double weight, String dateIso) {
        viewModel.logNewWeight(weight, dateIso,
                () -> requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show();
                    dismiss();
                }),
                e -> requireActivity().runOnUiThread(() -> {
                    binding.dateText.setError(e.getMessage());
                    Toast.makeText(requireContext(),
                            getString(R.string.generic_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                }));
    }

    private void edit(double weight, String dateIso) {
        viewModel.updateExistingWeight(originalDateIso, weight, dateIso,
                () -> requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show();
                    dismiss();
                }));
    }

    private void openDatePicker() {
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.cd_pick_date)
                .setSelection(parseIsoToUtcMillis(selectedDateIso))
                .setCalendarConstraints(constraints.build())
                .build();

        picker.addOnPositiveButtonClickListener(sel -> {
            selectedDateIso = isoUtc(new Date(sel));
            if (binding != null) {
                binding.dateText.setText(getString(R.string.current_date, selectedDateIso));
            }
        });
        picker.show(getParentFragmentManager(), "date_picker");
    }

    private boolean isValidWeight(String s) {
        try {
            double w = Double.parseDouble(s);
            return w > 0 && w < Utils.MAX_ALLOWED_WEIGHT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static long parseIsoToUtcMillis(String iso) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(iso);
            return d != null ? d.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error during date conversion: " + e.getMessage());
            return System.currentTimeMillis();
        }
    }

    public static DialogWeightEntry newEdit(double currentWeight, @NonNull String originalDateIso) {
        DialogWeightEntry d = new DialogWeightEntry();
        Bundle b = new Bundle();
        b.putDouble(ARG_WEIGHT, currentWeight);
        b.putString(ARG_DATE, originalDateIso);
        d.setArguments(b);
        return d;
    }
}