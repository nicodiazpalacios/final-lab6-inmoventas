package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.ActividadRepository;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdminContratosFragment extends Fragment {

    private static final int PAGE_SIZE = 6;

    private TextInputEditText etBuscarContrato;
    private LinearLayout layoutContractsContainer;
    private View layoutEmptyContracts;
    private TextView tvEmptyContractsTitle;
    private TextView tvEmptyContractsSubtitle;
    private View layoutPagination;
    private FloatingActionButton fabNuevoContrato;

    private final List<Contrato> allContratos = new ArrayList<>();
    private String currentStatusFilter = "Todos";
    private String currentSearchQuery = "";
    private String currentSortOrder = "Recientes";

    private int currentPage = 1;
    private int startPage = 1;
    private int maxPages = 1;

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

    public AdminContratosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_contratos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etBuscarContrato = view.findViewById(R.id.et_buscar_contrato);
        layoutContractsContainer = view.findViewById(R.id.layout_contracts_container);
        layoutEmptyContracts = view.findViewById(R.id.layout_empty_contracts);
        tvEmptyContractsTitle = view.findViewById(R.id.tv_empty_contracts_title);
        tvEmptyContractsSubtitle = view.findViewById(R.id.tv_empty_contracts_subtitle);
        layoutPagination = view.findViewById(R.id.layout_pagination);

        fabNuevoContrato = view.findViewById(R.id.fab_nuevo_contrato);
        if (fabNuevoContrato != null) {
            fabNuevoContrato.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_nav_contratos_to_nav_admin_agregar_contrato)
            );
        }

        MaterialButton btnFilterStatus = view.findViewById(R.id.btn_filter_status);
        if (btnFilterStatus != null) {
            btnFilterStatus.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), btnFilterStatus);
                popup.getMenu().add("Todos");
                popup.getMenu().add("Activo");
                popup.getMenu().add("Pendiente");
                popup.getMenu().add("Cancelado");
                popup.setOnMenuItemClickListener(item -> {
                    currentStatusFilter = item.getTitle().toString();
                    btnFilterStatus.setText("Todos".equalsIgnoreCase(currentStatusFilter)
                            ? "FILTROS"
                            : "FILTROS (" + currentStatusFilter + ")");
                    currentPage = 1;
                    startPage = 1;
                    renderContractsList();
                    Toast.makeText(requireContext(), "Filtro: " + currentStatusFilter, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        MaterialButton btnSortContracts = view.findViewById(R.id.btn_sort_contracts);
        if (btnSortContracts != null) {
            btnSortContracts.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), btnSortContracts);
                popup.getMenu().add("Recientes");
                popup.getMenu().add("Mayor Monto");
                popup.getMenu().add("Menor Monto");
                popup.setOnMenuItemClickListener(item -> {
                    currentSortOrder = item.getTitle().toString();
                    btnSortContracts.setText("ORDEN (" + currentSortOrder + ")");
                    currentPage = 1;
                    startPage = 1;
                    renderContractsList();
                    Toast.makeText(requireContext(), "Orden: " + currentSortOrder, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        setupSearchFilter();
        setupPagination(view);
        observeContracts();
    }

    private void observeContracts() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.contratoDao().obtenerTodosLiveData().observe(getViewLifecycleOwner(), contratos -> {
            allContratos.clear();
            if (contratos != null) {
                allContratos.addAll(contratos);
                sincronizarPropiedadesPausadasPorContratos(new ArrayList<>(contratos));
            }
            renderContractsList();
        });
        sincronizarContratosConSupabase();
    }

    private void sincronizarContratosConSupabase() {
        if (getActivity() == null) return;
        android.app.Application appContext = requireActivity().getApplication();
        AppDatabase db = AppDatabase.getInstance(appContext);

        // 1. Subir todos los contratos locales a Supabase (upsert) para asegurar que ninguno quede solo local
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Contrato> locales = db.contratoDao().obtenerTodosSync();
            if (locales != null) {
                for (Contrato local : locales) {
                    SupabaseClient.guardarContrato(local, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                local.estadoSincronizacion = "SINCRONIZADO";
                                db.contratoDao().insertar(local);
                            });
                        }

                        @Override
                        public void onError(Exception e) { }
                    });
                }
            }
        });

        // 2. Descargar contratos desde Supabase y actualizar Room
        SupabaseClient.obtenerContratos(new SupabaseClient.SupabaseCallback<List<Contrato>>() {
            @Override
            public void onSuccess(List<Contrato> remotos) {
                if (remotos == null || remotos.isEmpty()) return;
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    List<com.example.inmobiliariaventa.domain.entity.Propiedad> todasProps = db.propiedadDao().obtenerTodasSync();
                    for (Contrato c : remotos) {
                        if ((c.propiedadTitulo == null || c.propiedadTitulo.isEmpty())
                                && c.propiedadId != null && todasProps != null) {
                            for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todasProps) {
                                if (c.propiedadId.equals(p.id) && p.titulo != null) {
                                    c.propiedadTitulo = p.titulo;
                                    break;
                                }
                            }
                        }
                    }
                    db.contratoDao().insertarTodos(remotos);
                });
            }

            @Override
            public void onError(Exception e) { }
        });
    }

    private void sincronizarPropiedadesPausadasPorContratos(List<Contrato> contratos) {
        if (getActivity() == null) return;
        android.app.Application appContext = requireActivity().getApplication();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            List<com.example.inmobiliariaventa.domain.entity.Propiedad> todas = db.propiedadDao().obtenerTodasSync();
            if (todas == null || todas.isEmpty()) return;

            com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl repo = null;
            for (Contrato c : contratos) {
                if (c.estado != null && ("Activo".equalsIgnoreCase(c.estado) || "Pendiente".equalsIgnoreCase(c.estado))) {
                    for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todas) {
                        boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(p.id);
                        boolean matchTitulo = c.propiedadTitulo != null && p.titulo != null && c.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                        if ((matchId || matchTitulo) && !p.isPausada()) {
                            p.estado = "Pausado";
                            if (repo == null) {
                                repo = new com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl(appContext);
                            }
                            repo.update(p);
                        }
                    }
                }
            }
        });
    }

    private void renderContractsList() {
        if (layoutContractsContainer == null || getContext() == null) return;

        List<Contrato> filtered = new ArrayList<>();
        for (Contrato c : allContratos) {
            if (!"Todos".equalsIgnoreCase(currentStatusFilter)) {
                String est = c.estado != null ? c.estado : "";
                if ("Rechazado".equalsIgnoreCase(est) || "Finalizado".equalsIgnoreCase(est)) {
                    est = "Cancelado";
                }
                if (!est.equalsIgnoreCase(currentStatusFilter)) {
                    continue;
                }
            }

            if (!currentSearchQuery.isEmpty()) {
                String fullClient = c.getNombreCompletoCliente().toLowerCase(Locale.ROOT);
                String prop = (c.propiedadTitulo != null ? c.propiedadTitulo : "").toLowerCase(Locale.ROOT);
                String dni = (c.clienteDni != null ? c.clienteDni : "").toLowerCase(Locale.ROOT);
                if (!fullClient.contains(currentSearchQuery) && !prop.contains(currentSearchQuery) && !dni.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filtered.add(c);
        }

        if ("Mayor Monto".equals(currentSortOrder)) {
            Collections.sort(filtered, (a, b) -> Double.compare(
                    b.montoTotal != null ? b.montoTotal : 0.0,
                    a.montoTotal != null ? a.montoTotal : 0.0));
        } else if ("Menor Monto".equals(currentSortOrder)) {
            Collections.sort(filtered, (a, b) -> Double.compare(
                    a.montoTotal != null ? a.montoTotal : 0.0,
                    b.montoTotal != null ? b.montoTotal : 0.0));
        } else {
            Collections.sort(filtered, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        }

        layoutContractsContainer.removeAllViews();

        if (filtered.isEmpty()) {
            if (layoutEmptyContracts != null) {
                layoutEmptyContracts.setVisibility(View.VISIBLE);
                if (allContratos.isEmpty()) {
                    if (tvEmptyContractsTitle != null) tvEmptyContractsTitle.setText("No hay contratos cargados");
                    if (tvEmptyContractsSubtitle != null) {
                        tvEmptyContractsSubtitle.setText("Presiona el botón + abajo a la derecha para registrar el primer contrato en el sistema.");
                    }
                } else {
                    if (tvEmptyContractsTitle != null) tvEmptyContractsTitle.setText("Sin resultados");
                    if (tvEmptyContractsSubtitle != null) {
                        tvEmptyContractsSubtitle.setText("No se encontraron contratos que coincidan con la búsqueda o el filtro actual.");
                    }
                }
            }
            if (layoutPagination != null) {
                layoutPagination.setVisibility(View.GONE);
            }
            return;
        }

        if (layoutEmptyContracts != null) {
            layoutEmptyContracts.setVisibility(View.GONE);
        }

        maxPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (currentPage > maxPages) {
            currentPage = maxPages;
            startPage = currentPage;
        }

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());
        List<Contrato> pageItems = filtered.subList(fromIndex, toIndex);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMaximumFractionDigits(0);

        for (Contrato contrato : pageItems) {
            View cardView = inflater.inflate(R.layout.item_contrato_card, layoutContractsContainer, false);

            TextView tvCliente = cardView.findViewById(R.id.tv_cliente);
            TextView tvPropiedad = cardView.findViewById(R.id.tv_propiedad);
            MaterialCardView badgeWrapper = cardView.findViewById(R.id.badge_wrapper);
            TextView tvBadgeStatus = cardView.findViewById(R.id.tv_badge_status);
            TextView tvMonto = cardView.findViewById(R.id.tv_monto);
            TextView tvLabelVencimiento = cardView.findViewById(R.id.tv_label_vencimiento);
            TextView tvVencimiento = cardView.findViewById(R.id.tv_vencimiento);
            TextView tvPlan = cardView.findViewById(R.id.tv_plan);
            MaterialButton btnPdf = cardView.findViewById(R.id.btn_pdf);
            MaterialButton btnModificar = cardView.findViewById(R.id.btn_modificar);
            MaterialButton btnBaja = cardView.findViewById(R.id.btn_baja);

            String clienteFullName = contrato.getNombreCompletoCliente();
            String propiedadName = contrato.propiedadTitulo != null && !contrato.propiedadTitulo.isEmpty()
                    ? contrato.propiedadTitulo
                    : contrato.propiedadId;

            double montoVal = contrato.montoTotal != null ? contrato.montoTotal : 0.0;
            String montoFormatted = "$ " + currencyFormat.format(montoVal);

            tvCliente.setText(clienteFullName);
            tvPropiedad.setText(propiedadName);
            tvMonto.setText(montoFormatted);

            int cuotasVal = contrato.cuotas != null ? contrato.cuotas : 1;
            if (!"Crédito".equalsIgnoreCase(contrato.metodoPago) || cuotasVal <= 1) {
                String metodo = contrato.metodoPago != null && !contrato.metodoPago.isEmpty() ? contrato.metodoPago : "Efectivo";
                tvPlan.setText("Contado (" + metodo + ")");
                if (tvLabelVencimiento != null) tvLabelVencimiento.setText("FECHA FIRMA");
                tvVencimiento.setText(contrato.fechaInicio != null && !contrato.fechaInicio.isEmpty() ? contrato.fechaInicio : "-");
            } else {
                tvPlan.setText(cuotasVal + " Cuotas");
                if (tvLabelVencimiento != null) tvLabelVencimiento.setText("VENCIMIENTO");
                tvVencimiento.setText(contrato.fechaVencimiento != null && !contrato.fechaVencimiento.isEmpty()
                        ? contrato.fechaVencimiento
                        : (contrato.fechaInicio != null ? contrato.fechaInicio : "-"));
            }

            String estadoActual = contrato.estado != null ? contrato.estado : "Activo";
            if ("Rechazado".equalsIgnoreCase(estadoActual) || "Finalizado".equalsIgnoreCase(estadoActual)) {
                estadoActual = "Cancelado";
            }
            actualizarBadgeSituacion(badgeWrapper, tvBadgeStatus, estadoActual);

            final String finalClienteName = clienteFullName;
            final String finalPropiedadName = propiedadName;
            final String finalMonto = montoFormatted;

            if (btnPdf != null) {
                btnPdf.setOnClickListener(v ->
                        com.example.inmobiliariaventa.utils.ContratoPdfGenerator.mostrarDialogoExportarPdf(requireContext(), contrato)
                );
            }

            btnModificar.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("contrato_id", contrato.id);
                Navigation.findNavController(v).navigate(R.id.action_nav_contratos_to_nav_admin_agregar_contrato, args);
            });

            if ("Cancelado".equalsIgnoreCase(estadoActual)) {
                btnBaja.setEnabled(false);
                btnBaja.setAlpha(0.55f);
                btnBaja.setText("Cancelado");
                btnBaja.setOnClickListener(null);
            } else {
                btnBaja.setEnabled(true);
                btnBaja.setAlpha(1.0f);
                btnBaja.setText("Dar de baja");
                btnBaja.setOnClickListener(v -> mostrarDialogoConfirmarBaja(contrato, finalClienteName, finalPropiedadName, finalMonto));
            }

            layoutContractsContainer.addView(cardView);
        }

        if (layoutPagination != null) {
            layoutPagination.setVisibility(maxPages > 1 ? View.VISIBLE : View.GONE);
        }
        renderPagination(false);
    }

    private void mostrarDialogoConfirmarBaja(Contrato contrato, String clienteName, String propiedadName, String monto) {
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

        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_mensaje);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_aviso_cerrar);
        MaterialButton btnConfirmar = dialogView.findViewById(R.id.btn_dialog_aviso_ver_contratos);

        if (tvTitulo != null) {
            tvTitulo.setText("¿Dar de baja el contrato?");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("¿Estás seguro de que deseas dar de baja el contrato de " + clienteName
                    + " para \"" + propiedadName + "\"?\n\nEl contrato pasará a estado Cancelado y la propiedad volverá a publicarse en el catálogo.");
        }
        if (btnCancelar != null) {
            btnCancelar.setText("No, volver");
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnConfirmar != null) {
            btnConfirmar.setText("Sí, dar de baja");
            btnConfirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BA1A1A")));
            btnConfirmar.setTextColor(Color.WHITE);
            btnConfirmar.setOnClickListener(v -> {
                dialog.dismiss();
                darDeBajaContrato(contrato);
                ActividadRepository.getInstance(requireContext())
                        .registrarModificacionContrato(clienteName, propiedadName, monto, "Cancelado (Baja)");
                Toast.makeText(requireContext(), "Contrato de " + clienteName + " cancelado", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void darDeBajaContrato(Contrato contrato) {
        contrato.estado = "Cancelado";
        contrato.estadoSincronizacion = "PENDIENTE_SUBIR";
        if (getActivity() == null) return;
        android.app.Application appContext = requireActivity().getApplication();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            db.contratoDao().actualizar(contrato);

            SupabaseClient.guardarContrato(contrato, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        contrato.estadoSincronizacion = "SINCRONIZADO";
                        db.contratoDao().actualizar(contrato);
                    });
                }

                @Override
                public void onError(Exception e) {
                    SupabaseClient.actualizarEstadoContrato(contrato.id, "Cancelado", null);
                }
            });

            List<com.example.inmobiliariaventa.domain.entity.Propiedad> todas = db.propiedadDao().obtenerTodasSync();
            List<Contrato> todosContratos = db.contratoDao().obtenerTodosSync();
            if (todas != null) {
                for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todas) {
                    boolean matchId = contrato.propiedadId != null && !contrato.propiedadId.isEmpty() && contrato.propiedadId.equals(p.id);
                    boolean matchTitulo = contrato.propiedadTitulo != null && p.titulo != null && contrato.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                    if ((matchId || matchTitulo) && p.isPausada()) {
                        boolean otroVigente = false;
                        if (todosContratos != null) {
                            for (Contrato c : todosContratos) {
                                if (!c.id.equals(contrato.id) && c.estado != null
                                        && ("Activo".equalsIgnoreCase(c.estado) || "Pendiente".equalsIgnoreCase(c.estado))) {
                                    boolean mId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(p.id);
                                    boolean mTit = c.propiedadTitulo != null && p.titulo != null && c.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                                    if (mId || mTit) {
                                        otroVigente = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!otroVigente) {
                            p.estado = "Publicado";
                            new com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl(appContext).update(p);
                        }
                    }
                }
            }
        });
    }

    private void actualizarBadgeSituacion(MaterialCardView badgeWrapper, TextView tvBadgeStatus, String situacion) {
        if (tvBadgeStatus != null) {
            tvBadgeStatus.setText(situacion);
        }
        if (badgeWrapper != null) {
            int color;
            switch (situacion) {
                case "Activo":
                    color = android.graphics.Color.parseColor("#006C49");
                    break;
                case "Pendiente":
                    color = android.graphics.Color.parseColor("#BA8400");
                    break;
                case "Cancelado":
                default:
                    color = android.graphics.Color.parseColor("#BA1A1A");
                    break;
            }
            badgeWrapper.setCardBackgroundColor(color);
        }
    }

    private void setupSearchFilter() {
        if (etBuscarContrato == null) return;

        etBuscarContrato.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                currentPage = 1;
                startPage = 1;
                renderContractsList();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setupPagination(View root) {
        btnPagePrev = root.findViewById(R.id.btn_page_prev);
        btnPageNext = root.findViewById(R.id.btn_page_next);
        tvPagePrev = root.findViewById(R.id.tv_page_prev);
        tvPageNext = root.findViewById(R.id.tv_page_next);

        cardPage1 = root.findViewById(R.id.card_page_1);
        cardPage2 = root.findViewById(R.id.card_page_2);
        cardPage3 = root.findViewById(R.id.card_page_3);
        cardPage4 = root.findViewById(R.id.card_page_4);

        tvPage1 = root.findViewById(R.id.tv_page_1);
        tvPage2 = root.findViewById(R.id.tv_page_2);
        tvPage3 = root.findViewById(R.id.tv_page_3);
        tvPage4 = root.findViewById(R.id.tv_page_4);

        if (btnPagePrev != null) {
            btnPagePrev.setOnClickListener(v -> {
                if (currentPage > 1) {
                    goToPage(currentPage - 1);
                }
            });
        }

        if (btnPageNext != null) {
            btnPageNext.setOnClickListener(v -> {
                if (currentPage < maxPages) {
                    goToPage(currentPage + 1);
                }
            });
        }

        if (cardPage1 != null) {
            cardPage1.setOnClickListener(v -> goToPage(startPage));
        }
        if (cardPage2 != null) {
            cardPage2.setOnClickListener(v -> goToPage(startPage + 1));
        }
        if (cardPage3 != null) {
            cardPage3.setOnClickListener(v -> goToPage(startPage + 2));
        }
        if (cardPage4 != null) {
            cardPage4.setOnClickListener(v -> goToPage(startPage + 3));
        }

        renderPagination(false);
    }

    private void goToPage(int page) {
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        if (page == currentPage && startPage == page) return;

        currentPage = page;
        startPage = currentPage;
        renderContractsList();
        Toast.makeText(requireContext(), "Página " + currentPage + " de " + maxPages, Toast.LENGTH_SHORT).show();
    }

    private void renderPagination(boolean showToast) {
        if (cardPage1 == null) return;

        int slot1 = startPage;
        int slot2 = startPage + 1;
        int slot3 = startPage + 2;
        int slot4 = startPage + 3;

        tvPage1.setText(String.valueOf(slot1));
        tvPage2.setText(String.valueOf(slot2));
        tvPage3.setText(String.valueOf(slot3));
        tvPage4.setText(String.valueOf(slot4));

        cardPage2.setVisibility(slot2 <= maxPages ? View.VISIBLE : View.GONE);
        cardPage3.setVisibility(slot3 <= maxPages ? View.VISIBLE : View.GONE);
        cardPage4.setVisibility(slot4 <= maxPages ? View.VISIBLE : View.GONE);

        stylePageCard(cardPage1, tvPage1, true);
        stylePageCard(cardPage2, tvPage2, false);
        stylePageCard(cardPage3, tvPage3, false);
        stylePageCard(cardPage4, tvPage4, false);

        boolean canGoPrev = currentPage > 1;
        btnPagePrev.setEnabled(canGoPrev);
        btnPagePrev.setAlpha(canGoPrev ? 1.0f : 0.35f);

        boolean canGoNext = currentPage < maxPages;
        btnPageNext.setEnabled(canGoNext);
        btnPageNext.setAlpha(canGoNext ? 1.0f : 0.35f);

        if (showToast) {
            Toast.makeText(requireContext(), "Página " + currentPage + " de " + maxPages, Toast.LENGTH_SHORT).show();
        }
    }

    private void stylePageCard(MaterialCardView card, TextView tv, boolean isSelected) {
        if (card == null || tv == null || getContext() == null) return;
        int dp1 = Math.round(1 * getResources().getDisplayMetrics().density);
        int dp2 = Math.round(2 * getResources().getDisplayMetrics().density);

        if (isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            card.setStrokeWidth(0);
            card.setCardElevation(dp2);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnPrimary));
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSurfaceContainerLowest));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.colorOutlineVariant));
            card.setStrokeWidth(dp1);
            card.setCardElevation(dp1);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant));
            tv.setTypeface(null, Typeface.NORMAL);
        }
    }
}
