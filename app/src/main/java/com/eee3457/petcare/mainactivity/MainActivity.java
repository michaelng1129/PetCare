package com.eee3457.petcare.mainactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.eee3457.petcare.R;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);

        // Setup BottomNavigationView and NavController
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Configure AppBar with top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.mainHomeScreen, R.id.mainCareScreen, R.id.mainSettingsScreen).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Listen for destination changes to update UI
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Update toolbar title
            if (toolbarTitle != null) {
                if (destination.getId() == R.id.mainHomeScreen) {
                    toolbarTitle.setText("Home");
                } else if (destination.getId() == R.id.healthTrackerViewPager) {
                    toolbarTitle.setText("Health");
                } else if (destination.getId() == R.id.mainCareScreen) {
                    toolbarTitle.setText("Care");
                } else if (destination.getId() == R.id.mainSettingsScreen) {
                    toolbarTitle.setText("Settings");
                } else {
                    toolbarTitle.setText("PetCare");
                }
            }

            // Control AppBarLayout and BottomNavigationView visibility
            if (destination.getId() == R.id.healthTrackerViewPager) {
                // Hide AppBar and BottomNavigation for HealthTrackerViewPager
                if (appBarLayout != null) {
                    appBarLayout.setVisibility(View.VISIBLE);
                }
                if (bottomNavigationView != null) {
                    bottomNavigationView.setVisibility(View.GONE);
                }
            } else {
                // Show AppBar and BottomNavigation for other fragments
                if (appBarLayout != null) {
                    appBarLayout.setVisibility(View.VISIBLE);
                }
                if (bottomNavigationView != null) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}