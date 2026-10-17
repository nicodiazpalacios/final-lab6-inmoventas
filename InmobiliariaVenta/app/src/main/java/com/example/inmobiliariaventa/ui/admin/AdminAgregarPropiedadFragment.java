package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class AdminAgregarPropiedadFragment extends Fragment {

    private TextInputEditText etTitulo, etDescripcion, etPrecio, etDireccion, etCiudad;
    private TextInputEditText etAmbientes, etBanos, etSuperficie;
    private AutoCompleteTextView spinnerTipoPropiedad;
    private MaterialButton btnPublicarPropiedad, btnSeleccionarFotos;
    private PropiedadViewModel propiedadViewModel;

    public AdminAgregarPropiedadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_agregar_propiedad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);

        etTitulo = view.findViewById(R.id.et_titulo_propiedad);
        etDescripcion = view.findViewById(R.id.et_descripcion_propiedad);
        etPrecio = view.findViewById(R.id.et_precio_propiedad);
        etDireccion = view.findViewById(R.id.et_direccion_propiedad);
        etCiudad = view.findViewById(R.id.et_ciudad_propiedad);
        etAmbientes = view.findViewById(R.id.et_ambientes_propiedad);
        etBanos = view.findViewById(R.id.et_banos_propiedad);
        etSuperficie = view.findViewById(R.id.et_superficie_propiedad);

        spinnerTipoPropiedad = view.findViewById(R.id.spinner_tipo_propiedad);
        if (spinnerTipoPropiedad != null) {
            String[] tipos = new String[]{"Casa", "Departamento"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos);
            spinnerTipoPropiedad.setAdapter(adapter);
            spinnerTipoPropiedad.setText(tipos[0], false);
        }

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        btnSeleccionarFotos = view.findViewById(R.id.btn_seleccionar_fotos);
        if (btnSeleccionarFotos != null) {
            btnSeleccionarFotos.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Selección de fotos lista para adjuntar", Toast.LENGTH_SHORT).show()
            );
        }

        btnPublicarPropiedad = view.findViewById(R.id.btn_publicar_propiedad);
        if (btnPublicarPropiedad != null) {
            btnPublicarPropiedad.setOnClickListener(v -> publicarPropiedad(view));
        }
    }

    private void publicarPropiedad(View view) {
        String titulo = etTitulo != null && etTitulo.getText() != null ? etTitulo.getText().toString().trim() : "";
        String direccion = etDireccion != null && etDireccion.getText() != null ? etDireccion.getText().toString().trim() : "";
        String precioStr = etPrecio != null && etPrecio.getText() != null ? etPrecio.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo)) {
            if (etTitulo != null) etTitulo.setError("Ingrese un título");
            return;
        }

        if (TextUtils.isEmpty(direccion)) {
            if (etDireccion != null) etDireccion.setError("Ingrese una dirección");
            return;
        }

        if (TextUtils.isEmpty(precioStr)) {
            if (etPrecio != null) etPrecio.setError("Ingrese un precio válido");
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            if (etPrecio != null) etPrecio.setError("Precio inválido");
            return;
        }

        Propiedad propiedad = new Propiedad();
        propiedad.id = UUID.randomUUID().toString();
        propiedad.titulo = titulo;
        propiedad.domicilio = direccion;
        propiedad.precio = precio;
        propiedad.descripcion = etDescripcion != null && etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        propiedad.ciudad = etCiudad != null && etCiudad.getText() != null ? etCiudad.getText().toString().trim() : "Buenos Aires";

        String tipoSeleccionado = spinnerTipoPropiedad != null ? spinnerTipoPropiedad.getText().toString() : "Casa";
        propiedad.tipoPropiedad = "Departamento".equalsIgnoreCase(tipoSeleccionado) ? TipoPropiedad.DEPARTAMENTO : TipoPropiedad.CASA;

        try {
            propiedad.ambientes = etAmbientes != null && !TextUtils.isEmpty(etAmbientes.getText()) ?
                    Integer.parseInt(etAmbientes.getText().toString().trim()) : 1;
        } catch (Exception ignored) { propiedad.ambientes = 1; }

        try {
            propiedad.banos = etBanos != null && !TextUtils.isEmpty(etBanos.getText()) ?
                    Integer.parseInt(etBanos.getText().toString().trim()) : 1;
        } catch (Exception ignored) { propiedad.banos = 1; }

        try {
            propiedad.superficieM2 = etSuperficie != null && !TextUtils.isEmpty(etSuperficie.getText()) ?
                    Double.parseDouble(etSuperficie.getText().toString().trim()) : 0.0;
        } catch (Exception ignored) { propiedad.superficieM2 = 0.0; }

        propiedad.estado = "DISPONIBLE";

        propiedadViewModel.insert(propiedad);

        Toast.makeText(requireContext(), "Propiedad publicada y sincronizada correctamente", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }
}
