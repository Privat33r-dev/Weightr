package com.snhu.weightr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.databinding.ActivityMainBinding;
import com.snhu.weightr.ui.dialogs.DialogGoalEntry;
import com.snhu.weightr.ui.dialogs.DialogWeightEntry;
import com.snhu.weightr.ui.login.LoginActivity;
import com.snhu.weightr.ui.viewmodel.MainViewModel;
import com.snhu.weightr.util.LocalNotifier;


public final class MainActivity extends AppCompatActivity {

    private static final int ID_CONGRATS = 123;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser currentUser;

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        authStateListener = firebaseAuth -> {
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) {
                sendToLogin();
            }
        };

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportFragmentManager().setFragmentResultListener(
                "notifications_perm_result",
                this,
                (key, bundle) -> {
                    boolean granted = bundle.getBoolean("granted", false);
                    if (granted) {
                        Toast.makeText(this, "Thank you for your trust!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

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

        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.action_goal) {
                    showGoalWeightDialog();
                    return true;
                } else if (id == R.id.action_logout) {
                    // Clear session
                    SessionStore.get(getBaseContext()).reset();
                    mAuth.signOut();
                    return true;
                }

                return MainActivity.super.onOptionsItemSelected(item);
            }
        });

        // Floating Action Button -> Log weight dialog
        binding.fab.setOnClickListener(v -> showNewWeightDialog());
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
        });
    }

    private void congratulateUser(@NonNull Activity activity, double currentWeight) {
        // Compose the message
        String body = activity.getString(R.string.goal_reached, currentWeight);
        // Local notification (fallbacks to Toast if lacks permissions for proper notification)
        LocalNotifier.notify(
                activity,
                ID_CONGRATS,
                activity.getString(R.string.app_name),
                body
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }

    private void sendToLogin() {
        Intent i = new Intent(getBaseContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }


}
