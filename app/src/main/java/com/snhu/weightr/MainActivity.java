package com.snhu.weightr;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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

        long userId = vm.getUserId().getValue();
        String userName = vm.getUserName().getValue();

        if (userId <= 0) {
            // session expired or missing → redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) throw new IllegalStateException("NavHostFragment missing");

        NavController navController = navHostFragment.getNavController();

        // Top-level destinations (no Up arrow there)
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.MainFragment
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Toolbar menu clicks (e.g., settings)
        binding.toolbar.setOnMenuItemClickListener(item -> {
//            if (item.getItemId() == R.id.action_settings) {
//                navController.navigate(R.id.settingsFragment);
//                return true;
//            }
            return false;
        });

        // Toolbar navigation icon -> History
        binding.toolbar.setNavigationOnClickListener(v ->
                navController.navigate(R.id.HistoryFragment));

        // FAB -> Log weight
        binding.fab.setOnClickListener(v -> {
            DialogWeightEntry dialog = new DialogWeightEntry();
            dialog.show(getSupportFragmentManager(), "WeightEntryDialog");
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
