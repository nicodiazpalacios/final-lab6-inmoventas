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

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeUsuarioFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private RecyclerView featuredRecyclerView;
    private RecyclerView latestRecyclerView;
    private LinearLayout searchContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_usuario, container, false);

        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        featuredRecyclerView = view.findViewById(R.id.featuredRecyclerView);
        latestRecyclerView = view.findViewById(R.id.latestRecyclerView);
        searchContainer = view.findViewById(R.id.searchContainer);

        searchContainer.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_inicio_to_nav_search_location);
        });

        setupCategories();
        setupFeaturedProperties();
        setupLatestProperties();

        return view;
    }

    private void setupCategories() {
        List<CategoriaAdapter.Categoria> categorias = new ArrayList<>();
        categorias.add(new CategoriaAdapter.Categoria("Casa", R.drawable.ic_home));
        categorias.add(new CategoriaAdapter.Categoria("Depto", R.drawable.ic_propiedades));

        CategoriaAdapter adapter = new CategoriaAdapter(categorias);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        categoriesRecyclerView.setAdapter(adapter);
    }

    private void setupFeaturedProperties() {
        List<Propiedad> destacadas = new ArrayList<>();
        
        Propiedad p1 = new Propiedad();
        p1.id = UUID.randomUUID().toString();
        p1.precio = 4250000.0;
        p1.domicilio = "1042 Oceanview Promenade";
        p1.descripcion = "Santa Monica, CA 90403";
        p1.ambientes = 5;
        p1.banos = 6;
        
        Propiedad p2 = new Propiedad();
        p2.id = UUID.randomUUID().toString();
        p2.precio = 2895000.0;
        p2.domicilio = "770 Riverwalk Drive";
        p2.descripcion = "Austin, TX 78704";
        p2.ambientes = 4;
        p2.banos = 3;
        
        destacadas.add(p1);
        destacadas.add(p2);

        PropiedadDestacadaAdapter adapter = new PropiedadDestacadaAdapter(destacadas);
        // CHANGE TO VERTICAL for mobile design
        featuredRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        featuredRecyclerView.setAdapter(adapter);
    }

    private void setupLatestProperties() {
        List<Propiedad> ultimas = new ArrayList<>();
        
        Propiedad p1 = new Propiedad();
        p1.id = UUID.randomUUID().toString();
        p1.precio = 895000;
        p1.domicilio = "Skyline Penthouse Suite";
        p1.descripcion = "Downtown Seattle, WA 98101";
        p1.ambientes = 2;
        p1.banos = 2;
        p1.tipoPropiedad = TipoPropiedad.DEPARTAMENTO;
        
        Propiedad p2 = new Propiedad();
        p2.id = UUID.randomUUID().toString();
        p2.precio = 1150000;
        p2.domicilio = "Modern Suburban Retreat";
        p2.descripcion = "Cherry Creek, CO 80206";
        p2.ambientes = 3;
        p2.banos = 2;
        p2.tipoPropiedad = TipoPropiedad.CASA;
        
        ultimas.add(p1);
        ultimas.add(p2);

        PropiedadListaAdapter adapter = new PropiedadListaAdapter(ultimas);
        latestRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        latestRecyclerView.setAdapter(adapter);
    }
}
