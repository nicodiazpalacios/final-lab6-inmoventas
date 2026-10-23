package com.example.inmobiliariaventa.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class HomeUsuarioFragment extends Fragment {

    private androidx.core.widget.NestedScrollView homeNestedScrollView;
    private RecyclerView categoriesRecyclerView;
    private RecyclerView featuredRecyclerView;
    private LinearLayout searchContainer;

    private boolean isCargandoMas = false;
    private PropiedadDestacadaAdapter featuredAdapter;
    private List<Propiedad> listaCompletaHome = new ArrayList<>();
    private int limiteActualHome = 10;
    private static final int BATCH_SIZE = 10;

    private MaterialCardView cardModoOffline;
    private LinearLayout layoutOfflineVacio;
    private TextView tvOfflineVacioTitulo;
    private TextView tvOfflineVacioMensaje;
    private TextView tvTituloPropiedadesHome;

    private PropiedadViewModel viewModel;
    private LiveData<List<Propiedad>> liveDataPropiedadesActual;
    private androidx.lifecycle.Observer<List<Propiedad>> observerPropiedades;
    private Boolean ultimoEstadoConexion = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_usuario, container, false);

                homeNestedScrollView = view.findViewById(R.id.homeNestedScrollView);
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        featuredRecyclerView = view.findViewById(R.id.featuredRecyclerView);
        searchContainer = view.findViewById(R.id.searchContainer);

        cardModoOffline = view.findViewById(R.id.cardModoOffline);
        layoutOfflineVacio = view.findViewById(R.id.layoutOfflineVacio);
        tvOfflineVacioTitulo = view.findViewById(R.id.tvOfflineVacioTitulo);
        tvOfflineVacioMensaje = view.findViewById(R.id.tvOfflineVacioMensaje);
        tvTituloPropiedadesHome = view.findViewById(R.id.tvTituloPropiedadesHome);

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PropiedadViewModel.class);

        searchContainer.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_inicio_to_nav_search_location);
        });

        MaterialButton filterButton = view.findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_buscar);
            } else {
                Navigation.findNavController(v).navigate(R.id.nav_buscar);
            }
        });

        // Configurar botón de notificaciones en la cabecera
        View notificationsButton = view.findViewById(R.id.notificationsButton);
        View badgeNotificationDot = view.findViewById(R.id.badgeNotificationDot);
        if (badgeNotificationDot != null && getContext() != null) {
            com.example.inmobiliariaventa.utils.NotificacionBadgeHelper.configurarObservadorBadge(
                    getViewLifecycleOwner(), requireContext(), badgeNotificationDot);
            com.example.inmobiliariaventa.utils.NotificacionBadgeHelper.sembrarNotificacionesClienteAsync(requireContext());
        }
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> {
                if (getContext() == null) return;
                SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
                if (isLoggedIn) {
                    // En modo vista de usuario, la campanita siempre abre el centro de notificaciones de clientes
                    Navigation.findNavController(v).navigate(R.id.nav_cliente_notificaciones);
                } else {
                    mostrarDialogoRequerirLogin(getContext(), v);
                }
            });
        }

        MaterialButton btnAdminMode = view.findViewById(R.id.btnAdminMode);
        if (btnAdminMode != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
            String userRol = prefs.getString("user_rol", "");

            if (isLoggedIn && userRol != null && userRol.trim().toUpperCase().contains("ADMIN")) {
                btnAdminMode.setVisibility(View.VISIBLE);
                btnAdminMode.setOnClickListener(v -> {
                    Navigation.findNavController(v).navigate(R.id.nav_admin_home);
                });
            } else {
                btnAdminMode.setVisibility(View.GONE);
            }
        }

        setupCategories();
        setupFeaturedProperties(view);
        configurarObservadorRed(view);

        return view;
    }

    private void setupCategories() {
        List<CategoriaAdapter.Categoria> categorias = new ArrayList<>();
        categorias.add(new CategoriaAdapter.Categoria("Casa", R.drawable.ic_home));
        categorias.add(new CategoriaAdapter.Categoria("Depto", R.drawable.ic_propiedades));

        CategoriaAdapter adapter = new CategoriaAdapter(categorias, cat -> {
            Bundle bundle = new Bundle();
            String tipo = "Casa".equalsIgnoreCase(cat.getNombre()) ? "Casa" : "Departamento";
            bundle.putString("tipo", tipo);
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.action_nav_inicio_to_nav_resultados_busqueda, bundle);
            }
        });
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        categoriesRecyclerView.setAdapter(adapter);
    }

    private void setupFeaturedProperties(View rootView) {
        featuredAdapter = new PropiedadDestacadaAdapter(new ArrayList<>());
        if (rootView != null) {
            featuredAdapter.setCoordinatorLayout(rootView.findViewById(R.id.coordinator_home));
        }
        featuredRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        featuredRecyclerView.setNestedScrollingEnabled(false);
        featuredRecyclerView.setHasFixedSize(false);
        featuredRecyclerView.setItemViewCacheSize(4);
        featuredRecyclerView.setAdapter(featuredAdapter);

        if (homeNestedScrollView != null) {
            homeNestedScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (v.getChildAt(0) != null && !isCargandoMas && scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 350)) {
                    cargarMasPropiedadesHome();
                }
            });
        }
    }

    private void configurarObservadorRed(View rootView) {
        if (getContext() == null) return;

        com.example.inmobiliariaventa.utils.NetworkUtils.getNetworkLiveData(requireContext())
                .observe(getViewLifecycleOwner(), isOnline -> {
                    boolean conexionActiva = Boolean.TRUE.equals(isOnline);
                    actualizarModoConexion(conexionActiva, rootView);
                });
    }

    private void actualizarModoConexion(boolean isOnline, View rootView) {
        if (getContext() == null) return;

        if (liveDataPropiedadesActual != null && observerPropiedades != null) {
            liveDataPropiedadesActual.removeObserver(observerPropiedades);
        }

        if (ultimoEstadoConexion != null && !ultimoEstadoConexion && isOnline) {
            mostrarSnackbarReconexion(rootView);
            if (viewModel != null) {
                viewModel.sincronizarConNube(null);
            }
        }
        ultimoEstadoConexion = isOnline;

        if (isOnline) {
            if (cardModoOffline != null) cardModoOffline.setVisibility(View.GONE);
            if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.GONE);
            if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.VISIBLE);
            if (tvTituloPropiedadesHome != null) tvTituloPropiedadesHome.setText("Propiedades");

            observerPropiedades = propiedades -> {
                if (Boolean.TRUE.equals(ultimoEstadoConexion)) {
                    List<Propiedad> visibles = filtrarPropiedadesVisibles(propiedades);
                    listaCompletaHome = visibles;
                    limiteActualHome = Math.min(BATCH_SIZE, listaCompletaHome.size());
                    featuredAdapter.setPropiedades(new ArrayList<>(listaCompletaHome.subList(0, limiteActualHome)));
                }
            };

            liveDataPropiedadesActual = viewModel.getAllPropiedades();
            liveDataPropiedadesActual.observe(getViewLifecycleOwner(), observerPropiedades);

        } else {
            if (cardModoOffline != null) cardModoOffline.setVisibility(View.VISIBLE);
            if (tvTituloPropiedadesHome != null) tvTituloPropiedadesHome.setText("Propiedades guardadas (Modo sin conexión)");

            observerPropiedades = propiedades -> {
                if (Boolean.FALSE.equals(ultimoEstadoConexion)) {
                    List<Propiedad> visibles = filtrarPropiedadesVisibles(propiedades);
                    if (visibles.isEmpty()) {
                        if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.GONE);
                        if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.VISIBLE);
                        if (tvOfflineVacioTitulo != null) tvOfflineVacioTitulo.setText("Sin propiedades guardadas");
                        if (tvOfflineVacioMensaje != null) tvOfflineVacioMensaje.setText("Aún no hay propiedades almacenadas en este dispositivo. Conéctate a internet para sincronizar el catálogo.");
                        listaCompletaHome = new ArrayList<>();
                        featuredAdapter.setPropiedades(new ArrayList<>());
                    } else {
                        if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.GONE);
                        if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.VISIBLE);
                        listaCompletaHome = visibles;
                        limiteActualHome = Math.min(BATCH_SIZE, listaCompletaHome.size());
                        featuredAdapter.setPropiedades(new ArrayList<>(listaCompletaHome.subList(0, limiteActualHome)));
                    }
                }
            };

            liveDataPropiedadesActual = viewModel.getAllPropiedades();
            liveDataPropiedadesActual.observe(getViewLifecycleOwner(), observerPropiedades);
        }
    }

    private List<Propiedad> filtrarPropiedadesVisibles(List<Propiedad> lista) {
        List<Propiedad> visibles = new ArrayList<>();
        if (lista != null) {
            for (Propiedad p : lista) {
                if (p != null && p.isVisibleParaCliente()) {
                    visibles.add(p);
                }
            }
        }
        return visibles;
    }

    private void mostrarSnackbarReconexion(View rootView) {
        if (rootView == null || getContext() == null) return;
        try {
            View target = rootView.findViewById(R.id.coordinator_home);
            if (target == null) target = rootView;
            com.google.android.material.snackbar.Snackbar snack =
                    com.google.android.material.snackbar.Snackbar.make(target, "Conexión restablecida • Mostrando catálogo completo", com.google.android.material.snackbar.Snackbar.LENGTH_LONG);
            View sView = snack.getView();
            sView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
            sView.setBackgroundTintList(null);
            snack.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white));

            android.view.ViewGroup.LayoutParams lp = sView.getLayoutParams();
            if (lp instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams marginParams = (android.view.ViewGroup.MarginLayoutParams) lp;
                int marginH = (int) (16 * getResources().getDisplayMetrics().density);
                int marginB = (int) (8 * getResources().getDisplayMetrics().density);
                marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
                sView.setLayoutParams(marginParams);
            }
            sView.setElevation(8 * getResources().getDisplayMetrics().density);

            snack.show();
        } catch (Exception ignored) {}
    }

    private void cargarMasPropiedadesHome() {
        if (listaCompletaHome == null || limiteActualHome >= listaCompletaHome.size() || isCargandoMas) return;
        isCargandoMas = true;
        int nuevoLimite = Math.min(limiteActualHome + BATCH_SIZE, listaCompletaHome.size());
        List<Propiedad> lote = new ArrayList<>(listaCompletaHome.subList(limiteActualHome, nuevoLimite));
        limiteActualHome = nuevoLimite;
        featuredAdapter.agregarPropiedades(lote);
        if (homeNestedScrollView != null) {
            homeNestedScrollView.post(() -> isCargandoMas = false);
        } else {
            isCargandoMas = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (featuredAdapter != null && getContext() != null) {
            featuredAdapter.recargarFavoritos(getContext());
        }
        if (getView() != null && getContext() != null) {
            boolean isOnline = com.example.inmobiliariaventa.utils.NetworkUtils.isNetworkAvailable(requireContext());
            if (ultimoEstadoConexion == null || !ultimoEstadoConexion.equals(isOnline)) {
                actualizarModoConexion(isOnline, getView());
            }
        }
    }

    private void mostrarDialogoRequerirLogin(Context context, View anchorView) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_requerir_login, null);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_login_icon);
        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_login_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_login_mensaje);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btn_dialog_login);
        MaterialButton btnRegistro = dialogView.findViewById(R.id.btn_dialog_registro);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_cancelar);

        if (ivIcon != null) {
            ivIcon.setImageResource(R.drawable.ic_notifications);
        }
        if (tvTitulo != null) {
            tvTitulo.setText("Centro de notificaciones");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("Para enterarte de nuevas propiedades, cambios de precio en tus guardados y respuestas a tus consultas, iniciá sesión o creá tu cuenta.");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Navigation.findNavController(anchorView).navigate(R.id.nav_login);
                } catch (Exception e) {
                    com.example.inmobiliariaventa.utils.CartelUtils.mostrar(anchorView, "Por favor ingresá desde la sección Perfil");
                }
            });
        }

        if (btnRegistro != null) {
            btnRegistro.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Navigation.findNavController(anchorView).navigate(R.id.nav_registro);
                } catch (Exception e) {
                    com.example.inmobiliariaventa.utils.CartelUtils.mostrar(anchorView, "Por favor registrate desde la sección Perfil");
                }
            });
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
