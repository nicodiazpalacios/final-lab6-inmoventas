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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeUsuarioFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private RecyclerView featuredRecyclerView;
    private LinearLayout searchContainer;

    private androidx.core.widget.NestedScrollView homeNestedScrollView;
    private PropiedadDestacadaAdapter featuredAdapter;
    private List<Propiedad> listaCompletaHome = new ArrayList<>();
    private int limiteActualHome = 20;
    private static final int BATCH_SIZE = 20;

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
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> {
                if (getContext() == null) return;
                SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
                if (isLoggedIn) {
                    String userRol = prefs.getString("user_rol", "");
                    if (userRol != null && userRol.trim().toUpperCase().contains("ADMIN")) {
                        Navigation.findNavController(v).navigate(R.id.nav_admin_notificaciones);
                    } else {
                        Toast.makeText(getContext(), "No tienes nuevas notificaciones por el momento", Toast.LENGTH_SHORT).show();
                    }
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
        featuredRecyclerView.setHasFixedSize(true);
        featuredRecyclerView.setItemViewCacheSize(20);
        featuredRecyclerView.setAdapter(featuredAdapter);

        // Desplazamiento automático fluido sin botones
        if (homeNestedScrollView != null) {
            homeNestedScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (v.getChildAt(0) != null && scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 800)) {
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

        // Limpiar observador anterior para evitar fugas o listas cruzadas
        if (liveDataPropiedadesActual != null && observerPropiedades != null) {
            liveDataPropiedadesActual.removeObserver(observerPropiedades);
        }

        // Si se restablece la conexión tras estar desconectado, notificar al usuario
        if (ultimoEstadoConexion != null && !ultimoEstadoConexion && isOnline) {
            mostrarSnackbarReconexion(rootView);
            if (viewModel != null) {
                viewModel.sincronizarConNube(null);
            }
        }
        ultimoEstadoConexion = isOnline;

        if (isOnline) {
            // ==================== MODO ONLINE ====================
            if (cardModoOffline != null) cardModoOffline.setVisibility(View.GONE);
            if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.GONE);
            if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.VISIBLE);
            if (tvTituloPropiedadesHome != null) tvTituloPropiedadesHome.setText("Propiedades");

            observerPropiedades = propiedades -> {
                if (Boolean.TRUE.equals(ultimoEstadoConexion)) {
                    if (propiedades != null) {
                        listaCompletaHome = propiedades;
                        limiteActualHome = Math.min(BATCH_SIZE, listaCompletaHome.size());
                        featuredAdapter.setPropiedades(new ArrayList<>(listaCompletaHome.subList(0, limiteActualHome)));
                    }
                }
            };

            liveDataPropiedadesActual = viewModel.getAllPropiedades();
            liveDataPropiedadesActual.observe(getViewLifecycleOwner(), observerPropiedades);

        } else {
            // ==================== MODO OFFLINE (CATÁLOGO COMPLETO ROOM) ====================
            if (cardModoOffline != null) cardModoOffline.setVisibility(View.VISIBLE);
            if (tvTituloPropiedadesHome != null) tvTituloPropiedadesHome.setText("Propiedades guardadas (Modo sin conexión)");

            observerPropiedades = propiedades -> {
                if (Boolean.FALSE.equals(ultimoEstadoConexion)) {
                    if (propiedades == null || propiedades.isEmpty()) {
                        if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.GONE);
                        if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.VISIBLE);
                        if (tvOfflineVacioTitulo != null) tvOfflineVacioTitulo.setText("Sin propiedades guardadas");
                        if (tvOfflineVacioMensaje != null) tvOfflineVacioMensaje.setText("Aún no hay propiedades almacenadas en este dispositivo. Conéctate a internet para sincronizar el catálogo.");
                        listaCompletaHome = new ArrayList<>();
                        featuredAdapter.setPropiedades(new ArrayList<>());
                    } else {
                        if (layoutOfflineVacio != null) layoutOfflineVacio.setVisibility(View.GONE);
                        if (featuredRecyclerView != null) featuredRecyclerView.setVisibility(View.VISIBLE);
                        listaCompletaHome = propiedades;
                        limiteActualHome = Math.min(BATCH_SIZE, listaCompletaHome.size());
                        featuredAdapter.setPropiedades(new ArrayList<>(listaCompletaHome.subList(0, limiteActualHome)));
                    }
                }
            };

            liveDataPropiedadesActual = viewModel.getAllPropiedades();
            liveDataPropiedadesActual.observe(getViewLifecycleOwner(), observerPropiedades);
        }
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
        if (listaCompletaHome == null || limiteActualHome >= listaCompletaHome.size()) return;
        int nuevoLimite = Math.min(limiteActualHome + BATCH_SIZE, listaCompletaHome.size());
        List<Propiedad> lote = new ArrayList<>(listaCompletaHome.subList(limiteActualHome, nuevoLimite));
        limiteActualHome = nuevoLimite;
        featuredAdapter.agregarPropiedades(lote);
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
                    Toast.makeText(context, "Por favor ingresá desde la sección Perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnRegistro != null) {
            btnRegistro.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Navigation.findNavController(anchorView).navigate(R.id.nav_registro);
                } catch (Exception e) {
                    Toast.makeText(context, "Por favor registrate desde la sección Perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
