package com.snhu.weightr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.data.settings.SettingsStore;
import com.snhu.weightr.databinding.ActivityMainBinding;
import com.snhu.weightr.ui.dialogs.DialogGoalEntry;
import com.snhu.weightr.ui.dialogs.DialogWeightEntry;
import com.snhu.weightr.ui.dialogs.SmsDialog;
import com.snhu.weightr.ui.login.LoginActivity;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.LocalNotifier;
import com.snhu.weightr.util.SmsNotifier;
import com.snhu.weightr.util.Utils;

import java.util.Date;


public final class MainActivity extends AppCompatActivity {

    private static final int ID_CONGRATS = 123;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.init(this);

        Long userId = viewModel.getUserId() != null ? viewModel.getUserId().getValue() : null;
        if (userId == null || userId <= 0) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportFragmentManager().setFragmentResultListener(
                "sms_perm_result",
                this,
                (key, bundle) -> {
                    boolean granted = bundle.getBoolean("granted", false);
                }
        );

        // On each start/login
        // TODO: allow to stop (maybe 1-5 days inactive)
        enableSmsIfNeeded();
        attachGoalObserver();


        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) throw new IllegalStateException("NavHostFragment missing");

        NavController navController = navHostFragment.getNavController();

        // Build the app bar configuration normally
        appBarConfiguration = new AppBarConfiguration.Builder(R.id.MainFragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Dynamically control nav icon (History vs Up arrow)
        navController.addOnDestinationChangedListener((controller, dest, args) -> {
            if (dest.getId() == R.id.MainFragment) {
                // Show custom History icon on the top-level screen
                binding.toolbar.setNavigationIcon(R.drawable.ic_history);
                binding.toolbar.setNavigationContentDescription(R.string.navigate_to_history);
                binding.toolbar.setNavigationOnClickListener(v ->
                        navController.navigate(R.id.action_MainFragment_to_HistoryFragment));
            } else {
                binding.toolbar.setNavigationIcon(R.drawable.ic_back);
                binding.toolbar.setNavigationOnClickListener(v ->
                        navController.navigate(R.id.action_HistoryFragment_to_MainFragment));
            }
        });

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_goal) {
                showGoalWeightDialog();
                return true;
            } else if (id == R.id.action_logout) {
                // Clear session and return to login
                SessionStore.get(this).reset();
                Intent i = new Intent(this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
                return true;
            }

            return super.onOptionsItemSelected(item);
        });

        // Floating Action Button -> Log weight dialog
        binding.fab.setOnClickListener(v -> {
            showNewWeightDialog();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showGoalWeightDialog() {
        DialogGoalEntry dialog = new DialogGoalEntry();
        dialog.show(getSupportFragmentManager(), "GoalEntryDialog");
    }

    private void showNewWeightDialog() {
        DialogWeightEntry dialog = new DialogWeightEntry();
        dialog.show(getSupportFragmentManager(), "WeightEntryDialog");
    }

    private void attachGoalObserver() {
        viewModel.getCurrentWeight().observe(this, w -> {
            if (w == null) return;

            Double start = viewModel.getGoalStartWeight().getValue();
            Double goal = viewModel.getGoalWeight().getValue();
            if (start == null || goal == null) return;

            boolean goingDown = goal < start;                // losing vs gaining
            boolean reached = goingDown ? (w <= goal)      // reached in the correct direction
                    : (w >= goal);
            if (!reached) return;

            congratulateUser(this, w);
            // Reset goal weight to avoid redundant notifications on each state update/activity load
            viewModel.deleteGoalWeight();

            String todayIso = Utils.isoUtc(new Date());
            SettingsStore settings = SettingsStore.get(this);
            settings.setLastCongratsDate(todayIso);
        });
    }

    private void congratulateUser(@NonNull Activity activity, double currentWeight) {
        // Compose the message
        String body = activity.getString(R.string.sms_goal_reached, currentWeight);

        // SmsNotifier returns true on send; false otherwise
        SmsNotifier.sendAlert(activity, body,
                (sent, err) -> {
                    if (sent) return;

                    Toast.makeText(activity, getString(R.string.sms_error, err), Toast.LENGTH_SHORT).show();

                    // Fallback: local notification (works even if SMS is denied/unavailable)
                    LocalNotifier.notify(
                            activity,
                            ID_CONGRATS,
                            activity.getString(R.string.app_name),
                            body
                    );
                }
        );
    }


    private void enableSmsIfNeeded() {
        if (SettingsStore.get(this).isSmsEnabled()) return;

        if (!Utils.hasAllSmsPermissions(this)) {
            new SmsDialog().show(getSupportFragmentManager(), "SmsDialog");
        }
    }

}
