package com.example.inmobiliariaventa.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.LinearLayout;
import com.google.android.material.button.MaterialButton;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_usuario, container, false);

        homeNestedScrollView = view.findViewById(R.id.homeNestedScrollView);
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        featuredRecyclerView = view.findViewById(R.id.featuredRecyclerView);

        searchContainer = view.findViewById(R.id.searchContainer);

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

        MaterialButton btnAdminMode = view.findViewById(R.id.btnAdminMode);
        if (btnAdminMode != null) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
            String userRol = prefs.getString("user_rol", "");

            if (isLoggedIn && "ADMIN".equalsIgnoreCase(userRol)) {
                btnAdminMode.setVisibility(View.VISIBLE);
                btnAdminMode.setOnClickListener(v -> {
                    Navigation.findNavController(v).navigate(R.id.nav_admin_home);
                });
            } else {
                btnAdminMode.setVisibility(View.GONE);
            }
        }

        setupCategories();
        setupFeaturedProperties();

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

    private void setupFeaturedProperties() {
        featuredAdapter = new PropiedadDestacadaAdapter(new ArrayList<>());
        featuredRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        featuredRecyclerView.setHasFixedSize(true);
        featuredRecyclerView.setItemViewCacheSize(20);
        featuredRecyclerView.setAdapter(featuredAdapter);

        // Desplazamiento automático fluido sin botones: al acercarse al final del scroll, se cargan más elementos
        if (homeNestedScrollView != null) {
            homeNestedScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (v.getChildAt(0) != null && scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 800)) {
                    cargarMasPropiedadesHome();
                }
            });
        }

        com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel viewModel =
                new androidx.lifecycle.ViewModelProvider(this).get(com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel.class);
        viewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            if (propiedades != null) {
                listaCompletaHome = propiedades;
                limiteActualHome = Math.min(BATCH_SIZE, listaCompletaHome.size());
                featuredAdapter.setPropiedades(new ArrayList<>(listaCompletaHome.subList(0, limiteActualHome)));
            }
        });
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
    }


}
