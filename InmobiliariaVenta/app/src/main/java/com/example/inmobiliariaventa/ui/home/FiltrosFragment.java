package com.example.inmobiliariaventa.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Disposicion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.materialswitch.MaterialSwitch;

public class FiltrosFragment extends Fragment {

    private MaterialButtonToggleGroup tipoPropiedadGroup;
    private LinearLayout caracteristicasCasaContainer;
    private LinearLayout caracteristicasDeptoContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filtros, container, false);

        // Header controls
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        TextView clearFiltersButton = view.findViewById(R.id.clearFiltersButton);

        // Property type toggles
        tipoPropiedadGroup = view.findViewById(R.id.tipoPropiedadGroup);
        caracteristicasCasaContainer = view.findViewById(R.id.caracteristicasCasaContainer);
        caracteristicasDeptoContainer = view.findViewById(R.id.caracteristicasDeptoContainer);

        // Dropdown
        AutoCompleteTextView dropdownDisposicion = view.findViewById(R.id.dropdownDisposicion);

        // Buttons
        MaterialButton btnAplicarFiltros = view.findViewById(R.id.btnAplicarFiltros);
        
        // Navigation
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Setup Disposicion dropdown
        String[] disposiciones = {"Cualquiera", Disposicion.FRENTE.name(), Disposicion.CONTRAFRENTE.name(), Disposicion.LATERAL.name(), Disposicion.INTERNO.name()};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, disposiciones);
        dropdownDisposicion.setAdapter(adapter);
        dropdownDisposicion.setText("Cualquiera", false);

        // Setup dynamic visibility based on property type
        tipoPropiedadGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnCasa) {
                    caracteristicasCasaContainer.setVisibility(View.VISIBLE);
                    caracteristicasDeptoContainer.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnDepartamento) {
                    caracteristicasCasaContainer.setVisibility(View.GONE);
                    caracteristicasDeptoContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        // Initialize state
        caracteristicasCasaContainer.setVisibility(View.VISIBLE);
        caracteristicasDeptoContainer.setVisibility(View.GONE);

        clearFiltersButton.setOnClickListener(v -> {
            // Reset logic here (e.g., untoggle groups, clear text inputs)
            tipoPropiedadGroup.check(R.id.btnCasa);
            
            MaterialButtonToggleGroup ambientesGroup = view.findViewById(R.id.ambientesGroup);
            ambientesGroup.clearChecked();
            
            MaterialButtonToggleGroup banosGroup = view.findViewById(R.id.banosGroup);
            banosGroup.clearChecked();

            ((TextInputEditText) view.findViewById(R.id.inputPrecioDesde)).setText("");
            ((TextInputEditText) view.findViewById(R.id.inputPrecioHasta)).setText("");
            
            ((MaterialSwitch) view.findViewById(R.id.switchCochera)).setChecked(false);
            ((MaterialSwitch) view.findViewById(R.id.switchPatio)).setChecked(false);
            ((MaterialSwitch) view.findViewById(R.id.switchBalcon)).setChecked(false);
            ((MaterialSwitch) view.findViewById(R.id.switchAscensor)).setChecked(false);
            
            dropdownDisposicion.setText("Cualquiera", false);
        });

        btnAplicarFiltros.setOnClickListener(v -> {
            // Here you would gather filter data and pass it back or navigate
            Navigation.findNavController(v).navigateUp();
        });

        return view;
    }
}
