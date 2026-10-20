package com.example.inmobiliariaventa.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.ui.home.LocationSuggestionAdapter.SuggestionItem;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchLocationFragment extends Fragment {

    private static final String TAG = "SearchLocation";

    private EditText searchEditText;
    private ImageView btnClearSearch;
    private RecyclerView suggestionsRecyclerView;
    private LocationSuggestionAdapter adapter;
    private PropiedadViewModel propiedadViewModel;

    private HorizontalScrollView scrollSelectedChips;
    private ChipGroup selectedChipsGroup;
    private MaterialButton btnConfirmarUbicaciones;
    private Set<String> selectedLocations = new LinkedHashSet<>();

    private List<Propiedad> cachedProperties = new ArrayList<>();
    private String detectedUserCity = null;
    private boolean isFromFiltros = false;

    // Lanzador para solicitar permisos de ubicación al entrar
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            isFromFiltros = getArguments().getBoolean("from_filtros", false);
            ArrayList<String> initialLocs = getArguments().getStringArrayList("selected_locations");
            if (initialLocs != null) {
                selectedLocations.addAll(initialLocs);
            }
        }

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if ((fine != null && fine) || (coarse != null && coarse)) {
                        detectUserLocation();
                    } else {
                        renderPublishedLocations();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_location, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (isFromFiltros) {
                    returnSelectedLocationsAndFinish();
                } else {
                    hideKeyboard(v);
                    Navigation.findNavController(v).navigateUp();
                }
            });
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFromFiltros) {
                    returnSelectedLocationsAndFinish();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        searchEditText = view.findViewById(R.id.searchEditText);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        ImageView btnFiltrosTune = view.findViewById(R.id.btnFiltrosTune);
        suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        scrollSelectedChips = view.findViewById(R.id.scrollSelectedChips);
        selectedChipsGroup = view.findViewById(R.id.selectedChipsGroup);
        btnConfirmarUbicaciones = view.findViewById(R.id.btnConfirmarUbicaciones);

        // Botón Filtros: pasa todo lo seleccionado/buscado a FiltrosFragment
        if (btnFiltrosTune != null) {
            btnFiltrosTune.setOnClickListener(v -> {
                hideKeyboard(v);

                ArrayList<String> locsToPass = new ArrayList<>(selectedLocations);
                String currentSearchText = searchEditText != null && searchEditText.getText() != null
                        ? searchEditText.getText().toString().trim() : "";
                if (!currentSearchText.isEmpty() && !locsToPass.contains(currentSearchText)) {
                    locsToPass.add(currentSearchText);
                }

                if (isFromFiltros) {
                    selectedLocations.clear();
                    selectedLocations.addAll(locsToPass);
                    returnSelectedLocationsAndFinish();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("selected_locations", locsToPass);
                    if (!locsToPass.isEmpty()) {
                        bundle.putString("filtro_ubicacion", TextUtils.join(", ", locsToPass));
                    }
                    Navigation.findNavController(v).navigate(
                            R.id.action_nav_search_location_to_nav_buscar,
                            bundle
                    );
                }
            });
        }

        // Setup RecyclerView Adapter
        adapter = new LocationSuggestionAdapter(new ArrayList<>(), this::onLocationSelected);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionsRecyclerView.setAdapter(adapter);

        adapter.setSelectedLocations(selectedLocations);
        renderSelectedChips();
        updateConfirmButton();

        if (btnConfirmarUbicaciones != null) {
            btnConfirmarUbicaciones.setOnClickListener(v -> {
                hideKeyboard(v);
                if (isFromFiltros) {
                    returnSelectedLocationsAndFinish();
                } else {
                    Bundle bundle = new Bundle();
                    ArrayList<String> locsList = new ArrayList<>(selectedLocations);
                    String currentSearchText = searchEditText != null && searchEditText.getText() != null
                            ? searchEditText.getText().toString().trim() : "";
                    if (!currentSearchText.isEmpty() && !locsList.contains(currentSearchText)) {
                        locsList.add(currentSearchText);
                    }
                    bundle.putStringArrayList("filtro_ubicaciones", locsList);
                    if (!locsList.isEmpty()) {
                        bundle.putString("query", TextUtils.join(", ", locsList));
                        bundle.putString("filtro_ubicacion", TextUtils.join(", ", locsList));
                    }
                    Navigation.findNavController(v).navigate(
                            R.id.action_nav_search_location_to_nav_resultados_busqueda,
                            bundle
                    );
                }
            });
        }

        // Botón para vaciar búsqueda
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (searchEditText != null) {
                    searchEditText.setText("");
                    searchEditText.requestFocus();
                }
            });
        }

        // Búsqueda en caliente en tiempo real de ciudades y barrios
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.trim().isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (query.trim().isEmpty()) {
                        renderPublishedLocations();
                    } else {
                        performLiveLocationSearch(query.trim());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Evitar saltos de línea (filtrar \n y \r)
            searchEditText.setFilters(new InputFilter[] {
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        if (source.charAt(i) == '\n' || source.charAt(i) == '\r') {
                            return "";
                        }
                    }
                    return null;
                }
            });

            // Tecla Enter/Buscar en el teclado
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String q = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
                    if (!q.isEmpty()) {
                        onLocationSelected(SuggestionItem.item("📍 " + q, q));
                    }
                    hideKeyboard(v);
                    return true;
                }
                return false;
            });

            searchEditText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        String q = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
                        if (!q.isEmpty()) {
                            onLocationSelected(SuggestionItem.item("📍 " + q, q));
                        }
                        hideKeyboard(v);
                    }
                    return true;
                }
                return false;
            });
        }

        // Cargar catálogo de propiedades para extraer ciudades y barrios reales de la base local Room
        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);
        propiedadViewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            if (propiedades != null) {
                cachedProperties = new ArrayList<>(propiedades);
                String currentText = searchEditText != null ? searchEditText.getText().toString().trim() : "";
                if (currentText.isEmpty()) {
                    renderPublishedLocations();
                } else {
                    performLiveLocationSearch(currentText);
                }
            }
        });

        if (com.example.inmobiliariaventa.utils.NetworkUtils.isNetworkAvailable(getContext())) {
            propiedadViewModel.sincronizarConNube(null);
        }

        // Solicitar o detectar ubicación
        if (hasLocationPermission()) {
            detectUserLocation();
        } else {
            renderPublishedLocations();
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        return view;
    }

    private boolean hasLocationPermission() {
        Context ctx = getContext();
        if (ctx == null) return false;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void detectUserLocation() {
        if (!isAdded() || getContext() == null) return;

        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;

        boolean networkEnabled = false;
        boolean gpsEnabled = false;
        try { networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
        try { gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}

        Location bestLocation = null;
        try {
            if (networkEnabled) bestLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gpsEnabled) {
                Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (bestLocation == null || (gpsLoc != null && gpsLoc.getTime() > bestLocation.getTime())) {
                    bestLocation = gpsLoc;
                }
            }
        } catch (SecurityException ignored) {}

        if (bestLocation != null) {
            resolveCoordinates(bestLocation);
        } else {
            try {
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        try { lm.removeUpdates(this); } catch (SecurityException ignored) {}
                        resolveCoordinates(location);
                    }
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                };
                if (networkEnabled) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                } else if (gpsEnabled) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                }
            } catch (SecurityException ignored) {}
        }
    }

    private void resolveCoordinates(Location loc) {
        if (loc == null || !isAdded() || getContext() == null) return;
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();

        // 1. Detección rápida por coordenadas
        if (lat >= -29.5 && lat <= -25.5 && lon >= -66.0 && lon <= -62.0) {
            detectedUserCity = "Santiago del Estero";
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::renderPublishedLocations);
            }
            return;
        } else if (lat >= -35.5 && lat <= -34.0 && lon >= -59.5 && lon <= -57.5) {
            detectedUserCity = "Buenos Aires";
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::renderPublishedLocations);
            }
            return;
        } else if (lat >= -33.5 && lat <= -30.0 && lon >= -65.5 && lon <= -63.0) {
            detectedUserCity = "Córdoba";
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::renderPublishedLocations);
            }
            return;
        }

        // 2. Geocoder
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(appContext, new Locale("es", "AR"));
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    detectedUserCity = addr.getLocality() != null ? addr.getLocality() : addr.getAdminArea();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) renderPublishedLocations();
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Muestra exclusivamente Ciudades y Barrios (sin tarjetas completas de propiedades)
     */
    private void renderPublishedLocations() {
        List<SuggestionItem> list = new ArrayList<>();

        // 1. Ubicación detectada del usuario
        if (detectedUserCity != null && !detectedUserCity.isEmpty()) {
            list.add(SuggestionItem.header("TU UBICACIÓN ACTUAL DETECTADA"));
            list.add(SuggestionItem.item("📍 <b>" + detectedUserCity + "</b><br/><small><font color='#1E40AF'>Tu ubicación actual</font></small>", detectedUserCity));
        }

        // 2. Ciudades con propiedades publicadas
        Map<String, Integer> cityCountMap = new LinkedHashMap<>();
        Set<String> publishedNeighborhoods = new LinkedHashSet<>();

        if (cachedProperties != null) {
            for (Propiedad p : cachedProperties) {
                if (p.ciudad != null && !p.ciudad.trim().isEmpty()) {
                    String c = p.ciudad.trim();
                    cityCountMap.put(c, cityCountMap.getOrDefault(c, 0) + 1);
                }

                // Extraer barrios reales de las propiedades publicadas
                if (p.domicilio != null && !p.domicilio.trim().isEmpty()) {
                    String dom = p.domicilio.trim();
                    String c = p.ciudad != null ? p.ciudad.trim() : "";
                    if (dom.toLowerCase(Locale.ROOT).contains("belgrano")) {
                        publishedNeighborhoods.add("Barrio Belgrano" + (!c.isEmpty() ? ", " + c : ""));
                    } else if (dom.toLowerCase(Locale.ROOT).contains("aguirre") || dom.toLowerCase(Locale.ROOT).contains("vargas")) {
                        publishedNeighborhoods.add("Parque Aguirre" + (!c.isEmpty() ? ", " + c : ""));
                    } else if (dom.toLowerCase(Locale.ROOT).contains("libertad") || dom.toLowerCase(Locale.ROOT).contains("independencia")) {
                        publishedNeighborhoods.add("Centro" + (!c.isEmpty() ? ", " + c : ""));
                    } else if (c.toLowerCase(Locale.ROOT).contains("palermo")) {
                        publishedNeighborhoods.add("Palermo, Buenos Aires");
                    } else if (c.toLowerCase(Locale.ROOT).contains("puerto madero") || dom.toLowerCase(Locale.ROOT).contains("manso")) {
                        publishedNeighborhoods.add("Puerto Madero, Buenos Aires");
                    }
                }
            }
        }

        if (!cityCountMap.isEmpty()) {
            list.add(SuggestionItem.header("CIUDADES CON PROPIEDADES DISPONIBLES"));
            for (Map.Entry<String, Integer> entry : cityCountMap.entrySet()) {
                String countText = entry.getValue() == 1 ? "1 propiedad disponible" : entry.getValue() + " propiedades disponibles";
                list.add(SuggestionItem.item("📍 <b>" + entry.getKey() + "</b><br/><small><font color='#6B7280'>" + countText + "</font></small>", entry.getKey()));
            }
        }

        if (!publishedNeighborhoods.isEmpty()) {
            list.add(SuggestionItem.header("BARRIOS Y ZONAS DISPONIBLES"));
            for (String neighborhood : publishedNeighborhoods) {
                String cleanQuery = neighborhood.split(",")[0].trim();
                list.add(SuggestionItem.item("📍 <b>" + neighborhood + "</b>", cleanQuery));
            }
        }

        adapter.updateData(list);
    }

    /**
     * Búsqueda en caliente: filtra exclusivamente por nombres de ciudades y barrios
     */
    private void performLiveLocationSearch(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<SuggestionItem> results = new ArrayList<>();

        // Opción 1: Usar exactamente lo que el usuario escribió
        results.add(SuggestionItem.item("🔍 Usar ubicación: \"<b><font color='#1E40AF'>" + query + "</font></b>\"", query));

        // Filtrar ciudades del catálogo
        Map<String, Integer> matchedCities = new LinkedHashMap<>();
        Set<String> matchedNeighborhoods = new LinkedHashSet<>();

        if (cachedProperties != null) {
            for (Propiedad p : cachedProperties) {
                String c = p.ciudad != null ? p.ciudad.trim() : "";
                if (!c.isEmpty() && c.toLowerCase(Locale.ROOT).contains(q)) {
                    matchedCities.put(c, matchedCities.getOrDefault(c, 0) + 1);
                }

                String dom = p.domicilio != null ? p.domicilio.trim() : "";
                if (dom.toLowerCase(Locale.ROOT).contains("belgrano") && ("barrio belgrano".contains(q) || "belgrano".contains(q))) {
                    matchedNeighborhoods.add("Barrio Belgrano" + (!c.isEmpty() ? ", " + c : ""));
                } else if ((dom.toLowerCase(Locale.ROOT).contains("aguirre") || dom.toLowerCase(Locale.ROOT).contains("vargas")) && "parque aguirre".contains(q)) {
                    matchedNeighborhoods.add("Parque Aguirre" + (!c.isEmpty() ? ", " + c : ""));
                } else if ((dom.toLowerCase(Locale.ROOT).contains("libertad") || dom.toLowerCase(Locale.ROOT).contains("independencia")) && "centro".contains(q)) {
                    matchedNeighborhoods.add("Centro" + (!c.isEmpty() ? ", " + c : ""));
                } else if (c.toLowerCase(Locale.ROOT).contains("palermo") && "palermo".contains(q)) {
                    matchedNeighborhoods.add("Palermo, Buenos Aires");
                } else if ((c.toLowerCase(Locale.ROOT).contains("puerto madero") || dom.toLowerCase(Locale.ROOT).contains("manso")) && "puerto madero".contains(q)) {
                    matchedNeighborhoods.add("Puerto Madero, Buenos Aires");
                }
            }
        }

        if (!matchedCities.isEmpty()) {
            results.add(SuggestionItem.header("CIUDADES COINCIDENTES"));
            for (Map.Entry<String, Integer> entry : matchedCities.entrySet()) {
                String countText = entry.getValue() == 1 ? "1 propiedad disponible" : entry.getValue() + " propiedades disponibles";
                results.add(SuggestionItem.item("📍 " + highlightMatch(entry.getKey(), query) + "<br/><small><font color='#6B7280'>" + countText + "</font></small>", entry.getKey()));
            }
        }

        if (!matchedNeighborhoods.isEmpty()) {
            results.add(SuggestionItem.header("BARRIOS COINCIDENTES"));
            for (String n : matchedNeighborhoods) {
                String cleanQuery = n.split(",")[0].trim();
                results.add(SuggestionItem.item("📍 " + highlightMatch(n, query), cleanQuery));
            }
        }

        adapter.updateData(results);
    }

    private String highlightMatch(String fullText, String query) {
        if (fullText == null) return "";
        if (query == null || query.trim().isEmpty()) return fullText;

        String trimmed = query.trim();
        int idx = fullText.toLowerCase(Locale.ROOT).indexOf(trimmed.toLowerCase(Locale.ROOT));
        if (idx >= 0) {
            String before = fullText.substring(0, idx);
            String match = fullText.substring(idx, idx + trimmed.length());
            String after = fullText.substring(idx + trimmed.length());
            return before + "<b><font color='#1E40AF'>" + match + "</font></b>" + after;
        }
        return fullText;
    }

    private void onLocationSelected(SuggestionItem item) {
        if (item == null || item.isHeader) return;

        String location = item.searchQuery != null ? item.searchQuery.trim() : "";
        if (location.isEmpty()) return;

        if (isFromFiltros) {
            // Alternar selección (agregar o quitar)
            boolean removed = false;
            for (String s : new ArrayList<>(selectedLocations)) {
                if (s.equalsIgnoreCase(location)) {
                    selectedLocations.remove(s);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                selectedLocations.add(location);
            }

            if (searchEditText != null && !searchEditText.getText().toString().isEmpty()) {
                searchEditText.setText("");
            }

            renderSelectedChips();
            adapter.setSelectedLocations(selectedLocations);
            updateConfirmButton();
        } else {
            // Si vino desde el Home: navegar a los resultados de esa ubicación
            hideKeyboard(getView());
            Bundle bundle = new Bundle();
            bundle.putString("query", location);
            bundle.putString("filtro_ubicacion", location);
            ArrayList<String> locList = new ArrayList<>();
            locList.add(location);
            bundle.putStringArrayList("filtro_ubicaciones", locList);
            bundle.putStringArrayList("selected_locations", locList);
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(
                        R.id.action_nav_search_location_to_nav_resultados_busqueda,
                        bundle
                );
            }
        }
    }

    private void renderSelectedChips() {
        if (selectedChipsGroup == null || scrollSelectedChips == null) return;
        selectedChipsGroup.removeAllViews();

        if (selectedLocations.isEmpty()) {
            scrollSelectedChips.setVisibility(View.GONE);
            return;
        }

        scrollSelectedChips.setVisibility(View.VISIBLE);
        for (String loc : selectedLocations) {
            Chip chip = new Chip(requireContext());
            chip.setText(loc);
            chip.setCloseIconVisible(true);
            chip.setClickable(false);
            chip.setOnCloseIconClickListener(v -> {
                selectedLocations.remove(loc);
                renderSelectedChips();
                adapter.setSelectedLocations(selectedLocations);
                updateConfirmButton();
            });
            selectedChipsGroup.addView(chip);
        }
    }

    private void updateConfirmButton() {
        if (btnConfirmarUbicaciones == null) return;

        if (isFromFiltros) {
            btnConfirmarUbicaciones.setVisibility(View.VISIBLE);
            if (selectedLocations.isEmpty()) {
                btnConfirmarUbicaciones.setText("Listo");
            } else {
                btnConfirmarUbicaciones.setText("Listo (" + selectedLocations.size() + ")");
            }
        } else {
            if (selectedLocations.isEmpty()) {
                btnConfirmarUbicaciones.setVisibility(View.GONE);
            } else if (selectedLocations.size() == 1) {
                btnConfirmarUbicaciones.setVisibility(View.VISIBLE);
                btnConfirmarUbicaciones.setText("Ver propiedades en " + selectedLocations.iterator().next());
            } else {
                btnConfirmarUbicaciones.setVisibility(View.VISIBLE);
                btnConfirmarUbicaciones.setText("Ver propiedades (" + selectedLocations.size() + " zonas)");
            }
        }
    }

    private void returnSelectedLocationsAndFinish() {
        hideKeyboard(getView());
        String currentSearchText = searchEditText != null && searchEditText.getText() != null
                ? searchEditText.getText().toString().trim() : "";
        if (!currentSearchText.isEmpty() && !selectedLocations.contains(currentSearchText)) {
            selectedLocations.add(currentSearchText);
        }

        try {
            NavController navController = Navigation.findNavController(requireView());
            if (navController.getPreviousBackStackEntry() != null) {
                navController.getPreviousBackStackEntry().getSavedStateHandle()
                        .set("selected_locations", new ArrayList<>(selectedLocations));
            }
            navController.navigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Error returning to FiltrosFragment", e);
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
}
