package com.example.inmobiliariaventa.ui.list;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.ui.home.PropiedadDestacadaAdapter;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ResultadosBusquedaFragment extends Fragment {

    private RecyclerView searchResultsRecyclerView;
    private PropiedadDestacadaAdapter adapter;
    private PropiedadViewModel propiedadViewModel;

    private TextView txtResultadosTitle;
    private TextView txtResultadosCount;
    private TextView txtTipoPropiedad;
    private TextView txtSortOption;
    private EditText inputBusquedaResultados;
    private LinearLayout emptyStateContainer;

    private List<Propiedad> masterList = new ArrayList<>();
    private String selectedTipo = "Todas";
    private String selectedSort = "Recomendados";
    private String currentSearchQuery = "";
    private boolean hasSeededDefaults = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resultados_busqueda, container, false);

        // Bind Views
        ImageView btnBack = view.findViewById(R.id.btnBack);
        ImageView btnFiltrosTune = view.findViewById(R.id.btnFiltrosTune);
        inputBusquedaResultados = view.findViewById(R.id.inputBusquedaResultados);
        View chipTipoPropiedad = view.findViewById(R.id.chipTipoPropiedad);
        txtTipoPropiedad = view.findViewById(R.id.txtTipoPropiedad);
        View btnOrdenar = view.findViewById(R.id.btnOrdenar);
        txtSortOption = view.findViewById(R.id.txtSortOption);
        txtResultadosTitle = view.findViewById(R.id.txtResultadosTitle);
        txtResultadosCount = view.findViewById(R.id.txtResultadosCount);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        MaterialButton loadMoreButton = view.findViewById(R.id.loadMoreButton);

        // RecyclerView Setup
        adapter = new PropiedadDestacadaAdapter(new ArrayList<>());
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        searchResultsRecyclerView.setAdapter(adapter);

        // Volver atrás
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Ir a Filtros
        if (btnFiltrosTune != null) {
            btnFiltrosTune.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.nav_buscar);
            });
        }

        // Menú desplegable para Tipo de Propiedad
        if (chipTipoPropiedad != null) {
            chipTipoPropiedad.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(requireContext(), chipTipoPropiedad);
                popupMenu.getMenu().add("Todas las propiedades");
                popupMenu.getMenu().add("Casa");
                popupMenu.getMenu().add("Departamento");

                popupMenu.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    if ("Todas las propiedades".equals(title)) {
                        selectedTipo = "Todas";
                        txtTipoPropiedad.setText("Tipo de propiedad");
                    } else {
                        selectedTipo = title;
                        txtTipoPropiedad.setText(title);
                    }
                    applyFiltersAndSort();
                    return true;
                });
                popupMenu.show();
            });
        }

        // Menú desplegable para Ordenar
        if (btnOrdenar != null) {
            btnOrdenar.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(requireContext(), btnOrdenar);
                popupMenu.getMenu().add("Recomendados");
                popupMenu.getMenu().add("Menor Precio");
                popupMenu.getMenu().add("Mayor Precio");

                popupMenu.setOnMenuItemClickListener(item -> {
                    selectedSort = item.getTitle().toString();
                    txtSortOption.setText(selectedSort);
                    applyFiltersAndSort();
                    return true;
                });
                popupMenu.show();
            });
        }

        // Búsqueda en tiempo real por texto
        if (inputBusquedaResultados != null) {
            inputBusquedaResultados.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim() : "";
                    applyFiltersAndSort();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Botón Cargar Más
        if (loadMoreButton != null) {
            loadMoreButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Todas las propiedades ya están cargadas", Toast.LENGTH_SHORT).show();
            });
        }

        // ViewModel y carga de datos
        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);
        propiedadViewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            if (propiedades == null || propiedades.isEmpty()) {
                if (!hasSeededDefaults) {
                    hasSeededDefaults = true;
                    seedDefaultProperties();
                }
            } else {
                masterList = new ArrayList<>(propiedades);
                applyFiltersAndSort();
            }
        });

        return view;
    }

    private void seedDefaultProperties() {
        List<Propiedad> defaults = createSampleProperties();
        masterList = new ArrayList<>(defaults);
        applyFiltersAndSort();

        // Guardar en la base de datos Room para persistencia
        for (Propiedad p : defaults) {
            propiedadViewModel.insert(p);
        }
    }

    private List<Propiedad> createSampleProperties() {
        List<Propiedad> lista = new ArrayList<>();

        Propiedad p1 = new Propiedad();
        p1.id = UUID.randomUUID().toString();
        p1.precio = 4250000.0;
        p1.domicilio = "1042 Oceanview Promenade";
        p1.descripcion = "Santa Monica, CA 90403";
        p1.ambientes = 5;
        p1.banos = 6;
        p1.tipoPropiedad = TipoPropiedad.CASA;
        p1.tieneCochera = true;
        p1.tienePatio = true;
        lista.add(p1);

        Propiedad p2 = new Propiedad();
        p2.id = UUID.randomUUID().toString();
        p2.precio = 2895000.0;
        p2.domicilio = "770 Riverwalk Drive";
        p2.descripcion = "Austin, TX 78704";
        p2.ambientes = 4;
        p2.banos = 3;
        p2.tipoPropiedad = TipoPropiedad.CASA;
        p2.tieneCochera = true;
        lista.add(p2);

        Propiedad p3 = new Propiedad();
        p3.id = UUID.randomUUID().toString();
        p3.precio = 1150000.0;
        p3.domicilio = "Av. del Libertador 4800";
        p3.descripcion = "Palermo, Buenos Aires";
        p3.ambientes = 3;
        p3.banos = 2;
        p3.tipoPropiedad = TipoPropiedad.DEPARTAMENTO;
        p3.tieneBalcon = true;
        p3.tieneCochera = true;
        lista.add(p3);

        Propiedad p4 = new Propiedad();
        p4.id = UUID.randomUUID().toString();
        p4.precio = 3400000.0;
        p4.domicilio = "Juana Manso 1250 - Penthouse";
        p4.descripcion = "Puerto Madero, Buenos Aires";
        p4.ambientes = 4;
        p4.banos = 4;
        p4.tipoPropiedad = TipoPropiedad.DEPARTAMENTO;
        p4.tieneBalcon = true;
        p4.tieneAscensor = true;
        p4.tieneCochera = true;
        lista.add(p4);

        Propiedad p5 = new Propiedad();
        p5.id = UUID.randomUUID().toString();
        p5.precio = 5600000.0;
        p5.domicilio = "21400 Pacific Coast Hwy";
        p5.descripcion = "Malibu, CA 90265";
        p5.ambientes = 6;
        p5.banos = 5;
        p5.tipoPropiedad = TipoPropiedad.CASA;
        p5.tienePatio = true;
        p5.tieneCochera = true;
        lista.add(p5);

        Propiedad p6 = new Propiedad();
        p6.id = UUID.randomUUID().toString();
        p6.precio = 780000.0;
        p6.domicilio = "Callao y Quintana";
        p6.descripcion = "Recoleta, Buenos Aires";
        p6.ambientes = 2;
        p6.banos = 1;
        p6.tipoPropiedad = TipoPropiedad.DEPARTAMENTO;
        p6.tieneAscensor = true;
        lista.add(p6);

        return lista;
    }

    private void applyFiltersAndSort() {
        if (masterList == null) return;

        List<Propiedad> filtered = new ArrayList<>();

        for (Propiedad prop : masterList) {
            // Filtro por tipo
            if (!"Todas".equalsIgnoreCase(selectedTipo)) {
                if (prop.tipoPropiedad == null) continue;
                if ("Casa".equalsIgnoreCase(selectedTipo) && prop.tipoPropiedad != TipoPropiedad.CASA) {
                    continue;
                }
                if ("Departamento".equalsIgnoreCase(selectedTipo) && prop.tipoPropiedad != TipoPropiedad.DEPARTAMENTO) {
                    continue;
                }
            }

            // Filtro por texto de búsqueda
            if (!currentSearchQuery.isEmpty()) {
                String query = currentSearchQuery.toLowerCase(Locale.ROOT);
                boolean matchesDomicilio = prop.domicilio != null && prop.domicilio.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesDescripcion = prop.descripcion != null && prop.descripcion.toLowerCase(Locale.ROOT).contains(query);
                if (!matchesDomicilio && !matchesDescripcion) {
                    continue;
                }
            }

            filtered.add(prop);
        }

        // Ordenamiento
        if ("Menor Precio".equals(selectedSort)) {
            Collections.sort(filtered, (a, b) -> Double.compare(a.precio, b.precio));
        } else if ("Mayor Precio".equals(selectedSort)) {
            Collections.sort(filtered, (a, b) -> Double.compare(b.precio, a.precio));
        }

        // Actualizar UI
        adapter.setPropiedades(filtered);

        int count = filtered.size();
        if (txtResultadosCount != null) {
            String countText = count + (count == 1 ? " propiedad encontrada" : " propiedades encontradas");
            txtResultadosCount.setText(countText);
        }

        if (emptyStateContainer != null && searchResultsRecyclerView != null) {
            if (count == 0) {
                emptyStateContainer.setVisibility(View.VISIBLE);
                searchResultsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateContainer.setVisibility(View.GONE);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}
