package com.example.inmobiliariaventa.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Disposicion;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FiltrosFragment extends Fragment {

    private View filtrosSearchContainer;
    private ChipGroup chipGroupUbicaciones;
    private Set<String> selectedUbicaciones = new LinkedHashSet<>();

    private MaterialButtonToggleGroup tipoPropiedadGroup;
    private LinearLayout caracteristicasCasaContainer;
    private LinearLayout caracteristicasDeptoContainer;

    private TextInputEditText inputPrecioDesde;
    private TextInputEditText inputPrecioHasta;
    private MaterialButtonToggleGroup ambientesGroup;
    private MaterialButtonToggleGroup banosGroup;

    private MaterialSwitch switchCochera;
    private MaterialSwitch switchPatio;
    private MaterialSwitch switchBalcon;
    private MaterialSwitch switchAscensor;
    private AutoCompleteTextView dropdownDisposicion;

    private MaterialButton btnAplicarFiltros;
    private PropiedadViewModel propiedadViewModel;

    private List<Propiedad> cachedProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filtros, container, false);

        // Header controls
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        TextView clearFiltersButton = view.findViewById(R.id.clearFiltersButton);

        // Ubicación
        filtrosSearchContainer = view.findViewById(R.id.filtrosSearchContainer);
        chipGroupUbicaciones = view.findViewById(R.id.chipGroupUbicaciones);

        // Tipo de propiedad
        tipoPropiedadGroup = view.findViewById(R.id.tipoPropiedadGroup);
        caracteristicasCasaContainer = view.findViewById(R.id.caracteristicasCasaContainer);
        caracteristicasDeptoContainer = view.findViewById(R.id.caracteristicasDeptoContainer);

        // Precios
        inputPrecioDesde = view.findViewById(R.id.inputPrecioDesde);
        inputPrecioHasta = view.findViewById(R.id.inputPrecioHasta);

        // Ambientes y Baños
        ambientesGroup = view.findViewById(R.id.ambientesGroup);
        banosGroup = view.findViewById(R.id.banosGroup);

        // Switches características
        switchCochera = view.findViewById(R.id.switchCochera);
        switchPatio = view.findViewById(R.id.switchPatio);
        switchBalcon = view.findViewById(R.id.switchBalcon);
        switchAscensor = view.findViewById(R.id.switchAscensor);

        // Dropdown Disposición
        dropdownDisposicion = view.findViewById(R.id.dropdownDisposicion);

        // Botón Aplicar
        btnAplicarFiltros = view.findViewById(R.id.btnAplicarFiltros);

        // Navegación atrás
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Setup Disposición dropdown
        String[] disposiciones = {"Cualquiera", Disposicion.FRENTE.name(), Disposicion.CONTRAFRENTE.name(), Disposicion.LATERAL.name(), Disposicion.INTERNO.name()};
        ArrayAdapter<String> dispAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, disposiciones);
        dropdownDisposicion.setAdapter(dispAdapter);
        dropdownDisposicion.setText("Cualquiera", false);

        // Abrir SearchLocationFragment como selector de ubicación para Filtros
        filtrosSearchContainer.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("from_filtros", true);
            bundle.putStringArrayList("selected_locations", new ArrayList<>(selectedUbicaciones));
            Navigation.findNavController(v).navigate(R.id.nav_search_location, bundle);
        });

        // Toggle tipo de propiedad (opcional: ninguno, Casa o Departamento)
        tipoPropiedadGroup.setSelectionRequired(false);
        tipoPropiedadGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            int selectedId = group.getCheckedButtonId();
            if (selectedId == R.id.btnCasa) {
                caracteristicasCasaContainer.setVisibility(View.VISIBLE);
                caracteristicasDeptoContainer.setVisibility(View.GONE);
            } else if (selectedId == R.id.btnDepartamento) {
                caracteristicasCasaContainer.setVisibility(View.GONE);
                caracteristicasDeptoContainer.setVisibility(View.VISIBLE);
            } else {
                // Ninguno seleccionado: ambas características ocultas
                caracteristicasCasaContainer.setVisibility(View.GONE);
                caracteristicasDeptoContainer.setVisibility(View.GONE);
            }
            updateMatchingCountAndButton();
        });

        // Configuración inicial de Tipo de Propiedad: por defecto ninguno seleccionado
        String initialTipo = (getArguments() != null) ? getArguments().getString("tipo", "") : "";
        if ("Casa".equalsIgnoreCase(initialTipo)) {
            tipoPropiedadGroup.check(R.id.btnCasa);
            caracteristicasCasaContainer.setVisibility(View.VISIBLE);
            caracteristicasDeptoContainer.setVisibility(View.GONE);
        } else if ("Departamento".equalsIgnoreCase(initialTipo)) {
            tipoPropiedadGroup.check(R.id.btnDepartamento);
            caracteristicasCasaContainer.setVisibility(View.GONE);
            caracteristicasDeptoContainer.setVisibility(View.VISIBLE);
        } else {
            tipoPropiedadGroup.clearChecked();
            caracteristicasCasaContainer.setVisibility(View.GONE);
            caracteristicasDeptoContainer.setVisibility(View.GONE);
        }

        // Listeners para precio
        TextWatcher priceWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMatchingCountAndButton();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        if (inputPrecioDesde != null) inputPrecioDesde.addTextChangedListener(priceWatcher);
        if (inputPrecioHasta != null) inputPrecioHasta.addTextChangedListener(priceWatcher);

        // Listeners para ambientes y baños
        if (ambientesGroup != null) {
            ambientesGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> updateMatchingCountAndButton());
        }
        if (banosGroup != null) {
            banosGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> updateMatchingCountAndButton());
        }

        // Listeners para switches
        if (switchCochera != null) switchCochera.setOnCheckedChangeListener((btn, isChecked) -> updateMatchingCountAndButton());
        if (switchPatio != null) switchPatio.setOnCheckedChangeListener((btn, isChecked) -> updateMatchingCountAndButton());
        if (switchBalcon != null) switchBalcon.setOnCheckedChangeListener((btn, isChecked) -> updateMatchingCountAndButton());
        if (switchAscensor != null) switchAscensor.setOnCheckedChangeListener((btn, isChecked) -> updateMatchingCountAndButton());

        // Listener para disposición
        if (dropdownDisposicion != null) {
            dropdownDisposicion.setOnItemClickListener((parent, v, position, id) -> updateMatchingCountAndButton());
        }

        // Cargar propiedades de la base de datos para conteo dinámico
        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);
        propiedadViewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            cachedProperties.clear();
            if (propiedades != null) {
                for (Propiedad p : propiedades) {
                    if (p != null && p.isVisibleParaCliente()) {
                        cachedProperties.add(p);
                    }
                }
                updateMatchingCountAndButton();
            }
        });

        // Limpiar filtros
        clearFiltersButton.setOnClickListener(v -> {
            selectedUbicaciones.clear();
            if (chipGroupUbicaciones != null) {
                chipGroupUbicaciones.removeAllViews();
                chipGroupUbicaciones.setVisibility(View.GONE);
            }

            if (tipoPropiedadGroup != null) {
                tipoPropiedadGroup.clearChecked();
                caracteristicasCasaContainer.setVisibility(View.GONE);
                caracteristicasDeptoContainer.setVisibility(View.GONE);
            }
            if (ambientesGroup != null) ambientesGroup.clearChecked();
            if (banosGroup != null) banosGroup.clearChecked();

            if (inputPrecioDesde != null) inputPrecioDesde.setText("");
            if (inputPrecioHasta != null) inputPrecioHasta.setText("");

            if (switchCochera != null) switchCochera.setChecked(false);
            if (switchPatio != null) switchPatio.setChecked(false);
            if (switchBalcon != null) switchBalcon.setChecked(false);
            if (switchAscensor != null) switchAscensor.setChecked(false);

            if (dropdownDisposicion != null) dropdownDisposicion.setText("Cualquiera", false);

            updateMatchingCountAndButton();
        });

        // Botón Aplicar Filtros: empaqueta todos los filtros y navega a resultados
        btnAplicarFiltros.setOnClickListener(v -> {
            Bundle bundle = createFiltersBundle();
            Navigation.findNavController(v).navigate(R.id.action_nav_buscar_to_nav_resultados_busqueda, bundle);
        });

        // Leer ubicaciones y filtros recibidos por argumentos (ej. desde SearchLocationFragment o ResultadosBusquedaFragment)
        if (getArguments() != null) {
            ArrayList<String> initialLocs = getArguments().getStringArrayList("selected_locations");
            if (initialLocs == null || initialLocs.isEmpty()) {
                initialLocs = getArguments().getStringArrayList("filtro_ubicaciones");
            }
            if (initialLocs != null && !initialLocs.isEmpty()) {
                for (String l : initialLocs) {
                    if (l != null && !l.trim().isEmpty()) {
                        selectedUbicaciones.add(l.trim());
                    }
                }
            } else {
                String singleLoc = getArguments().getString("filtro_ubicacion", "");
                if (singleLoc == null || singleLoc.trim().isEmpty()) {
                    singleLoc = getArguments().getString("query", "");
                }
                if (singleLoc != null && !singleLoc.trim().isEmpty()) {
                    selectedUbicaciones.add(singleLoc.trim());
                }
            }



            double precioDesdeArg = getArguments().getDouble("precio_desde", -1);
            if (precioDesdeArg > 0 && inputPrecioDesde != null) {
                inputPrecioDesde.setText(String.valueOf((int) precioDesdeArg));
            }
            double precioHastaArg = getArguments().getDouble("precio_hasta", -1);
            if (precioHastaArg > 0 && inputPrecioHasta != null) {
                inputPrecioHasta.setText(String.valueOf((int) precioHastaArg));
            }

            int ambArg = getArguments().getInt("ambientes", -1);
            if (ambArg > 0 && ambientesGroup != null) {
                if (ambArg == 1) ambientesGroup.check(R.id.btnAmb1);
                else if (ambArg == 2) ambientesGroup.check(R.id.btnAmb2);
                else if (ambArg == 3) ambientesGroup.check(R.id.btnAmb3);
                else if (ambArg == 4) ambientesGroup.check(R.id.btnAmb4);
                else if (ambArg >= 5) ambientesGroup.check(R.id.btnAmb5);
            }

            int banosArg = getArguments().getInt("banos", -1);
            if (banosArg > 0 && banosGroup != null) {
                if (banosArg == 1) banosGroup.check(R.id.btnBan1);
                else if (banosArg == 2) banosGroup.check(R.id.btnBan2);
                else if (banosArg == 3) banosGroup.check(R.id.btnBan3);
                else if (banosArg >= 4) banosGroup.check(R.id.btnBan4);
            }

            if (switchCochera != null && getArguments().containsKey("tiene_cochera")) {
                switchCochera.setChecked(getArguments().getBoolean("tiene_cochera", false));
            }
            if (switchPatio != null && getArguments().containsKey("tiene_patio")) {
                switchPatio.setChecked(getArguments().getBoolean("tiene_patio", false));
            }
            if (switchBalcon != null && getArguments().containsKey("tiene_balcon")) {
                switchBalcon.setChecked(getArguments().getBoolean("tiene_balcon", false));
            }
            if (switchAscensor != null && getArguments().containsKey("tiene_ascensor")) {
                switchAscensor.setChecked(getArguments().getBoolean("tiene_ascensor", false));
            }

            String dispArg = getArguments().getString("disposicion", "Cualquiera");
            if (dropdownDisposicion != null && dispArg != null) {
                dropdownDisposicion.setText(dispArg, false);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Renderizar chips iniciales si vinieron por argumentos
        renderUbicacionesChips();
        updateMatchingCountAndButton();

        // Recibir la lista de ubicaciones seleccionadas desde SearchLocationFragment vía SavedStateHandle
        try {
            NavController navController = Navigation.findNavController(view);
            if (navController.getCurrentBackStackEntry() != null) {
                navController.getCurrentBackStackEntry().getSavedStateHandle()
                        .<ArrayList<String>>getLiveData("selected_locations")
                        .observe(getViewLifecycleOwner(), locations -> {
                            if (locations != null) {
                                selectedUbicaciones.clear();
                                for (String loc : locations) {
                                    if (loc != null && !loc.trim().isEmpty()) {
                                        selectedUbicaciones.add(loc.trim());
                                    }
                                }
                                renderUbicacionesChips();
                                navController.getCurrentBackStackEntry().getSavedStateHandle().set("selected_locations", null);
                                updateMatchingCountAndButton();
                            }
                        });
            }
        } catch (Exception ignored) {}
    }

    private void renderUbicacionesChips() {
        if (chipGroupUbicaciones == null) return;
        chipGroupUbicaciones.removeAllViews();

        if (selectedUbicaciones.isEmpty()) {
            chipGroupUbicaciones.setVisibility(View.GONE);
            return;
        }

        chipGroupUbicaciones.setVisibility(View.VISIBLE);
        for (String loc : selectedUbicaciones) {
            Chip chip = new Chip(requireContext());
            chip.setText(loc);
            chip.setCloseIconVisible(true);
            chip.setClickable(false);
            chip.setOnCloseIconClickListener(v -> {
                selectedUbicaciones.remove(loc);
                chipGroupUbicaciones.removeView(chip);
                if (selectedUbicaciones.isEmpty()) {
                    chipGroupUbicaciones.setVisibility(View.GONE);
                }
                updateMatchingCountAndButton();
            });
            chipGroupUbicaciones.addView(chip);
        }
    }

    private int getSelectedAmbientes() {
        if (ambientesGroup == null) return -1;
        int id = ambientesGroup.getCheckedButtonId();
        if (id == R.id.btnAmb1) return 1;
        if (id == R.id.btnAmb2) return 2;
        if (id == R.id.btnAmb3) return 3;
        if (id == R.id.btnAmb4) return 4;
        if (id == R.id.btnAmb5) return 5; // 5 o más
        return -1;
    }

    private int getSelectedBanos() {
        if (banosGroup == null) return -1;
        int id = banosGroup.getCheckedButtonId();
        if (id == R.id.btnBan1) return 1;
        if (id == R.id.btnBan2) return 2;
        if (id == R.id.btnBan3) return 3;
        if (id == R.id.btnBan4) return 4; // 4 o más
        return -1;
    }

    private List<Propiedad> filterProperties() {
        List<Propiedad> result = new ArrayList<>();
        if (cachedProperties == null) return result;

        int checkedTipoId = tipoPropiedadGroup != null ? tipoPropiedadGroup.getCheckedButtonId() : View.NO_ID;
        String tipo = "";
        if (checkedTipoId == R.id.btnCasa) {
            tipo = "Casa";
        } else if (checkedTipoId == R.id.btnDepartamento) {
            tipo = "Departamento";
        }

        double precioMin = -1;
        if (inputPrecioDesde != null && inputPrecioDesde.getText() != null) {
            String str = inputPrecioDesde.getText().toString().trim();
            if (!str.isEmpty()) {
                try { precioMin = Double.parseDouble(str); } catch (Exception ignored) {}
            }
        }

        double precioMax = -1;
        if (inputPrecioHasta != null && inputPrecioHasta.getText() != null) {
            String str = inputPrecioHasta.getText().toString().trim();
            if (!str.isEmpty()) {
                try { precioMax = Double.parseDouble(str); } catch (Exception ignored) {}
            }
        }

        int ambientes = getSelectedAmbientes();
        int banos = getSelectedBanos();

        boolean cochera = switchCochera != null && switchCochera.isChecked();
        boolean patio = switchPatio != null && switchPatio.isChecked();
        boolean balcon = switchBalcon != null && switchBalcon.isChecked();
        boolean ascensor = switchAscensor != null && switchAscensor.isChecked();

        String disposicion = dropdownDisposicion != null ? dropdownDisposicion.getText().toString().trim() : "Cualquiera";

        for (Propiedad p : cachedProperties) {
            // Filtro por tipo
            if (!tipo.isEmpty()) {
                if (p.tipoPropiedad == null) continue;
                if ("Casa".equalsIgnoreCase(tipo) && p.tipoPropiedad != TipoPropiedad.CASA) continue;
                if ("Departamento".equalsIgnoreCase(tipo) && p.tipoPropiedad != TipoPropiedad.DEPARTAMENTO) continue;
            }

            // Filtro por ubicación seleccionada (ciudades o barrios)
            if (!selectedUbicaciones.isEmpty()) {
                boolean matchesAnyLocation = false;
                for (String loc : selectedUbicaciones) {
                    String u = loc.toLowerCase(Locale.ROOT).trim();
                    String ciudad = p.ciudad != null ? p.ciudad.toLowerCase(Locale.ROOT) : "";
                    String prov = p.estadoProvincia != null ? p.estadoProvincia.toLowerCase(Locale.ROOT) : "";
                    String dom = p.domicilio != null ? p.domicilio.toLowerCase(Locale.ROOT) : "";
                    if (ciudad.contains(u) || prov.contains(u) || dom.contains(u)) {
                        matchesAnyLocation = true;
                        break;
                    }
                }
                if (!matchesAnyLocation) {
                    continue;
                }
            }

            // Filtro por precio
            if (precioMin > 0 && p.precio < precioMin) continue;
            if (precioMax > 0 && p.precio > precioMax) continue;

            // Filtro por ambientes
            if (ambientes > 0) {
                if (ambientes == 5) {
                    if (p.ambientes < 5) continue;
                } else {
                    if (p.ambientes != ambientes) continue;
                }
            }

            // Filtro por baños
            if (banos > 0) {
                if (banos == 4) {
                    if (p.banos < 4) continue;
                } else {
                    if (p.banos != banos) continue;
                }
            }

            // Características de Casa
            if ("Casa".equalsIgnoreCase(tipo) || tipo.isEmpty()) {
                if (cochera && !p.tieneCochera) continue;
                if (patio && !p.tienePatio) continue;
            }

            // Características de Departamento
            if ("Departamento".equalsIgnoreCase(tipo) || tipo.isEmpty()) {
                if (balcon && !p.tieneBalcon) continue;
                if (ascensor && !p.tieneAscensor) continue;
                if (!"Cualquiera".equalsIgnoreCase(disposicion) && p.disposicion != null) {
                    if (!p.disposicion.name().equalsIgnoreCase(disposicion)) continue;
                }
            }

            result.add(p);
        }

        return result;
    }

    private void updateMatchingCountAndButton() {
        int count = filterProperties().size();
        if (btnAplicarFiltros != null) {
            if (count == 0) {
                btnAplicarFiltros.setText("No se encontraron propiedades");
            } else if (count == 1) {
                btnAplicarFiltros.setText("Ver 1 propiedad");
            } else {
                btnAplicarFiltros.setText("Ver " + count + " propiedades");
            }
        }
    }

    private Bundle createFiltersBundle() {
        Bundle bundle = new Bundle();

        bundle.putStringArrayList("filtro_ubicaciones", new ArrayList<>(selectedUbicaciones));
        if (!selectedUbicaciones.isEmpty()) {
            bundle.putString("filtro_ubicacion", android.text.TextUtils.join(", ", selectedUbicaciones));
        } else {
            bundle.putString("filtro_ubicacion", "");
        }

        int checkedTipoId = tipoPropiedadGroup != null ? tipoPropiedadGroup.getCheckedButtonId() : View.NO_ID;
        String tipo = "Todas";
        if (checkedTipoId == R.id.btnCasa) {
            tipo = "Casa";
        } else if (checkedTipoId == R.id.btnDepartamento) {
            tipo = "Departamento";
        }
        bundle.putString("tipo", tipo);

        double precioMin = -1;
        if (inputPrecioDesde != null && inputPrecioDesde.getText() != null) {
            String str = inputPrecioDesde.getText().toString().trim();
            if (!str.isEmpty()) {
                try { precioMin = Double.parseDouble(str); } catch (Exception ignored) {}
            }
        }
        bundle.putDouble("precio_desde", precioMin);

        double precioMax = -1;
        if (inputPrecioHasta != null && inputPrecioHasta.getText() != null) {
            String str = inputPrecioHasta.getText().toString().trim();
            if (!str.isEmpty()) {
                try { precioMax = Double.parseDouble(str); } catch (Exception ignored) {}
            }
        }
        bundle.putDouble("precio_hasta", precioMax);

        bundle.putInt("ambientes", getSelectedAmbientes());
        bundle.putInt("banos", getSelectedBanos());

        bundle.putBoolean("tiene_cochera", "Casa".equals(tipo) && switchCochera != null && switchCochera.isChecked());
        bundle.putBoolean("tiene_patio", "Casa".equals(tipo) && switchPatio != null && switchPatio.isChecked());
        bundle.putBoolean("tiene_balcon", "Departamento".equals(tipo) && switchBalcon != null && switchBalcon.isChecked());
        bundle.putBoolean("tiene_ascensor", "Departamento".equals(tipo) && switchAscensor != null && switchAscensor.isChecked());

        String disp = ("Departamento".equals(tipo) && dropdownDisposicion != null) ? dropdownDisposicion.getText().toString().trim() : "Cualquiera";
        bundle.putString("disposicion", disp);

        return bundle;
    }
}
