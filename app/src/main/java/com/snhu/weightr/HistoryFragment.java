package com.snhu.weightr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.data.settings.SettingsStore;
import com.snhu.weightr.ui.dialogs.DialogWeightEntry;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.Utils;

import java.util.Locale;
import java.util.Objects;

public final class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private MainViewModel viewModel;
    private Long userId;
    private boolean sortDescending;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    private void addMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.sort_menu, menu);

                // Update main sort icon
                MenuItem sortItem = menu.findItem(R.id.action_sort);
                if (sortItem != null) {
                    sortItem.setIcon(ContextCompat.getDrawable(requireContext(),
                            sortDescending ? R.drawable.ic_sort_descending : R.drawable.ic_sort_ascending));
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId != R.id.action_sort) {
                    Log.e(TAG, "onMenuItemSelected: invalid itemId chosen" + itemId);
                    return false;
                }
                // Toggle sort direction
                sortDescending = !sortDescending;

                // Save preference and refresh
                SettingsStore.get(requireContext()).setSortDescending(sortDescending);
                viewModel.setSortDescending(sortDescending);
                requireActivity().invalidateMenu();
                return true;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recyclerView = v.findViewById(R.id.rv_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(this::onDelete, this::onEdit);
        recyclerView.setAdapter(adapter);

        long uid = SessionStore.get(requireContext()).userId();
        userId = (uid > 0) ? uid : null;

        if (userId == null) {
            Toast.makeText(requireContext(), R.string.error_no_user, Toast.LENGTH_SHORT).show();
            return;
        }

        // set dynamic updates
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getHistory().observe(getViewLifecycleOwner(), list -> adapter.submitList(list));

        // Load sort preference
        sortDescending = SettingsStore.get(requireContext()).isSortDescending();
        viewModel.setSortDescending(sortDescending);

        addMenu();
    }

    private void onDelete(@NonNull DailyWeightEntity item) {
        viewModel.deleteWeight(item);
    }

    private void onEdit(@NonNull DailyWeightEntity item) {
        DialogWeightEntry dialog = DialogWeightEntry.newEdit(item.id, item.userId, item.weight, item.date);
        dialog.show(getParentFragmentManager(), "WeightEntryDialog");
    }

    private static final class HistoryAdapter
            extends ListAdapter<DailyWeightEntity, HistoryAdapter.VH> {

        interface OnDeleteClick {
            void onDelete(DailyWeightEntity item);
        }

        interface OnEditClick {
            void onEdit(DailyWeightEntity item);
        }

        private final OnDeleteClick onDelete;
        private final OnEditClick onEdit;

        HistoryAdapter(OnDeleteClick onDelete, OnEditClick onEdit) {
            super(DIFF);
            this.onDelete = onDelete;
            this.onEdit = onEdit;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_weight_history, parent, false);
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DailyWeightEntity item = getItem(position);
            h.bind(item, onDelete, onEdit);
        }

        static final DiffUtil.ItemCallback<DailyWeightEntity> DIFF =
                new DiffUtil.ItemCallback<>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull DailyWeightEntity a,
                                                   @NonNull DailyWeightEntity b) {
                        return Objects.equals(a.id, b.id);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull DailyWeightEntity a,
                                                      @NonNull DailyWeightEntity b) {
                        return Utils.approximatelyEqual(a.weight, b.weight)
                                && Objects.equals(a.date, b.date)
                                && Objects.equals(a.userId, b.userId);
                    }
                };

        static final class VH extends RecyclerView.ViewHolder {
            private final android.widget.TextView dateTv;
            private final android.widget.TextView weightTv;
            private final android.widget.ImageButton deleteBtn;
            @Nullable
            private final android.widget.ImageButton editBtn;

            VH(@NonNull View itemView) {
                super(itemView);
                dateTv = itemView.findViewById(R.id.date_tv);
                weightTv = itemView.findViewById(R.id.weight_tv);
                editBtn = itemView.findViewById(R.id.edit_btn);
                deleteBtn = itemView.findViewById(R.id.delete_btn);
            }

            void bind(@NonNull DailyWeightEntity item,
                      @NonNull OnDeleteClick onDelete,
                      @NonNull OnEditClick onEdit) {
                dateTv.setText(item.date);
                weightTv.setText(String.format(Locale.US, "%.1f lbs", item.weight));
                deleteBtn.setOnClickListener(v -> onDelete.onDelete(item));
                if (editBtn != null) editBtn.setOnClickListener(v -> onEdit.onEdit(item));
                itemView.setOnClickListener(v -> onEdit.onEdit(item));
            }
        }
    }
}