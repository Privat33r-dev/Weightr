package com.snhu.weightr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.db.dao.DailyWeightDao;
import com.snhu.weightr.data.db.entity.DailyWeightEntity;
import com.snhu.weightr.data.repo.DailyWeightRepository;
import com.snhu.weightr.data.session.SessionStore;

import java.util.Locale;
import java.util.Objects;

public final class HistoryFragment extends Fragment {

    private RecyclerView rv;
    private HistoryAdapter adapter;
    private DailyWeightRepository repo;
    private Long userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        rv = v.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(this::onDelete);
        rv.setAdapter(adapter);

        DailyWeightDao dao = WeightrDb.get(requireContext()).dailyWeightDao();
        repo = new DailyWeightRepository(dao);

        long uid = SessionStore.get(requireContext()).userId();
        userId = (uid > 0) ? uid : null;

        if (userId == null) {
            Toast.makeText(requireContext(), R.string.error_no_user, Toast.LENGTH_SHORT).show();
            return;
        }

        loadData();
    }

    private void loadData() {
        repo.listWeightsForUser(userId, list ->
                requireActivity().runOnUiThread(() -> adapter.submitList(list))
        );
    }

    private void onDelete(@NonNull DailyWeightEntity item) {
        repo.deleteWeight(item.id, () -> requireActivity().runOnUiThread(() -> {
            // TODO: something
            loadData();
        }));
    }

    private static final class HistoryAdapter
            extends ListAdapter<DailyWeightEntity, HistoryAdapter.VH> {

        interface OnDeleteClick {
            void onDelete(DailyWeightEntity item);
        }

        private final OnDeleteClick onDelete;

        HistoryAdapter(OnDeleteClick onDelete) {
            super(DIFF);
            this.onDelete = onDelete;
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
            h.bind(item, onDelete);
        }

        static final DiffUtil.ItemCallback<DailyWeightEntity> DIFF =
                new DiffUtil.ItemCallback<DailyWeightEntity>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull DailyWeightEntity a,
                                                   @NonNull DailyWeightEntity b) {
                        // stable ID if you have one
                        return a.id == b.id;
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull DailyWeightEntity a,
                                                      @NonNull DailyWeightEntity b) {
                        return a.weight == b.weight
                                && safeEq(a.date, b.date)
                                && safeEq(a.userId, b.userId);
                    }

                    private boolean safeEq(Object x, Object y) {
                        return Objects.equals(x, y);
                    }
                };

        static final class VH extends RecyclerView.ViewHolder {
            private final android.widget.TextView dateTv;
            private final android.widget.TextView weightTv;
            private final android.widget.ImageButton deleteBtn;

            VH(@NonNull View itemView) {
                super(itemView);
                dateTv = itemView.findViewById(R.id.date_tv);
                weightTv = itemView.findViewById(R.id.weight_tv);
                deleteBtn = itemView.findViewById(R.id.delete_btn);
            }

            void bind(@NonNull DailyWeightEntity item, @NonNull OnDeleteClick onDelete) {
                dateTv.setText(item.date);
                // show with unit; adapt if you use kg in strings
                weightTv.setText(String.format(Locale.US, "%.1f kg", item.weight));
                deleteBtn.setOnClickListener(v -> onDelete.onDelete(item));
            }
        }
    }
}
