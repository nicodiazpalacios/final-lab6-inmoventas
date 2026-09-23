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
                
                // Cambiar al menú de administrador si estamos en un fragmento de admin
                if (destination.getId() == R.id.nav_admin_perfil || destination.getId() == R.id.nav_admin_home || 
                    destination.getId() == R.id.nav_propiedades || destination.getId() == R.id.nav_contratos) {
                    if (bottomNav.getMenu().findItem(R.id.nav_admin_perfil) == null) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.menu_admin);
                        // Forzar que se marque la pestaña correcta en base al destino actual
                        if (bottomNav.getMenu().findItem(destination.getId()) != null) {
                            bottomNav.getMenu().findItem(destination.getId()).setChecked(true);
                        }
                    } else {
                        if (bottomNav.getMenu().findItem(destination.getId()) != null) {
                            bottomNav.getMenu().findItem(destination.getId()).setChecked(true);
                        }
                    }
                } 
                // Volver al menú de usuario si estamos en el perfil normal o inicio
                else if (destination.getId() == R.id.nav_perfil || destination.getId() == R.id.nav_guardados || destination.getId() == R.id.nav_inicio) {
                    if (bottomNav.getMenu().findItem(R.id.nav_perfil) == null) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.menu_usuario);
                        if (bottomNav.getMenu().findItem(destination.getId()) != null) {
                            bottomNav.getMenu().findItem(destination.getId()).setChecked(true);
                        }
                    } else {
                        if (bottomNav.getMenu().findItem(destination.getId()) != null) {
                            bottomNav.getMenu().findItem(destination.getId()).setChecked(true);
                        }
                    }
                }

                // Forzar que "Inicio" se marque en azul al estar en el detalle
                if (destination.getId() == R.id.nav_detalle_propiedad) {
                    if (bottomNav.getMenu().findItem(R.id.nav_inicio) != null) {
                        bottomNav.getMenu().findItem(R.id.nav_inicio).setChecked(true);
                    }
                }
            });
        }
    }
}
// Testing BOM
