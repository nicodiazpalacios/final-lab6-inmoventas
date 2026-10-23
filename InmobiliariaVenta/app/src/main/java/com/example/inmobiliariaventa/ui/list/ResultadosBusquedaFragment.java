package com.example.inmobiliariaventa.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    private List<Propiedad> listaFiltradaResultados = new ArrayList<>();
    private int limiteActualResultados = 20;
    private static final int BATCH_SIZE_SEARCH = 20;
    private boolean isCargandoMas = false;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private String selectedTipo = "Todas";
    private String selectedSort = "Recomendados";
    private String currentSearchQuery = "";
    private boolean hasSeededDefaults = false;

    // Filtros avanzados recibidos desde FiltrosFragment
    private String filterUbicacion = "";
    private List<String> filterUbicacionesList = new ArrayList<>();
    private double filterPrecioDesde = -1;
    private double filterPrecioHasta = -1;
    private int filterAmbientes = -1;
    private int filterBanos = -1;
    private boolean filterTieneCochera = false;
    private boolean filterTienePatio = false;
    private boolean filterTieneBalcon = false;
    private boolean filterTieneAscensor = false;
    private String filterDisposicion = "Cualquiera";

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

        // RecyclerView Setup con reciclaje de vistas fluido a 60/120 FPS
        adapter = new PropiedadDestacadaAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        searchResultsRecyclerView.setLayoutManager(layoutManager);
        searchResultsRecyclerView.setHasFixedSize(true);
        searchResultsRecyclerView.setItemViewCacheSize(6);
        searchResultsRecyclerView.setAdapter(adapter);

        // Carga continua automática al hacer scroll con vista reciclada
        searchResultsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isCargandoMas && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        cargarMasResultadosBusqueda();
                    }
                }
            }
        });

        // Volver atrás
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Ir a Filtros (recordando la ubicación y los filtros actuales)
        if (btnFiltrosTune != null) {
            btnFiltrosTune.setOnClickListener(v -> {
                Bundle bundle = new Bundle();

                ArrayList<String> locsToPass = new ArrayList<>();
                if (filterUbicacionesList != null && !filterUbicacionesList.isEmpty()) {
                    locsToPass.addAll(filterUbicacionesList);
                } else if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
                    locsToPass.add(currentSearchQuery.trim());
                } else if (filterUbicacion != null && !filterUbicacion.trim().isEmpty()) {
                    locsToPass.add(filterUbicacion.trim());
                }

                bundle.putStringArrayList("selected_locations", locsToPass);
                bundle.putStringArrayList("filtro_ubicaciones", locsToPass);
                if (!locsToPass.isEmpty()) {
                    bundle.putString("filtro_ubicacion", android.text.TextUtils.join(", ", locsToPass));
                }

                bundle.putString("tipo", selectedTipo);
                bundle.putDouble("precio_desde", filterPrecioDesde);
                bundle.putDouble("precio_hasta", filterPrecioHasta);
                bundle.putInt("ambientes", filterAmbientes);
                bundle.putInt("banos", filterBanos);
                bundle.putBoolean("tiene_cochera", filterTieneCochera);
                bundle.putBoolean("tiene_patio", filterTienePatio);
                bundle.putBoolean("tiene_balcon", filterTieneBalcon);
                bundle.putBoolean("tiene_ascensor", filterTieneAscensor);
                bundle.putString("disposicion", filterDisposicion);

                Navigation.findNavController(v).navigate(R.id.action_nav_resultados_busqueda_to_nav_buscar, bundle);
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

        // Botón limpiar texto del buscador
        ImageView btnClearSearch = view.findViewById(R.id.btnClearSearch);
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (inputBusquedaResultados != null) {
                    inputBusquedaResultados.setText("");
                    inputBusquedaResultados.requestFocus();
                }
            });
        }

        // Búsqueda en tiempo real por texto
        if (inputBusquedaResultados != null) {
            // Evitar saltos de línea (filtrar \n y \r)
            inputBusquedaResultados.setFilters(new InputFilter[] {
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        if (source.charAt(i) == '\n' || source.charAt(i) == '\r') {
                            return "";
                        }
                    }
                    return null;
                }
            });

            inputBusquedaResultados.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    hideKeyboard(v);
                    applyFiltersAndSort();
                    return true;
                }
                return false;
            });

            inputBusquedaResultados.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        hideKeyboard(v);
                        applyFiltersAndSort();
                    }
                    return true;
                }
                return false;
            });

            inputBusquedaResultados.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (filterUbicacionesList != null && !filterUbicacionesList.isEmpty()) {
                        String joined = android.text.TextUtils.join(", ", filterUbicacionesList);
                        if (!joined.equalsIgnoreCase(currentSearchQuery)) {
                            filterUbicacionesList.clear();
                        }
                    }
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> applyFiltersAndSort();
                    searchHandler.postDelayed(searchRunnable, 150);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Leer argumentos iniciales de búsqueda si existen
        if (getArguments() != null) {
            String initialQuery = getArguments().getString("query", "");
            String initialTipo = getArguments().getString("tipo", "");

            ArrayList<String> ubicacionesArg = getArguments().getStringArrayList("filtro_ubicaciones");
            if (ubicacionesArg == null || ubicacionesArg.isEmpty()) {
                ubicacionesArg = getArguments().getStringArrayList("selected_locations");
            }
            if (ubicacionesArg != null && !ubicacionesArg.isEmpty()) {
                filterUbicacionesList = new ArrayList<>(ubicacionesArg);
            }

            filterUbicacion = getArguments().getString("filtro_ubicacion", "");
            if (filterUbicacion == null) filterUbicacion = "";

            if (filterUbicacionesList.isEmpty() && !filterUbicacion.trim().isEmpty()) {
                filterUbicacionesList.add(filterUbicacion.trim());
            }

            filterPrecioDesde = getArguments().getDouble("precio_desde", -1);
            filterPrecioHasta = getArguments().getDouble("precio_hasta", -1);
            filterAmbientes = getArguments().getInt("ambientes", -1);
            filterBanos = getArguments().getInt("banos", -1);
            filterTieneCochera = getArguments().getBoolean("tiene_cochera", false);
            filterTienePatio = getArguments().getBoolean("tiene_patio", false);
            filterTieneBalcon = getArguments().getBoolean("tiene_balcon", false);
            filterTieneAscensor = getArguments().getBoolean("tiene_ascensor", false);
            filterDisposicion = getArguments().getString("disposicion", "Cualquiera");

            if (!filterUbicacionesList.isEmpty()) {
                currentSearchQuery = android.text.TextUtils.join(", ", filterUbicacionesList);
                if (inputBusquedaResultados != null) {
                    inputBusquedaResultados.setText(currentSearchQuery);
                    inputBusquedaResultados.setSelection(currentSearchQuery.length());
                }
            } else if (initialQuery != null && !initialQuery.isEmpty()) {
                currentSearchQuery = initialQuery;
                filterUbicacionesList.add(initialQuery.trim());
                if (inputBusquedaResultados != null) {
                    inputBusquedaResultados.setText(initialQuery);
                    inputBusquedaResultados.setSelection(initialQuery.length());
                }
            } else if (!filterUbicacion.isEmpty()) {
                currentSearchQuery = filterUbicacion;
                if (inputBusquedaResultados != null) {
                    inputBusquedaResultados.setText(filterUbicacion);
                    inputBusquedaResultados.setSelection(filterUbicacion.length());
                }
            }

            if (initialTipo != null && !initialTipo.isEmpty() && !"Todas".equalsIgnoreCase(initialTipo)) {
                selectedTipo = initialTipo;
                if (txtTipoPropiedad != null) {
                    txtTipoPropiedad.setText(initialTipo);
                }
            } else {
                selectedTipo = "Todas";
                if (txtTipoPropiedad != null) {
                    txtTipoPropiedad.setText("Todas las propiedades");
                }
            }
        }

        // ViewModel y carga de datos reales desde Room / Supabase
        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);
        cardModoOfflineResultados = view.findViewById(R.id.cardModoOfflineResultados);

        configurarObservadorRed();

        return view;
    }

    private com.google.android.material.card.MaterialCardView cardModoOfflineResultados;
    private androidx.lifecycle.LiveData<List<Propiedad>> liveDataActualResultados;
    private androidx.lifecycle.Observer<List<Propiedad>> observerActualResultados;
    private Boolean ultimoEstadoConexion = null;

    private void configurarObservadorRed() {
        if (getContext() == null) return;
        com.example.inmobiliariaventa.utils.NetworkUtils.getNetworkLiveData(requireContext())
                .observe(getViewLifecycleOwner(), isOnline -> {
                    boolean conexionActiva = Boolean.TRUE.equals(isOnline);
                    actualizarModoConexion(conexionActiva);
                });
    }

    private void actualizarModoConexion(boolean isOnline) {
        if (getContext() == null) return;

        if (liveDataActualResultados != null && observerActualResultados != null) {
            liveDataActualResultados.removeObserver(observerActualResultados);
        }

        ultimoEstadoConexion = isOnline;

        if (isOnline) {
            if (cardModoOfflineResultados != null) cardModoOfflineResultados.setVisibility(View.GONE);

            observerActualResultados = propiedades -> {
                if (Boolean.TRUE.equals(ultimoEstadoConexion)) {
                    if (propiedades == null || propiedades.isEmpty()) {
                        if (!hasSeededDefaults) {
                            hasSeededDefaults = true;
                            propiedadViewModel.sincronizarConNube(null);
                        }
                    } else {
                        masterList = new ArrayList<>(propiedades);
                        applyFiltersAndSort();
                    }
                }
            };

            liveDataActualResultados = propiedadViewModel.getAllPropiedades();
            liveDataActualResultados.observe(getViewLifecycleOwner(), observerActualResultados);
        } else {
            if (cardModoOfflineResultados != null) cardModoOfflineResultados.setVisibility(View.VISIBLE);

            observerActualResultados = propiedades -> {
                if (Boolean.FALSE.equals(ultimoEstadoConexion)) {
                    masterList = propiedades != null ? new ArrayList<>(propiedades) : new ArrayList<>();
                    applyFiltersAndSort();
                }
            };

            liveDataActualResultados = propiedadViewModel.getAllPropiedades();
            liveDataActualResultados.observe(getViewLifecycleOwner(), observerActualResultados);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && getContext() != null) {
            adapter.recargarFavoritos(getContext());
        }
        if (getContext() != null) {
            boolean isOnline = com.example.inmobiliariaventa.utils.NetworkUtils.isNetworkAvailable(requireContext());
            if (ultimoEstadoConexion == null || !ultimoEstadoConexion.equals(isOnline)) {
                actualizarModoConexion(isOnline);
            }
        }
    }

    private void applyFiltersAndSort() {
        if (masterList == null) return;

        List<Propiedad> filtered = new ArrayList<>();

        for (Propiedad prop : masterList) {
            // No mostrar al cliente propiedades pausadas por el administrador
            if (prop == null || !prop.isVisibleParaCliente()) {
                continue;
            }

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

            // Filtro por precio
            if (filterPrecioDesde > 0 && prop.precio < filterPrecioDesde) continue;
            if (filterPrecioHasta > 0 && prop.precio > filterPrecioHasta) continue;

            // Filtro por ambientes
            if (filterAmbientes > 0) {
                if (filterAmbientes == 5) {
                    if (prop.ambientes < 5) continue;
                } else {
                    if (prop.ambientes != filterAmbientes) continue;
                }
            }

            // Filtro por baños
            if (filterBanos > 0) {
                if (filterBanos == 4) {
                    if (prop.banos < 4) continue;
                } else {
                    if (prop.banos != filterBanos) continue;
                }
            }

            // Características de Casa
            if ("Casa".equalsIgnoreCase(selectedTipo) || "Todas".equalsIgnoreCase(selectedTipo)) {
                if (filterTieneCochera && !prop.tieneCochera) continue;
                if (filterTienePatio && !prop.tienePatio) continue;
            }

            // Características de Depto
            if ("Departamento".equalsIgnoreCase(selectedTipo) || "Todas".equalsIgnoreCase(selectedTipo)) {
                if (filterTieneBalcon && !prop.tieneBalcon) continue;
                if (filterTieneAscensor && !prop.tieneAscensor) continue;
                if (filterDisposicion != null && !"Cualquiera".equalsIgnoreCase(filterDisposicion) && prop.disposicion != null) {
                    if (!prop.disposicion.name().equalsIgnoreCase(filterDisposicion)) continue;
                }
            }

            // Filtro por múltiples ubicaciones seleccionadas
            if (filterUbicacionesList != null && !filterUbicacionesList.isEmpty()) {
                boolean matchesAnyLocation = false;
                for (String loc : filterUbicacionesList) {
                    String u = loc.toLowerCase(Locale.ROOT).trim();
                    String ciudad = prop.ciudad != null ? prop.ciudad.toLowerCase(Locale.ROOT) : "";
                    String prov = prop.estadoProvincia != null ? prop.estadoProvincia.toLowerCase(Locale.ROOT) : "";
                    String dom = prop.domicilio != null ? prop.domicilio.toLowerCase(Locale.ROOT) : "";
                    if (ciudad.contains(u) || prov.contains(u) || dom.contains(u)) {
                        matchesAnyLocation = true;
                        break;
                    }
                }
                if (!matchesAnyLocation) {
                    continue;
                }
            } else if (!currentSearchQuery.isEmpty()) {
                // Filtro por texto de búsqueda si no vino lista de ubicaciones o se limpió
                String query = currentSearchQuery.toLowerCase(Locale.ROOT);
                boolean matchesDomicilio = prop.domicilio != null && prop.domicilio.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesDescripcion = prop.descripcion != null && prop.descripcion.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesTitulo = prop.titulo != null && prop.titulo.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesCiudad = prop.ciudad != null && prop.ciudad.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesProvincia = prop.estadoProvincia != null && prop.estadoProvincia.toLowerCase(Locale.ROOT).contains(query);
                boolean matchesTipo = prop.tipoPropiedad != null && prop.tipoPropiedad.name().toLowerCase(Locale.ROOT).contains(query);

                if (!matchesDomicilio && !matchesDescripcion && !matchesTitulo && !matchesCiudad && !matchesProvincia && !matchesTipo) {
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

        // Actualizar UI con paginación fluida por lotes
        listaFiltradaResultados = filtered;
        limiteActualResultados = Math.min(BATCH_SIZE_SEARCH, listaFiltradaResultados.size());
        adapter.setPropiedades(new ArrayList<>(listaFiltradaResultados.subList(0, limiteActualResultados)));

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

    private void hideKeyboard(View view) {
        if (view != null && isAdded() && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void cargarMasResultadosBusqueda() {
        if (listaFiltradaResultados == null || limiteActualResultados >= listaFiltradaResultados.size() || isCargandoMas) return;
        isCargandoMas = true;
        int nuevoLimite = Math.min(limiteActualResultados + BATCH_SIZE_SEARCH, listaFiltradaResultados.size());
        List<Propiedad> lote = new ArrayList<>(listaFiltradaResultados.subList(limiteActualResultados, nuevoLimite));
        limiteActualResultados = nuevoLimite;
        adapter.agregarPropiedades(lote);
        if (searchResultsRecyclerView != null) {
            searchResultsRecyclerView.post(() -> isCargandoMas = false);
        } else {
            isCargandoMas = false;
        }
    }
}
