package com.example.inmobiliariaventa.ui.admin;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.ActividadRepository;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AdminAgregarPropiedadFragment extends Fragment {

    private TextInputEditText etTitulo, etDescripcion, etPrecio, etDireccion, etCiudad, etProvincia;
    private TextInputEditText etAmbientes, etBanos, etSuperficie;
    private AutoCompleteTextView spinnerTipoPropiedad;
    private MaterialButton btnPublicarPropiedad, btnSeleccionarFotos;
    private TextView tvExplorarArchivos;
    private TextView tvTituloPantalla, tvSubtituloPantalla;
    private View layoutFotosSubidasContainer;
    private TextView tvContadorFotos;
    private RecyclerView rvFotosPropiedad;
    private AdminFotosAdapter fotosAdapter;
    private final List<String> listaFotos = new ArrayList<>();

    private PropiedadViewModel propiedadViewModel;
    private String propiedadIdAEditar = null;
    private Propiedad propiedadExistente = null;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty() && getContext() != null) {
                    int agregadas = 0;
                    for (Uri uri : uris) {
                        if (uri != null) {
                            String uriStr = uri.toString();
                            if (!listaFotos.contains(uriStr)) {
                                listaFotos.add(uriStr);
                                agregadas++;
                            }
                        }
                    }
                    actualizarListaFotos();
                    Toast.makeText(requireContext(),
                            agregadas == 1 ? "Foto agregada" : agregadas + " fotos agregadas",
                            Toast.LENGTH_SHORT).show();
                }
            });

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

        tvTituloPantalla = view.findViewById(R.id.tv_titulo_pantalla_agregar);
        tvSubtituloPantalla = view.findViewById(R.id.tv_subtitulo_pantalla_agregar);
        etTitulo = view.findViewById(R.id.et_titulo_propiedad);
        etDescripcion = view.findViewById(R.id.et_descripcion_propiedad);
        etPrecio = view.findViewById(R.id.et_precio_propiedad);
        etDireccion = view.findViewById(R.id.et_direccion_propiedad);
        etCiudad = view.findViewById(R.id.et_ciudad_propiedad);
        etProvincia = view.findViewById(R.id.et_provincia_propiedad);
        etAmbientes = view.findViewById(R.id.et_ambientes_propiedad);
        etBanos = view.findViewById(R.id.et_banos_propiedad);
        etSuperficie = view.findViewById(R.id.et_superficie_propiedad);
        tvExplorarArchivos = view.findViewById(R.id.tv_explorar_archivos);

        layoutFotosSubidasContainer = view.findViewById(R.id.layout_fotos_subidas_container);
        tvContadorFotos = view.findViewById(R.id.tv_contador_fotos);
        rvFotosPropiedad = view.findViewById(R.id.rv_fotos_propiedad);

        if (rvFotosPropiedad != null) {
            rvFotosPropiedad.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            fotosAdapter = new AdminFotosAdapter(position -> {
                if (position >= 0 && position < listaFotos.size()) {
                    listaFotos.remove(position);
                    actualizarListaFotos();
                    Toast.makeText(requireContext(), "Foto eliminada", Toast.LENGTH_SHORT).show();
                }
            });
            rvFotosPropiedad.setAdapter(fotosAdapter);
        }

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
            btnSeleccionarFotos.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        btnPublicarPropiedad = view.findViewById(R.id.btn_publicar_propiedad);
        if (btnPublicarPropiedad != null) {
            btnPublicarPropiedad.setOnClickListener(v -> guardarOActualizarPropiedad(view));
        }

        actualizarListaFotos();

        // Verificar si venimos a modificar una propiedad existente
        if (getArguments() != null && getArguments().containsKey("propiedad_id")) {
            propiedadIdAEditar = getArguments().getString("propiedad_id");
            if (propiedadIdAEditar != null && !propiedadIdAEditar.isEmpty()) {
                configurarModoEdicion();
            }
        }
    }

    private void configurarModoEdicion() {
        if (tvTituloPantalla != null) {
            tvTituloPantalla.setText("Modificar Propiedad");
        }
        if (tvSubtituloPantalla != null) {
            tvSubtituloPantalla.setText("Actualiza los detalles, fotos y datos de la publicación.");
        }
        if (btnPublicarPropiedad != null) {
            btnPublicarPropiedad.setText("Guardar Cambios");
        }

        propiedadViewModel.getPropiedadPorId(propiedadIdAEditar).observe(getViewLifecycleOwner(), propiedad -> {
            if (propiedad != null && propiedadExistente == null) {
                propiedadExistente = propiedad;
                cargarDatosEnFormulario(propiedad);
            }
        });
    }

    private void cargarDatosEnFormulario(Propiedad p) {
        if (etTitulo != null && p.titulo != null) {
            etTitulo.setText(p.titulo);
        }
        if (etDescripcion != null && p.descripcion != null) {
            etDescripcion.setText(p.descripcion);
        }
        if (etPrecio != null) {
            long precioLong = (long) p.precio;
            if (p.precio == precioLong) {
                etPrecio.setText(String.valueOf(precioLong));
            } else {
                etPrecio.setText(String.valueOf(p.precio));
            }
        }
        if (etDireccion != null && p.domicilio != null) {
            etDireccion.setText(p.domicilio);
        }
        if (etCiudad != null && p.ciudad != null) {
            etCiudad.setText(p.ciudad);
        }
        if (etProvincia != null && p.estadoProvincia != null) {
            etProvincia.setText(p.estadoProvincia);
        }
        if (etAmbientes != null) {
            etAmbientes.setText(String.valueOf(p.ambientes));
        }
        if (etBanos != null) {
            etBanos.setText(String.valueOf(p.banos));
        }
        if (etSuperficie != null && p.superficieM2 != null && p.superficieM2 > 0) {
            etSuperficie.setText(String.valueOf(p.superficieM2.intValue()));
        }
        if (spinnerTipoPropiedad != null) {
            String tipoStr = (p.tipoPropiedad == TipoPropiedad.DEPARTAMENTO) ? "Departamento" : "Casa";
            spinnerTipoPropiedad.setText(tipoStr, false);
        }

        // Cargar fotos existentes en la lista
        listaFotos.clear();
        if (p.imagenes != null && !p.imagenes.isEmpty()) {
            for (String img : p.imagenes) {
                if (img != null && !img.trim().isEmpty()) {
                    listaFotos.add(img.trim());
                }
            }
        }
        actualizarListaFotos();
    }

    private void actualizarListaFotos() {
        if (fotosAdapter != null) {
            fotosAdapter.setFotos(listaFotos);
        }
        if (layoutFotosSubidasContainer != null) {
            layoutFotosSubidasContainer.setVisibility(listaFotos.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (tvContadorFotos != null) {
            tvContadorFotos.setText("Fotos cargadas (" + listaFotos.size() + ")");
        }
        if (tvExplorarArchivos != null) {
            if (listaFotos.isEmpty()) {
                tvExplorarArchivos.setText("Haga clic para agregar fotos a la propiedad");
            } else {
                tvExplorarArchivos.setText(listaFotos.size() + " foto(s) cargada(s). Clic para agregar más");
            }
        }
    }

    private void guardarOActualizarPropiedad(View view) {
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

        String descripcion = etDescripcion != null && etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String ciudad = etCiudad != null && etCiudad.getText() != null ? etCiudad.getText().toString().trim() : "Buenos Aires";
        String provincia = etProvincia != null && etProvincia.getText() != null ? etProvincia.getText().toString().trim() : "";
        String tipoSeleccionado = spinnerTipoPropiedad != null ? spinnerTipoPropiedad.getText().toString() : "Casa";
        TipoPropiedad tipoPropiedad = "Departamento".equalsIgnoreCase(tipoSeleccionado) ? TipoPropiedad.DEPARTAMENTO : TipoPropiedad.CASA;

        int ambientes = 1;
        try {
            if (etAmbientes != null && !TextUtils.isEmpty(etAmbientes.getText())) {
                ambientes = Integer.parseInt(etAmbientes.getText().toString().trim());
            }
        } catch (Exception ignored) {}

        int banos = 1;
        try {
            if (etBanos != null && !TextUtils.isEmpty(etBanos.getText())) {
                banos = Integer.parseInt(etBanos.getText().toString().trim());
            }
        } catch (Exception ignored) {}

        double superficie = 0.0;
        try {
            if (etSuperficie != null && !TextUtils.isEmpty(etSuperficie.getText())) {
                superficie = Double.parseDouble(etSuperficie.getText().toString().trim());
            }
        } catch (Exception ignored) {}

        if (propiedadIdAEditar != null && !propiedadIdAEditar.isEmpty()) {
            // Actualizar propiedad existente
            Propiedad p = propiedadExistente != null ? propiedadExistente : new Propiedad();
            double precioAnterior = p.precio;

            p.id = propiedadIdAEditar;
            p.titulo = titulo;
            p.domicilio = direccion;
            p.precio = precio;
            p.descripcion = descripcion;
            p.ciudad = ciudad;
            p.estadoProvincia = provincia;
            p.tipoPropiedad = tipoPropiedad;
            p.ambientes = ambientes;
            p.banos = banos;
            p.superficieM2 = superficie;

            p.imagenes = new ArrayList<>(listaFotos);

            if (p.estado == null || p.estado.isEmpty()) {
                p.estado = "DISPONIBLE";
            }

            propiedadViewModel.update(p);

            // Sincronizar el nuevo título en cualquier contrato histórico vinculado a esta propiedad
            if (getContext() != null) {
                android.content.Context ctx = requireContext().getApplicationContext();
                com.example.inmobiliariaventa.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                    com.example.inmobiliariaventa.data.local.AppDatabase db = com.example.inmobiliariaventa.data.local.AppDatabase.getInstance(ctx);
                    List<com.example.inmobiliariaventa.domain.entity.Contrato> contratos = db.contratoDao().obtenerTodosSync();
                    if (contratos != null) {
                        for (com.example.inmobiliariaventa.domain.entity.Contrato c : contratos) {
                            if (propiedadIdAEditar.equals(c.propiedadId) && !titulo.equals(c.propiedadTitulo)) {
                                c.propiedadTitulo = titulo;
                                db.contratoDao().actualizar(c);
                            }
                        }
                    }
                });
            }

            // Registrar en actividad reciente si cambió de precio
            if (precioAnterior > 0 && Math.abs(precioAnterior - precio) > 0.01 && getContext() != null) {
                ActividadRepository.getInstance(requireContext())
                        .registrarCambioPrecio(titulo, precioAnterior, precio);
            }

            Toast.makeText(requireContext(), "Propiedad actualizada correctamente", Toast.LENGTH_SHORT).show();
        } else {
            // Crear nueva propiedad
            Propiedad nueva = new Propiedad();
            nueva.id = UUID.randomUUID().toString();
            nueva.titulo = titulo;
            nueva.domicilio = direccion;
            nueva.precio = precio;
            nueva.descripcion = descripcion;
            nueva.ciudad = ciudad;
            nueva.estadoProvincia = provincia;
            nueva.tipoPropiedad = tipoPropiedad;
            nueva.ambientes = ambientes;
            nueva.banos = banos;
            nueva.superficieM2 = superficie;
            nueva.estado = "DISPONIBLE";
            nueva.createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());

            nueva.imagenes = new ArrayList<>(listaFotos);

            propiedadViewModel.insert(nueva);

            // Registrar en actividad reciente
            if (getContext() != null) {
                ActividadRepository.getInstance(requireContext())
                        .registrarNuevaPropiedad(titulo, precio);
            }

            Toast.makeText(requireContext(), "Propiedad publicada y sincronizada correctamente", Toast.LENGTH_SHORT).show();
        }

        Navigation.findNavController(view).popBackStack();
    }
}
