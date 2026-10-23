package com.example.inmobiliariaventa.ui.admin;

import com.example.inmobiliariaventa.utils.CartelUtils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.domain.entity.Contrato;
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
    private final List<Contrato> allContracts = new ArrayList<>();

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
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

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
            rvPropiedades.setHasFixedSize(true);
            rvPropiedades.setItemViewCacheSize(6);
            adapter = new AdminPropiedadAdapter(this);
            rvPropiedades.setAdapter(adapter);
        }

        // Botón Filtros: Todos, Publicados, Pausados
        if (btnFiltros != null) {
            btnFiltros.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), btnFiltros);
                popup.getMenu().add("Todos");
                popup.getMenu().add("Publicados");
                popup.getMenu().add("Pausados");

                popup.setOnMenuItemClickListener(item -> {
                    selectedFilter = item.getTitle().toString();
                    actualizarEstiloBotonFiltros();
                    aplicarFiltrosYOrden();
                    CartelUtils.mostrar(requireContext(), "Filtro: " + selectedFilter);
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
                    CartelUtils.mostrar(requireContext(), "Orden: " + selectedSort);
                    return true;
                });
                popup.show();
            });
        }

        // Botón de restablecer filtros cuando no hay resultados
        if (btnEmptyReset != null) {
            btnEmptyReset.setOnClickListener(v -> restablecerFiltros());
        }

        // Búsqueda en tiempo real con debounce para evitar lag al tipear
        if (etBuscarPropiedades != null) {
            etBuscarPropiedades.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim() : "";
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> aplicarFiltrosYOrden();
                    searchHandler.postDelayed(searchRunnable, 150);
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

        // Observar contratos para impedir reactivar propiedades con contrato Activo o Pendiente
        AppDatabase.getInstance(requireContext()).contratoDao().obtenerTodosLiveData()
                .observe(getViewLifecycleOwner(), contratos -> {
                    allContracts.clear();
                    if (contratos != null) {
                        allContracts.addAll(contratos);
                    }
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

            // Determinar estado de la propiedad (Publicado o Pausado)
            boolean isPausado = prop.isPausada();

            // Filtro por Estado: Todos, Publicados, Pausados
            boolean matchesFilter = true;
            if ("Publicados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = !isPausado;
            } else if ("Pausados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = isPausado;
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

        // Si intenta activar la propiedad, verificar que no tenga un contrato Activo o Pendiente
        if (isChecked) {
            Contrato contratoVigente = buscarContratoActivoOPendiente(propiedad);
            if (contratoVigente != null) {
                // Revertir el switch inmediatamente en la vista
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                mostrarDialogoAvisoContrato(propiedad, contratoVigente, "activar");
                return;
            }
        }

        // Cambiar el estado rápidamente en base de datos Room y sincronización con Supabase
        propiedad.estado = isChecked ? "Publicado" : "Pausado";
        propiedadViewModel.update(propiedad);

        String status = isChecked ? "activada" : "pausada";
        if (getView() != null) {
            CartelUtils.mostrar(getView(), "Propiedad " + status + " en el catálogo");
        }

        if (!isChecked && getContext() != null) {
            com.example.inmobiliariaventa.data.repository.ActividadRepository.getInstance(requireContext())
                    .registrarBajaPropiedad(propiedad.titulo, propiedad.precio, "Retirada de catálogo (Pausada)");
        } else if (isChecked && getContext() != null) {
            com.example.inmobiliariaventa.data.repository.ActividadRepository.getInstance(requireContext())
                    .notificarReactivacionPropiedad(propiedad);
        }

        // Re-filtrar si hay un filtro de estado activo
        if (!"Todos".equalsIgnoreCase(selectedFilter)) {
            aplicarFiltrosYOrden();
        }
    }

    private Contrato buscarContratoActivoOPendiente(Propiedad propiedad) {
        for (Contrato c : allContracts) {
            String est = c.getEstadoNormalizado();
            if (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(est) || Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(est)) {
                boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(propiedad.id);
                boolean matchTitulo = c.propiedadTitulo != null && propiedad.titulo != null
                        && c.propiedadTitulo.trim().equalsIgnoreCase(propiedad.titulo.trim());
                if (matchId || matchTitulo) {
                    return c;
                }
            }
        }
        return null;
    }

    private void mostrarDialogoAvisoContrato(Propiedad propiedad, Contrato contrato, String accion) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_aviso_propiedad_contrato, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_mensaje);
        MaterialButton btnCerrar = dialogView.findViewById(R.id.btn_dialog_aviso_cerrar);
        MaterialButton btnVerContratos = dialogView.findViewById(R.id.btn_dialog_aviso_ver_contratos);

        if (tvMensaje != null) {
            String cliente = contrato.getNombreCompletoCliente();
            String estadoContrato = contrato.getEstadoNormalizado();
            String instruccion;
            if ("eliminar".equalsIgnoreCase(accion)) {
                instruccion = "Para poder eliminarla del sistema, primero debés dar de baja el contrato.";
            } else if ("modificar".equalsIgnoreCase(accion)) {
                instruccion = "Para poder modificar sus datos o su precio, primero debés dar de baja el contrato.";
            } else {
                instruccion = "Para volver a publicarla en el catálogo, primero debés dar de baja el contrato.";
            }
            tvMensaje.setText("No es posible " + accion + " \"" + (propiedad.titulo != null ? propiedad.titulo : "esta propiedad")
                    + "\" porque posee un contrato en estado " + estadoContrato + " a nombre de " + cliente
                    + ".\n\n" + instruccion);
        }

        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnVerContratos != null) {
            btnVerContratos.setOnClickListener(v -> {
                dialog.dismiss();
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.nav_contratos);
                }
            });
        }

        dialog.show();
    }

    @Override
    public void onModificar(Propiedad propiedad) {
        if (propiedad == null || getView() == null) return;

        // No permitir modificar una propiedad si tiene un contrato Activo o Pendiente
        Contrato contratoVigente = buscarContratoActivoOPendiente(propiedad);
        if (contratoVigente != null) {
            mostrarDialogoAvisoContrato(propiedad, contratoVigente, "modificar");
            return;
        }

        Bundle args = new Bundle();
        args.putString("propiedad_id", propiedad.id);
        Navigation.findNavController(getView()).navigate(R.id.nav_admin_agregar_propiedad, args);
    }

    @Override
    public void onBorrar(Propiedad propiedad) {
        if (propiedad == null || getContext() == null) return;

        // No permitir borrar una propiedad si tiene un contrato Activo o Pendiente
        Contrato contratoVigente = buscarContratoActivoOPendiente(propiedad);
        if (contratoVigente != null) {
            mostrarDialogoAvisoContrato(propiedad, contratoVigente, "eliminar");
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_aviso_propiedad_contrato, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_mensaje);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_aviso_cerrar);
        MaterialButton btnConfirmar = dialogView.findViewById(R.id.btn_dialog_aviso_ver_contratos);

        if (tvTitulo != null) {
            tvTitulo.setText("¿Eliminar propiedad?");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("¿Estás seguro de que deseas eliminar \"" + (propiedad.titulo != null ? propiedad.titulo : "esta propiedad") + "\"?\n\nEsta acción quitará la propiedad del catálogo y no se puede deshacer.");
        }
        if (btnCancelar != null) {
            btnCancelar.setText("No, volver");
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnConfirmar != null) {
            btnConfirmar.setText("Sí, eliminar");
            btnConfirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BA1A1A")));
            btnConfirmar.setTextColor(Color.WHITE);
            btnConfirmar.setOnClickListener(v -> {
                dialog.dismiss();
                propiedadViewModel.delete(propiedad);
                if (getContext() != null) {
                    com.example.inmobiliariaventa.data.repository.ActividadRepository.getInstance(requireContext())
                            .registrarBajaPropiedad(propiedad.titulo, propiedad.precio, "Eliminada del catálogo");
                }
                if (getView() != null) {
                    CartelUtils.mostrar(getView(), "Propiedad eliminada correctamente");
                }
            });
        }

        dialog.show();
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
            CartelUtils.mostrar(getView(), "Filtros y orden restablecidos");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (propiedadViewModel != null) {
            propiedadViewModel.sincronizarConNube(null);
        }
    }
}
