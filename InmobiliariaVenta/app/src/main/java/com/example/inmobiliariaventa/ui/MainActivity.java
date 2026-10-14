package com.example.inmobiliariaventa.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
        }

        View rootLayout = findViewById(R.id.main);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Soporte global responsive para notch (cámara tipo gota), cutout y barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            int bottomPadding = (bottomNav.getVisibility() == View.GONE) ? insets.bottom : 0;
            v.setPadding(insets.left, insets.top, insets.right, bottomPadding);
            return windowInsets;
        });

        // Encontrar el NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            
            // Vincular el BottomNavigationView con el NavController
            NavigationUI.setupWithNavController(bottomNav, navController);
            
            // Interceptar clicks si el usuario no está registrado
            bottomNav.setOnItemSelectedListener(item -> {
                boolean isUserRegistered = false; // TODO: Cambiar por estado real de autenticación
                
                if (!isUserRegistered && (item.getItemId() == R.id.nav_guardados || item.getItemId() == R.id.nav_perfil)) {
                    navController.navigate(R.id.nav_login);
                    return false; // No marcamos la pestaña como seleccionada visualmente
                }
                
                // Navegación explícita para los botones del menú de administrador
                if (item.getItemId() == R.id.nav_admin_home || 
                    item.getItemId() == R.id.nav_admin_propiedades || 
                    item.getItemId() == R.id.nav_contratos || 
                    item.getItemId() == R.id.nav_admin_perfil) {
                    
                    // Evitar navegar a la misma pantalla si ya estamos en ella
                    if (navController.getCurrentDestination() != null && 
                        navController.getCurrentDestination().getId() != item.getItemId()) {
                        
                        try {
                            navController.navigate(item.getItemId());
                        } catch (IllegalArgumentException e) {
                            // El fragmento aún no existe en el grafo de navegación
                            // No hacemos nada, simplemente evitamos el crasheo
                        }
                    }
                    return true;
                }
                
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.nav_search_location || destination.getId() == R.id.nav_login || destination.getId() == R.id.nav_registro) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
                ViewCompat.requestApplyInsets(rootLayout);

                // Sincronizar el color de la barra de estado y notch con el header de cada ventana
                int destId = destination.getId();
                int topBarColor;
                if (destId == R.id.nav_inicio || destId == R.id.nav_admin_home || destId == R.id.nav_buscar) {
                    // Pantallas con header blanco (layout_header / toolbar blanco)
                    topBarColor = ContextCompat.getColor(this, R.color.colorSurfaceContainerLowest);
                } else if (destId == R.id.nav_login || destId == R.id.nav_registro) {
                    topBarColor = ContextCompat.getColor(this, R.color.colorSurfaceContainerLow);
                } else {
                    // Pantallas con fondo completo colorSurface
                    topBarColor = ContextCompat.getColor(this, R.color.colorSurface);
                }
                rootLayout.setBackgroundColor(topBarColor);
                getWindow().setStatusBarColor(topBarColor);
                
                // Cambiar al menú de administrador si estamos en un fragmento de admin
                if (destination.getId() == R.id.nav_admin_perfil || destination.getId() == R.id.nav_admin_home || 
                    destination.getId() == R.id.nav_admin_propiedades || destination.getId() == R.id.nav_admin_agregar_propiedad || 
                    destination.getId() == R.id.nav_contratos || destination.getId() == R.id.nav_admin_agregar_contrato ||
                    destination.getId() == R.id.nav_admin_configuracion ||
                    destination.getId() == R.id.nav_admin_gestion_usuarios || destination.getId() == R.id.nav_admin_ventas ||
                    destination.getId() == R.id.nav_admin_propiedades_vendidas) {
                    if (bottomNav.getMenu().findItem(R.id.nav_admin_perfil) == null) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.menu_admin);
                    }
                    
                    // Forzar que se marque la pestaña correcta en base al destino actual
                    int menuIdToSelect = destination.getId();
                    if (menuIdToSelect == R.id.nav_admin_agregar_propiedad) {
                        menuIdToSelect = R.id.nav_admin_propiedades;
                    } else if (menuIdToSelect == R.id.nav_admin_agregar_contrato) {
                        menuIdToSelect = R.id.nav_contratos;
                    }
                    if (bottomNav.getMenu().findItem(menuIdToSelect) != null) {
                        bottomNav.getMenu().findItem(menuIdToSelect).setChecked(true);
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
