package com.example.inmobiliariaventa.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.inmobiliariaventa.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Encontrar el NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            
            // Vincular el BottomNavigationView con el NavController
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
            
            // Interceptar clicks si el usuario no está registrado
            bottomNav.setOnItemSelectedListener(item -> {
                boolean isUserRegistered = false; // TODO: Cambiar por estado real de autenticación
                
                if (!isUserRegistered && (item.getItemId() == R.id.nav_guardados || item.getItemId() == R.id.nav_perfil)) {
                    navController.navigate(R.id.nav_login);
                    return false; // No marcamos la pestaña como seleccionada visualmente
                }
                
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.nav_search_location || destination.getId() == R.id.nav_login || destination.getId() == R.id.nav_registro) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
// Testing BOM
