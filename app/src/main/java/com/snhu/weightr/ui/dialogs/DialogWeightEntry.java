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
import com.snhu.weightr.R;
import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.repo.DailyWeightRepository;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.databinding.DialogWeightEntryBinding;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.data.db.dao.DailyWeightDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.material.datepicker.MaterialDatePicker;

public final class DialogWeightEntry extends DialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_WEIGHT = "weight";
    private static final String ARG_DATE = "date";

    private DialogWeightEntryBinding binding;
    private MainViewModel viewModel;
    private DailyWeightRepository dailyWeightRepository;

    private boolean isEdit = false;
    private long editId = -1L;
    private long userIdArg = -1L;          // optional, if passed
    private String originalDateIso = null; // when editing
    private String selectedDateIso;        // yyyy-MM-dd

    private static final String TAG = DialogWeightEntry.class.getName();


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogWeightEntryBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        DailyWeightDao dao = WeightrDb.get(requireContext()).dailyWeightDao();
        dailyWeightRepository = new DailyWeightRepository(dao);

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
        if (args == null) {
            isEdit = false;
            editId = -1L;
            userIdArg = -1L;
            originalDateIso = null;
            selectedDateIso = isoUtc(new Date());
            return;
        }
        // If these are present → edit mode
        if (args.containsKey(ARG_USER_ID) && args.containsKey(ARG_WEIGHT) && args.containsKey(ARG_DATE)) {
            isEdit = true;
            editId = args.getLong(ARG_ID, -1L);
            userIdArg = args.getLong(ARG_USER_ID, -1L);
            double w = args.getDouble(ARG_WEIGHT, 0d);
            originalDateIso = args.getString(ARG_DATE);
            selectedDateIso = originalDateIso != null ? originalDateIso : isoUtc(new Date());
            // Pre-fill weight field if > 0
            if (w > 0 && binding != null) {
                binding.weightInput.setText(String.valueOf(w));
            }
        } else {
            isEdit = false;
            editId = -1L;
            userIdArg = -1L;
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

            // Resolve userId: prefer argument if provided; otherwise from ViewModel/Session
            Long uidLive = viewModel.getUserId() != null ? viewModel.getUserId().getValue() : null;
            long uid = (userIdArg > 0) ? userIdArg : (uidLive != null ? uidLive : -1L);
            if (uid <= 0) {
                SessionStore.get(requireContext()).clear();
                Toast.makeText(requireContext(), R.string.error_no_user, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isEdit) {
                create(uid, newWeight, selectedDateIso);
            } else {
                edit(uid, newWeight, selectedDateIso);
            }
        });

        binding.cancelButton.setOnClickListener(v -> dismiss());
    }

    private void create(long uid, double weight, String dateIso) {
        dailyWeightRepository.logWeight(uid, weight, dateIso,
                () -> requireActivity().runOnUiThread(() -> {
                    viewModel.refreshData();
                    Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show();
                    dismiss();
                }),
                e -> requireActivity().runOnUiThread(() -> {
                    binding.dateText.setError(e.getMessage());
                    Toast.makeText(requireContext(),
                            getString(R.string.generic_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                }));
    }

    private void edit(long uid, double weight, String dateIso) {
        dailyWeightRepository.updateWeightById(editId, weight, dateIso,
                () -> requireActivity().runOnUiThread(() -> {
                    viewModel.refreshData();
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
            return w > 0 && w < 2000;
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

    public static DialogWeightEntry newEdit(long entryId, long userId, double weight, @NonNull String dateIso) {
        DialogWeightEntry d = new DialogWeightEntry();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, entryId);
        b.putLong(ARG_USER_ID, userId);
        b.putDouble(ARG_WEIGHT, weight);
        b.putString(ARG_DATE, dateIso);
        d.setArguments(b);
        return d;
    }
}