package com.example.inmobiliariaventa.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.ContratoRepository;
import com.example.inmobiliariaventa.data.repository.FavoritoRepositoryImpl;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private boolean isUpdatingMenuFromNav = false;
    private View layoutSplashOverlay;
    private TextView splashStatusText;
    private boolean isSplashDismissed = false;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar canal de notificaciones locales
        com.example.inmobiliariaventa.utils.NotificacionDispositivoUtils.crearCanalNotificaciones(this);

        // Iniciar conexión Realtime en caliente con Supabase
        com.example.inmobiliariaventa.data.remote.SupabaseRealtimeManager.getInstance(this).iniciar();

        // Solicitar permisos de inicio (Ubicación y Notificaciones agrupados en Android 13+)
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // El usuario respondió la solicitud de permisos
                }
        );
        solicitarPermisosIniciales();

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

            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean isUserRegistered = prefs.getBoolean("is_user_logged_in", false);
            String userRol = prefs.getString("user_rol", "");
            boolean isAdmin = isUserRegistered && userRol != null && userRol.trim().toUpperCase().contains("ADMIN");

            if (savedInstanceState == null && isAdmin) {
                bottomNav.getMenu().clear();
                bottomNav.inflateMenu(R.menu.menu_admin);
                NavGraph graph = navController.getNavInflater().inflate(R.navigation.mobile_navigation);
                graph.setStartDestination(R.id.nav_admin_home);
                navController.setGraph(graph);
            }
            
            // Interceptar clicks en la barra inferior con navegación consistente y controlada
            bottomNav.setOnItemSelectedListener(item -> {
                if (isUpdatingMenuFromNav) {
                    return true;
                }

                int itemId = item.getItemId();
                android.content.SharedPreferences navPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                boolean loggedIn = navPrefs.getBoolean("is_user_logged_in", false);
                
                if (!loggedIn && (itemId == R.id.nav_guardados || itemId == R.id.nav_perfil)) {
                    navController.navigate(R.id.nav_login);
                    return false; // No marcamos la pestaña como seleccionada si requiere autenticación
                }
                
                navegarADestinoMenu(navController, itemId);
                return true;
            });

            // Si el usuario toca una pestaña que ya estaba marcada como seleccionada en la barra
            bottomNav.setOnItemReselectedListener(item -> {
                int itemId = item.getItemId();
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getId() != itemId) {
                    // El usuario tocó la pestaña pero la pantalla actual no coincide (ej. estaba en favoritos o en una subpantalla)
                    navegarADestinoMenu(navController, itemId);
                } else if (itemId == R.id.nav_inicio) {
                    navController.popBackStack(R.id.nav_inicio, false);
                } else if (itemId == R.id.nav_admin_home) {
                    navController.popBackStack(R.id.nav_admin_home, false);
                }
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.nav_search_location || destId == R.id.nav_login || destId == R.id.nav_registro) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
                ViewCompat.requestApplyInsets(rootLayout);

                // Sincronizar el color de la barra de estado y notch con el header de cada ventana
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
                
                // Determinar si estamos en modo admin o usuario
                boolean isAdminDest = (destId == R.id.nav_admin_perfil || destId == R.id.nav_admin_home || 
                    destId == R.id.nav_admin_propiedades || destId == R.id.nav_admin_agregar_propiedad || 
                    destId == R.id.nav_contratos || destId == R.id.nav_admin_agregar_contrato ||
                    destId == R.id.nav_admin_configuracion ||
                    destId == R.id.nav_admin_gestion_usuarios || destId == R.id.nav_admin_ventas ||
                    destId == R.id.nav_admin_propiedades_vendidas || destId == R.id.nav_admin_notificaciones ||
                    destId == R.id.nav_admin_mensajes);

                if (isAdminDest) {
                    if (bottomNav.getMenu().findItem(R.id.nav_admin_perfil) == null) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.menu_admin);
                    }
                    
                    int menuIdToSelect = destId;
                    if (menuIdToSelect == R.id.nav_admin_agregar_propiedad) {
                        menuIdToSelect = R.id.nav_admin_propiedades;
                    } else if (menuIdToSelect == R.id.nav_admin_agregar_contrato) {
                        menuIdToSelect = R.id.nav_contratos;
                    }

                    if (bottomNav.getMenu().findItem(menuIdToSelect) != null) {
                        if (bottomNav.getSelectedItemId() != menuIdToSelect) {
                            isUpdatingMenuFromNav = true;
                            bottomNav.setSelectedItemId(menuIdToSelect);
                            isUpdatingMenuFromNav = false;
                        }
                    }
                } else {
                    if (bottomNav.getMenu().findItem(R.id.nav_perfil) == null) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.menu_usuario);
                    }

                    int menuIdToSelect = destId;
                    if (menuIdToSelect == R.id.nav_resultados_busqueda) {
                        menuIdToSelect = R.id.nav_buscar;
                    } else if (menuIdToSelect == R.id.nav_detalle_propiedad) {
                        menuIdToSelect = R.id.nav_inicio;
                    }

                    if (bottomNav.getMenu().findItem(menuIdToSelect) != null) {
                        if (bottomNav.getSelectedItemId() != menuIdToSelect) {
                            isUpdatingMenuFromNav = true;
                            bottomNav.setSelectedItemId(menuIdToSelect);
                            isUpdatingMenuFromNav = false;
                        }
                    }
                }
            });
        }

        // Sincronizar en segundo plano cualquier usuario local pendiente de subir a Supabase
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Usuario> usuariosLocales = AppDatabase.getInstance(getApplicationContext())
                        .usuarioDao().obtenerTodosDirecto();
                if (usuariosLocales != null) {
                    for (Usuario u : usuariosLocales) {
                        if (u.email != null && !u.email.isEmpty()) {
                            SupabaseClient.obtenerUsuarioPorEmail(u.email, new SupabaseClient.SupabaseCallback<Usuario>() {
                                @Override
                                public void onSuccess(Usuario remoto) {
                                    if (remoto == null) {
                                        SupabaseClient.registrarUsuario(u, new SupabaseClient.SupabaseCallback<Boolean>() {
                                            @Override
                                            public void onSuccess(Boolean result) {
                                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                                    u.estadoSincronizacion = "SINCRONIZADO";
                                                    AppDatabase.getInstance(getApplicationContext()).usuarioDao().actualizar(u);
                                                });
                                            }

                                            @Override
                                            public void onError(Exception error) {}
                                        });
                                    }
                                }

                                @Override
                                public void onError(Exception error) {}
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Sincronizar favoritos del usuario activo si tiene sesión iniciada
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                String userId = prefs.getString("user_id", "");
                if (prefs.getBoolean("is_user_logged_in", false) && !userId.isEmpty()) {
                    FavoritoRepositoryImpl.getInstance(getApplicationContext()).sincronizarConNube(userId, null);
                }
            } catch (Exception ignored) {}

            // Sincronizar mensajes de contacto con Supabase
            try {
                com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl.getInstance(getApplicationContext())
                        .sincronizarConNube(null);
            } catch (Exception ignored) {}

            // Sincronizar contratos al inicio únicamente si el usuario autenticado tiene rol de "Admin"
            try {
                ContratoRepository.getInstance(getApplicationContext())
                        .sincronizarAlIniciarSiEsAdmin();
            } catch (Exception ignored) {}
        });

        rootLayout.post(() -> procesarIntentNotificacion(getIntent()));

        layoutSplashOverlay = findViewById(R.id.layout_splash_overlay);
        splashStatusText = findViewById(R.id.splash_status_text);
        iniciarCargaCompletaConReloj();
    }

    private void iniciarCargaCompletaConReloj() {
        if (layoutSplashOverlay == null) return;

        long startTime = System.currentTimeMillis();
        final long MIN_SPLASH_TIME = 900; // Tiempo mínimo para apreciar la animación de inicio suave
        final long MAX_SPLASH_TIMEOUT = 3500; // Límite máximo de espera de seguridad

        // Watchdog de seguridad: Si en 3.5 segundos la conexión es lenta, entra a la app con caché local
        splashHandler.postDelayed(() -> {
            ocultarRelojDeCarga("¡Bienvenido a HomeFinder!");
        }, MAX_SPLASH_TIMEOUT);

        boolean isOnline = com.example.inmobiliariaventa.utils.NetworkUtils.isNetworkAvailable(this);

        if (!isOnline) {
            if (splashStatusText != null) {
                splashStatusText.setText("Modo sin conexión • Cargando datos locales...");
            }
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    AppDatabase.getInstance(getApplicationContext()).propiedadDao().obtenerTodasSync();
                } catch (Exception ignored) {}

                long elapsed = System.currentTimeMillis() - startTime;
                long delay = Math.max(0, 500 - elapsed);
                splashHandler.postDelayed(() -> {
                    ocultarRelojDeCarga("Modo sin conexión listo");
                }, delay);
            });
            return;
        }

        // Modo Online: Sincronizar catálogo y precargar portadas de imágenes en memoria
        if (splashStatusText != null) {
            splashStatusText.setText("Sincronizando catálogo de propiedades...");
        }

        new com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl(getApplication())
                .sincronizarConNube(() -> {
                    splashHandler.post(() -> {
                        if (splashStatusText != null && !isSplashDismissed) {
                            splashStatusText.setText("Preparando imágenes y favoritos...");
                        }
                    });

                    // Precargar portadas de las primeras propiedades para que se muestren de inmediato
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            List<com.example.inmobiliariaventa.domain.entity.Propiedad> props =
                                    AppDatabase.getInstance(getApplicationContext()).propiedadDao().obtenerTodasSync();
                            if (props != null) {
                                int count = 0;
                                for (com.example.inmobiliariaventa.domain.entity.Propiedad p : props) {
                                    if (p != null && p.imagenes != null && !p.imagenes.isEmpty()) {
                                        String img = p.imagenes.get(0);
                                        if (img != null && !img.trim().isEmpty()) {
                                            com.example.inmobiliariaventa.utils.ImageLoader.preload(getApplicationContext(), img);
                                            count++;
                                            if (count >= 8) break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        long elapsed = System.currentTimeMillis() - startTime;
                        long remainingDelay = Math.max(0, MIN_SPLASH_TIME - elapsed);
                        splashHandler.postDelayed(() -> {
                            ocultarRelojDeCarga("¡Todo listo!");
                        }, remainingDelay);
                    });
                });
    }

    private void ocultarRelojDeCarga(String mensajeFinal) {
        if (isSplashDismissed || layoutSplashOverlay == null) return;
        isSplashDismissed = true;

        if (splashStatusText != null && mensajeFinal != null) {
            splashStatusText.setText(mensajeFinal);
        }

        // Animación suave de desvanecimiento (Fade Out)
        layoutSplashOverlay.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> {
                    if (layoutSplashOverlay != null) {
                        layoutSplashOverlay.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void solicitarPermisosIniciales() {
        List<String> permisosPendientes = new ArrayList<>();

        boolean faltaFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean faltaCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (faltaFine && faltaCoarse) {
            permisosPendientes.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permisosPendientes.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permisosPendientes.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permisosPendientes.isEmpty()) {
            locationPermissionLauncher.launch(permisosPendientes.toArray(new String[0]));
        }
    }

    private void navegarADestinoMenu(NavController navController, int itemId) {
        if (navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() == itemId) {
            return;
        }

        NavOptions navOptions = new NavOptions.Builder()
                .setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
                .setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
                .setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
                .setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)
                .build();

        // Si el destino es inicio o nav_admin_home, siempre hacer pop hasta la raíz correspondiente
        if (itemId == R.id.nav_inicio) {
            if (!navController.popBackStack(R.id.nav_inicio, false)) {
                try {
                    navController.navigate(R.id.nav_inicio, null, navOptions);
                } catch (Exception ignored) {}
            }
            return;
        }
        if (itemId == R.id.nav_admin_home) {
            if (!navController.popBackStack(R.id.nav_admin_home, false)) {
                try {
                    navController.navigate(R.id.nav_admin_home, null, navOptions);
                } catch (Exception ignored) {}
            }
            return;
        }

        // Para las demás pestañas (Favoritos, Buscar, Perfil): navegar con animación fluida
        if (!navController.popBackStack(itemId, false)) {
            try {
                navController.navigate(itemId, null, navOptions);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        procesarIntentNotificacion(intent);
    }

    private void procesarIntentNotificacion(android.content.Intent intent) {
        if (intent == null) return;
        String propId = intent.getStringExtra("extra_propiedad_id");
        String destino = intent.getStringExtra("extra_destino");

        if (propId == null && destino == null) return;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;
        NavController navController = navHostFragment.getNavController();

        if (propId != null && !propId.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("propiedad_id", propId);
            try {
                navController.navigate(R.id.nav_detalle_propiedad, bundle);
            } catch (Exception ignored) {}
            intent.removeExtra("extra_propiedad_id");
        } else if ("admin_notificaciones".equals(destino)) {
            try {
                navController.navigate(R.id.nav_admin_notificaciones);
            } catch (Exception ignored) {}
            intent.removeExtra("extra_destino");
        } else if ("cliente_notificaciones".equals(destino)) {
            try {
                navController.navigate(R.id.nav_cliente_notificaciones);
            } catch (Exception ignored) {}
            intent.removeExtra("extra_destino");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.example.inmobiliariaventa.data.remote.SupabaseRealtimeManager.getInstance(this).detener();
    }
}
