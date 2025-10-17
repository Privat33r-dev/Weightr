package com.snhu.weightr;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.databinding.ActivityMainBinding;
import com.snhu.weightr.ui.dialogs.DialogWeightEntry;
import com.snhu.weightr.ui.login.LoginActivity;
import com.snhu.weightr.ui.viewmodel.MainViewModel;


public final class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainViewModel vm = new ViewModelProvider(this).get(MainViewModel.class);
        vm.init(this);

        Long userId = vm.getUserId() != null ? vm.getUserId().getValue() : null;
        if (userId == null || userId <= 0) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

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
                showNewWeightDialog();
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


    private void showNewWeightDialog() {
        DialogWeightEntry dialog = new DialogWeightEntry();
        dialog.show(getSupportFragmentManager(), "WeightEntryDialog");
    }
}
