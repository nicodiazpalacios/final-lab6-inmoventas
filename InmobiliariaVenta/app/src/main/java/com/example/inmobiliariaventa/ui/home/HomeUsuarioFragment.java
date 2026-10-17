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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_usuario, container, false);

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

        android.widget.TextView viewAllFeaturedTextView = view.findViewById(R.id.viewAllFeaturedTextView);
        if (viewAllFeaturedTextView != null) {
            viewAllFeaturedTextView.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_nav_inicio_to_nav_resultados_busqueda);
            });
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
        PropiedadDestacadaAdapter adapter = new PropiedadDestacadaAdapter(new ArrayList<>());
        featuredRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        featuredRecyclerView.setAdapter(adapter);

        com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel viewModel =
                new androidx.lifecycle.ViewModelProvider(this).get(com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel.class);
        viewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            if (propiedades != null) {
                adapter.setPropiedades(propiedades);
            }
        });
    }


}
