package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.ui.propiedades.PropiedadViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminPropiedadesFragment extends Fragment implements AdminPropiedadAdapter.OnAdminPropiedadListener {

    private final List<Propiedad> allProperties = new ArrayList<>();
    private final List<Propiedad> filteredProperties = new ArrayList<>();

    // Elementos de la interfaz
    private MaterialButton btnFiltros;
    private MaterialButton btnOrden;
    private TextInputEditText etBuscarPropiedades;
    private RecyclerView rvPropiedades;
    private AdminPropiedadAdapter adapter;
    private LinearLayout layoutEmpty;
    private MaterialButton btnEmptyReset;

    // ViewModel
    private PropiedadViewModel propiedadViewModel;

    // Estado actual de filtro y orden
    private String selectedFilter = "Todos"; // "Todos", "Publicados", "Pausados", "Finalizados"
    private String selectedSort = "Recientes"; // "Recientes", "Mayor Monto", "Menor Monto"
    private String searchQuery = "";

    public AdminPropiedadesFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_propiedades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);

        // Floating Action Button - Agregar Propiedad
        FloatingActionButton fabAddProperty = view.findViewById(R.id.fab_add_property);
        if (fabAddProperty != null) {
            fabAddProperty.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }

        // Elementos de Búsqueda, Filtros y Orden
        btnFiltros = view.findViewById(R.id.btn_filtros_propiedades);
        btnOrden = view.findViewById(R.id.btn_orden_propiedades);
        etBuscarPropiedades = view.findViewById(R.id.et_buscar_propiedades);
        rvPropiedades = view.findViewById(R.id.rv_admin_propiedades);
        layoutEmpty = view.findViewById(R.id.layout_empty_propiedades);
        btnEmptyReset = view.findViewById(R.id.btn_empty_reset_filtros);

        // Configuración de RecyclerView y Adapter
        if (rvPropiedades != null) {
            rvPropiedades.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new AdminPropiedadAdapter(this);
            rvPropiedades.setAdapter(adapter);
        }

        // Botón Filtros: Todos, Publicados, Pausados, Finalizados
        if (btnFiltros != null) {
            btnFiltros.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), btnFiltros);
                popup.getMenu().add("Todos");
                popup.getMenu().add("Publicados");
                popup.getMenu().add("Pausados");
                popup.getMenu().add("Finalizados");

                popup.setOnMenuItemClickListener(item -> {
                    selectedFilter = item.getTitle().toString();
                    actualizarEstiloBotonFiltros();
                    aplicarFiltrosYOrden();
                    Toast.makeText(requireContext(), "Filtro: " + selectedFilter, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Botón Orden: Recientes, Mayor Monto, Menor Monto
        if (btnOrden != null) {
            btnOrden.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), btnOrden);
                popup.getMenu().add("Recientes");
                popup.getMenu().add("Mayor Monto");
                popup.getMenu().add("Menor Monto");

                popup.setOnMenuItemClickListener(item -> {
                    selectedSort = item.getTitle().toString();
                    actualizarEstiloBotonOrden();
                    aplicarFiltrosYOrden();
                    Toast.makeText(requireContext(), "Orden: " + selectedSort, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Botón de restablecer filtros cuando no hay resultados
        if (btnEmptyReset != null) {
            btnEmptyReset.setOnClickListener(v -> restablecerFiltros());
        }

        // Búsqueda en tiempo real
        if (etBuscarPropiedades != null) {
            etBuscarPropiedades.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim() : "";
                    aplicarFiltrosYOrden();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Observar datos reales de la base de datos
        propiedadViewModel.getAllPropiedades().observe(getViewLifecycleOwner(), propiedades -> {
            allProperties.clear();
            if (propiedades != null) {
                allProperties.addAll(propiedades);
            }
            aplicarFiltrosYOrden();
        });
    }

    private void aplicarFiltrosYOrden() {
        if (adapter == null || getContext() == null) return;

        filteredProperties.clear();

        for (Propiedad prop : allProperties) {
            if (prop == null) continue;

            // Filtro por texto de búsqueda (título, domicilio, ciudad)
            boolean matchesSearch = true;
            if (!searchQuery.isEmpty()) {
                String q = searchQuery.toLowerCase();
                String tit = prop.titulo != null ? prop.titulo.toLowerCase() : "";
                String dom = prop.domicilio != null ? prop.domicilio.toLowerCase() : "";
                String ciu = prop.ciudad != null ? prop.ciudad.toLowerCase() : "";
                matchesSearch = tit.contains(q) || dom.contains(q) || ciu.contains(q);
            }

            // Determinar estado de la propiedad
            boolean isPausado = false;
            boolean isFinalizado = false;
            if (prop.estado != null) {
                String est = prop.estado.trim();
                if (est.equalsIgnoreCase("Pausado") || est.equalsIgnoreCase("PAUSADO") || est.equalsIgnoreCase("PAUSADA") || est.equalsIgnoreCase("NO_VISIBLE") || est.equalsIgnoreCase("Inactivo")) {
                    isPausado = true;
                } else if (est.equalsIgnoreCase("Finalizado") || est.equalsIgnoreCase("VENDIDA") || est.equalsIgnoreCase("Vendido") || est.equalsIgnoreCase("FINALIZADO")) {
                    isFinalizado = true;
                }
            }

            // Filtro por Estado: Todos, Publicados, Pausados, Finalizados
            boolean matchesFilter = true;
            if ("Publicados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = !isPausado && !isFinalizado;
            } else if ("Pausados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = isPausado;
            } else if ("Finalizados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = isFinalizado;
            }

            if (matchesSearch && matchesFilter) {
                filteredProperties.add(prop);
            }
        }

        // Ordenamiento: Recientes, Mayor Monto, Menor Monto
        Collections.sort(filteredProperties, (a, b) -> {
            switch (selectedSort) {
                case "Mayor Monto":
                    return Double.compare(b.precio, a.precio);
                case "Menor Monto":
                    return Double.compare(a.precio, b.precio);
                case "Recientes":
                default:
                    if (a.createdAt != null && b.createdAt != null) {
                        return b.createdAt.compareTo(a.createdAt);
                    }
                    return 0;
            }
        });

        // Manejar estado vacío y mostrar todas las propiedades filtradas
        boolean isEmpty = filteredProperties.isEmpty();
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvPropiedades != null) {
            rvPropiedades.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        if (isEmpty) {
            adapter.setPropiedades(Collections.emptyList());
        } else {
            // Mostrar todas las propiedades sin paginación
            adapter.setPropiedades(new ArrayList<>(filteredProperties));
        }
    }

    @Override
    public void onSwitchVisibilidad(Propiedad propiedad, boolean isChecked) {
        if (propiedad == null) return;
        // Cambiar el estado rápidamente en base de datos Room y sincronización con Supabase
        propiedad.estado = isChecked ? "Publicado" : "Pausado";
        propiedadViewModel.update(propiedad);

        String status = isChecked ? "activada" : "pausada";
        if (getView() != null) {
            Snackbar.make(getView(), "Propiedad " + status + " en el catálogo", Snackbar.LENGTH_SHORT).show();
        }

        if (!isChecked && getContext() != null) {
            com.example.inmobiliariaventa.data.repository.ActividadRepository.getInstance(requireContext())
                    .registrarBajaPropiedad(propiedad.titulo, propiedad.precio, "Retirada de catálogo (Pausada)");
        }

        // Re-filtrar si hay un filtro de estado activo
        if (!"Todos".equalsIgnoreCase(selectedFilter)) {
            aplicarFiltrosYOrden();
        }
    }

    @Override
    public void onModificar(Propiedad propiedad) {
        if (propiedad == null || getView() == null) return;
        Bundle args = new Bundle();
        args.putString("propiedad_id", propiedad.id);
        Navigation.findNavController(getView()).navigate(R.id.nav_admin_agregar_propiedad, args);
    }

    @Override
    public void onBorrar(Propiedad propiedad) {
        if (propiedad == null || getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar propiedad")
                .setMessage("¿Estás seguro de que deseas eliminar \"" + (propiedad.titulo != null ? propiedad.titulo : "esta propiedad") + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    propiedadViewModel.delete(propiedad);
                    if (getContext() != null) {
                        com.example.inmobiliariaventa.data.repository.ActividadRepository.getInstance(requireContext())
                                .registrarBajaPropiedad(propiedad.titulo, propiedad.precio, "Eliminada del catálogo");
                    }
                    if (getView() != null) {
                        Snackbar.make(getView(), "Propiedad eliminada correctamente", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDetalle(Propiedad propiedad) {
        if (propiedad == null || getView() == null) return;
        Bundle args = new Bundle();
        args.putString("propiedad_id", propiedad.id);
        Navigation.findNavController(getView()).navigate(R.id.nav_detalle_propiedad, args);
    }

    private void actualizarEstiloBotonFiltros() {
        if (btnFiltros == null || getContext() == null) return;
        btnFiltros.setText("FILTROS (" + selectedFilter + ")");
        btnFiltros.setStrokeColorResource(R.color.colorOutlineVariant);
        btnFiltros.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
        btnFiltros.setIconTintResource(R.color.colorOnSurface);
    }

    private void actualizarEstiloBotonOrden() {
        if (btnOrden == null || getContext() == null) return;
        btnOrden.setText("ORDEN (" + selectedSort + ")");
        btnOrden.setStrokeColorResource(R.color.colorOutlineVariant);
        btnOrden.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
        btnOrden.setIconTintResource(R.color.colorOnSurface);
    }

    private void restablecerFiltros() {
        selectedFilter = "Todos";
        selectedSort = "Recientes";
        searchQuery = "";

        if (etBuscarPropiedades != null) {
            etBuscarPropiedades.setText("");
        }

        if (btnFiltros != null) btnFiltros.setText("FILTROS");
        if (btnOrden != null) btnOrden.setText("ORDEN");
        aplicarFiltrosYOrden();

        if (getView() != null) {
            Snackbar.make(getView(), "Filtros y orden restablecidos", Snackbar.LENGTH_SHORT).show();
        }
    }
}
