package com.example.inmobiliariaventa.ui.admin;

import com.example.inmobiliariaventa.utils.CartelUtils;

import android.graphics.Color;
import android.content.res.ColorStateList;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.TextInputEditText;
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
import com.example.inmobiliariaventa.domain.entity.Pago;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
                popup.getMenu().add(Contrato.ESTADO_PENDIENTE);
                popup.getMenu().add(Contrato.ESTADO_COMPLETADO);
                popup.getMenu().add(Contrato.ESTADO_RECHAZADO);
                popup.setOnMenuItemClickListener(item -> {
                    currentStatusFilter = item.getTitle().toString();
                    btnFilterStatus.setText("Todos".equalsIgnoreCase(currentStatusFilter)
                            ? "FILTROS"
                            : "FILTROS (" + currentStatusFilter + ")");
                    currentPage = 1;
                    startPage = 1;
                    renderContractsList();
                    CartelUtils.mostrar(requireContext(), "Filtro: " + currentStatusFilter);
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
                    CartelUtils.mostrar(requireContext(), "Orden: " + currentSortOrder);
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
            AppDatabase.databaseWriteExecutor.execute(() -> {
                for (Contrato c : allContratos) {
                    c.setListaPagos(db.pagoDao().obtenerPorContratoSync(c.id));
                }
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> renderContractsList());
                }
            });
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
                    for (Contrato c : allContratos) {
                        c.setListaPagos(db.pagoDao().obtenerPorContratoSync(c.id));
                    }
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> renderContractsList());
                    }
                });
            }

            @Override
            public void onError(Exception e) { }
        });

        // 3. Descargar pagos desde Supabase
        SupabaseClient.obtenerPagos(new SupabaseClient.SupabaseCallback<List<Pago>>() {
            @Override
            public void onSuccess(List<Pago> remotos) {
                if (remotos == null || remotos.isEmpty()) return;
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    db.pagoDao().insertarTodos(remotos);
                    for (Contrato c : allContratos) {
                        c.setListaPagos(db.pagoDao().obtenerPorContratoSync(c.id));
                    }
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> renderContractsList());
                    }
                });
            }

            @Override
            public void onError(Exception e) { }
        });

        // 4. Subir pagos pendientes de sincronizar
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Pago> pendientes = db.pagoDao().obtenerPendientesDeSubir();
            if (pendientes != null) {
                for (Pago p : pendientes) {
                    SupabaseClient.guardarPago(p, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            p.estadoSincronizacion = "SINCRONIZADO";
                            AppDatabase.databaseWriteExecutor.execute(() -> db.pagoDao().insertar(p));
                        }
                        @Override
                        public void onError(Exception e) { }
                    });
                }
            }
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
                String est = c.getEstadoNormalizado();
                if (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(est)) {
                    for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todas) {
                        boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(p.id);
                        boolean matchTitulo = c.propiedadTitulo != null && p.titulo != null && c.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                        if ((matchId || matchTitulo) && !p.isPausada() && !"Vendida".equalsIgnoreCase(p.estado)) {
                            p.estado = "Pausado";
                            if (repo == null) {
                                repo = new com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl(appContext);
                            }
                            repo.update(p);
                        }
                    }
                } else if (Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(est)) {
                    for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todas) {
                        boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(p.id);
                        boolean matchTitulo = c.propiedadTitulo != null && p.titulo != null && c.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                        if ((matchId || matchTitulo) && !"Vendida".equalsIgnoreCase(p.estado)) {
                            p.estado = "Vendida";
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
                String est = c.getEstadoNormalizado();
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
            TextView tvAbonado = cardView.findViewById(R.id.tv_abonado);
            TextView tvAdeuda = cardView.findViewById(R.id.tv_adeuda);
            TextView tvPlan = cardView.findViewById(R.id.tv_plan);
            TextView tvVencimiento = cardView.findViewById(R.id.tv_vencimiento);
            com.google.android.material.progressindicator.LinearProgressIndicator progressCobranza = cardView.findViewById(R.id.progress_cobranza);
            TextView tvProgresoCuotas = cardView.findViewById(R.id.tv_progreso_cuotas);

            MaterialButton btnCobranzas = cardView.findViewById(R.id.btn_cobranzas);
            MaterialButton btnPdf = cardView.findViewById(R.id.btn_pdf);
            MaterialButton btnModificar = cardView.findViewById(R.id.btn_modificar);
            MaterialButton btnBaja = cardView.findViewById(R.id.btn_baja);

            String clienteFullName = contrato.getNombreCompletoCliente();
            String propiedadName = contrato.propiedadTitulo != null && !contrato.propiedadTitulo.isEmpty()
                    ? contrato.propiedadTitulo
                    : contrato.propiedadId;

            double montoVal = contrato.montoTotal != null ? contrato.montoTotal : 0.0;
            double abonadoVal = contrato.calcularTotalAbonado();
            double adeudaVal = contrato.calcularSaldoAdeudado();
            String estadoActual = contrato.getEstadoNormalizado();

            tvCliente.setText(clienteFullName);
            tvPropiedad.setText(propiedadName);
            tvMonto.setText("$ " + currencyFormat.format(montoVal));
            if (tvAbonado != null) tvAbonado.setText("$ " + currencyFormat.format(abonadoVal));

            if (tvAdeuda != null) {
                if (adeudaVal <= 0.001) {
                    tvAdeuda.setText("Saldado");
                    tvAdeuda.setTextColor(Color.parseColor("#006C49"));
                } else {
                    tvAdeuda.setText("$ " + currencyFormat.format(adeudaVal));
                    tvAdeuda.setTextColor(Color.parseColor("#B45309"));
                }
            }

            int pct = (montoVal > 0) ? (int) Math.min(100, Math.round((abonadoVal / montoVal) * 100)) : 100;
            if (progressCobranza != null) progressCobranza.setProgress(pct);

            int cuotasVal = contrato.cuotas != null && contrato.cuotas > 0 ? contrato.cuotas : 1;
            int cuotasPagadas = contrato.getCuotasPagadasCount();

            if ("Crédito".equalsIgnoreCase(contrato.metodoPago)) {
                double saldoFinanciado = Math.max(0.0, montoVal - (contrato.seniaMonto != null ? contrato.seniaMonto : 0.0));
                double valorCuota = saldoFinanciado / cuotasVal;
                tvPlan.setText(cuotasVal + (cuotasVal == 1 ? " Cuota ($ " : " Cuotas ($ ") + currencyFormat.format(valorCuota) + ")");
                tvVencimiento.setText("Vence: " + (contrato.fechaVencimiento != null && !contrato.fechaVencimiento.isEmpty()
                        ? contrato.fechaVencimiento
                        : (contrato.fechaInicio != null ? contrato.fechaInicio : "-")));
                if (tvProgresoCuotas != null) {
                    tvProgresoCuotas.setText(cuotasPagadas + " de " + cuotasVal + " cuotas cobradas (" + pct + "%)");
                }
            } else {
                String metodo = contrato.metodoPago != null && !contrato.metodoPago.isEmpty() ? contrato.metodoPago : "Efectivo";
                tvPlan.setText("Contado (" + metodo + ")");
                tvVencimiento.setText("Fecha: " + (contrato.fechaInicio != null && !contrato.fechaInicio.isEmpty() ? contrato.fechaInicio : "-"));
                if (tvProgresoCuotas != null) {
                    if (adeudaVal <= 0.001) {
                        tvProgresoCuotas.setText("100% abonado • Sin saldo pendiente");
                    } else {
                        tvProgresoCuotas.setText("Seña abonada • Saldo restante pendiente (" + pct + "%)");
                    }
                }
            }

            actualizarBadgeSituacion(badgeWrapper, tvBadgeStatus, estadoActual);

            final String finalClienteName = clienteFullName;
            final String finalPropiedadName = propiedadName;
            final String finalMonto = "$ " + currencyFormat.format(montoVal);

            if (btnCobranzas != null) {
                btnCobranzas.setOnClickListener(v -> mostrarDialogoCobranzas(contrato));
            }

            if (btnPdf != null) {
                btnPdf.setOnClickListener(v ->
                        com.example.inmobiliariaventa.utils.ContratoPdfGenerator.mostrarDialogoExportarPdf(requireContext(), contrato)
                );
            }

            if (btnModificar != null) {
                btnModificar.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("contrato_id", contrato.id);
                    Navigation.findNavController(v).navigate(R.id.action_nav_contratos_to_nav_admin_agregar_contrato, args);
                });
            }

            if (btnBaja != null) {
                if (Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(estadoActual)) {
                    btnBaja.setEnabled(false);
                    btnBaja.setAlpha(0.55f);
                    btnBaja.setText("Rechazado");
                    btnBaja.setOnClickListener(null);
                } else {
                    btnBaja.setEnabled(true);
                    btnBaja.setAlpha(1.0f);
                    btnBaja.setText("Rechazar");
                    btnBaja.setOnClickListener(v -> mostrarDialogoConfirmarBaja(contrato, finalClienteName, finalPropiedadName, finalMonto));
                }
            }

            layoutContractsContainer.addView(cardView);
        }

        if (layoutPagination != null) {
            layoutPagination.setVisibility(maxPages > 1 ? View.VISIBLE : View.GONE);
        }
        renderPagination(false);
    }

    private void mostrarDialogoCobranzas(Contrato contrato) {
        if (getContext() == null || contrato == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_gestion_cobranzas, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvSubtitulo = dialogView.findViewById(R.id.dialog_tv_contrato_subtitulo);
        TextView tvTotal = dialogView.findViewById(R.id.dialog_tv_resumen_total);
        TextView tvAbonado = dialogView.findViewById(R.id.dialog_tv_resumen_abonado);
        TextView tvAdeuda = dialogView.findViewById(R.id.dialog_tv_resumen_adeuda);
        com.google.android.material.progressindicator.LinearProgressIndicator progressBar = dialogView.findViewById(R.id.dialog_progress_cobranza);
        TextView tvPorcentaje = dialogView.findViewById(R.id.dialog_tv_progreso_porcentaje);
        TextView tvEstadoBadge = dialogView.findViewById(R.id.dialog_tv_estado_badge);
        LinearLayout layoutCuotas = dialogView.findViewById(R.id.dialog_layout_cuotas_container);
        View btnCerrarIcono = dialogView.findViewById(R.id.dialog_cobranzas_btn_cerrar);
        View btnCerrarPrincipal = dialogView.findViewById(R.id.dialog_btn_cerrar_principal);

        btnCerrarIcono.setOnClickListener(v -> dialog.dismiss());
        btnCerrarPrincipal.setOnClickListener(v -> dialog.dismiss());

        String propTitulo = contrato.propiedadTitulo != null ? contrato.propiedadTitulo : "Propiedad";
        tvSubtitulo.setText(contrato.getNombreCompletoCliente() + " • " + propTitulo);

        Runnable refrescarVista = new Runnable() {
            @Override
            public void run() {
                NumberFormat cf = NumberFormat.getNumberInstance(Locale.US);
                cf.setMaximumFractionDigits(0);

                double total = contrato.montoTotal != null ? contrato.montoTotal : 0.0;
                double abonado = contrato.calcularTotalAbonado();
                double adeuda = contrato.calcularSaldoAdeudado();
                int pct = total > 0 ? (int) Math.min(100, Math.round((abonado / total) * 100)) : 100;

                tvTotal.setText("$ " + cf.format(total));
                tvAbonado.setText("$ " + cf.format(abonado));
                if (adeuda <= 0.001) {
                    tvAdeuda.setText("Saldado ($ 0)");
                    tvAdeuda.setTextColor(Color.parseColor("#006C49"));
                } else {
                    tvAdeuda.setText("$ " + cf.format(adeuda));
                    tvAdeuda.setTextColor(Color.parseColor("#B45309"));
                }

                progressBar.setProgress(pct);
                tvPorcentaje.setText(pct + "% abonado ($ " + cf.format(abonado) + " de $ " + cf.format(total) + ")");

                String estadoNorm = contrato.getEstadoNormalizado();
                tvEstadoBadge.setText(estadoNorm.toUpperCase(Locale.ROOT));
                if (Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(estadoNorm)) {
                    tvEstadoBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1F8DF")));
                    tvEstadoBadge.setTextColor(Color.parseColor("#006C49"));
                } else if (Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(estadoNorm)) {
                    tvEstadoBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFDAD6")));
                    tvEstadoBadge.setTextColor(Color.parseColor("#BA1A1A"));
                } else {
                    tvEstadoBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                    tvEstadoBadge.setTextColor(Color.parseColor("#B45309"));
                }

                layoutCuotas.removeAllViews();
                LayoutInflater li = LayoutInflater.from(requireContext());

                // 1. Fila de Seña o Anticipo inicial
                double seniaVal = contrato.seniaMonto != null ? contrato.seniaMonto : 0.0;
                if (seniaVal > 0) {
                    View rowSeña = li.inflate(R.layout.item_cuota_cobranza, layoutCuotas, false);
                    TextView tvNum = rowSeña.findViewById(R.id.tv_cuota_numero);
                    TextView tvConcepto = rowSeña.findViewById(R.id.tv_cuota_concepto);
                    TextView tvSub = rowSeña.findViewById(R.id.tv_cuota_subtitulo);
                    TextView tvMonto = rowSeña.findViewById(R.id.tv_cuota_monto);
                    View btnCobrar = rowSeña.findViewById(R.id.btn_cuota_cobrar);
                    TextView tvPagada = rowSeña.findViewById(R.id.tv_cuota_estado_pagado);

                    tvNum.setText("S");
                    tvConcepto.setText("Seña / Anticipo Inicial");
                    tvSub.setText("Abonado al momento de la firma");
                    tvMonto.setText("$ " + cf.format(seniaVal));
                    btnCobrar.setVisibility(View.GONE);
                    tvPagada.setVisibility(View.VISIBLE);
                    layoutCuotas.addView(rowSeña);
                }

                // 2. Si es Crédito (Cuotas)
                if ("Crédito".equalsIgnoreCase(contrato.metodoPago)) {
                    int cantCuotas = contrato.cuotas != null && contrato.cuotas > 0 ? contrato.cuotas : 1;
                    double saldoFinanciado = Math.max(0.0, total - seniaVal);
                    double valorCuotaEstimado = saldoFinanciado / cantCuotas;

                    for (int i = 1; i <= cantCuotas; i++) {
                        final int nroCuota = i;
                        View row = li.inflate(R.layout.item_cuota_cobranza, layoutCuotas, false);
                        TextView tvNum = row.findViewById(R.id.tv_cuota_numero);
                        TextView tvConcepto = row.findViewById(R.id.tv_cuota_concepto);
                        TextView tvSub = row.findViewById(R.id.tv_cuota_subtitulo);
                        TextView tvMonto = row.findViewById(R.id.tv_cuota_monto);
                        MaterialButton btnCobrar = row.findViewById(R.id.btn_cuota_cobrar);
                        TextView tvPagada = row.findViewById(R.id.tv_cuota_estado_pagado);

                        tvNum.setText(String.valueOf(nroCuota));
                        tvConcepto.setText("Cuota " + nroCuota + " de " + cantCuotas);

                        Pago pago = contrato.obtenerPagoDeCuota(nroCuota);
                        if (pago != null) {
                            tvMonto.setText("$ " + cf.format(pago.monto));
                            tvSub.setText("Cobrado el " + pago.fecha + " • " + pago.metodoPago);
                            btnCobrar.setVisibility(View.GONE);
                            tvPagada.setVisibility(View.VISIBLE);
                        } else {
                            tvMonto.setText("$ " + cf.format(valorCuotaEstimado));
                            tvSub.setText("Pendiente de cobro");
                            tvPagada.setVisibility(View.GONE);
                            btnCobrar.setVisibility(View.VISIBLE);
                            btnCobrar.setOnClickListener(v -> {
                                mostrarDialogoRegistrarCobro(contrato, nroCuota, valorCuotaEstimado, "Cuota " + nroCuota + " de " + cantCuotas, this);
                            });
                        }
                        layoutCuotas.addView(row);
                    }
                } else {
                    // Contado / Transferencia / Efectivo
                    double saldoRestante = Math.max(0.0, total - seniaVal);
                    Pago pagoSaldo = null;
                    for (Pago p : contrato.getListaPagos()) {
                        if (p.nroCuota == 0) {
                            pagoSaldo = p;
                            break;
                        }
                    }

                    View row = li.inflate(R.layout.item_cuota_cobranza, layoutCuotas, false);
                    TextView tvNum = row.findViewById(R.id.tv_cuota_numero);
                    TextView tvConcepto = row.findViewById(R.id.tv_cuota_concepto);
                    TextView tvSub = row.findViewById(R.id.tv_cuota_subtitulo);
                    TextView tvMonto = row.findViewById(R.id.tv_cuota_monto);
                    MaterialButton btnCobrar = row.findViewById(R.id.btn_cuota_cobrar);
                    TextView tvPagada = row.findViewById(R.id.tv_cuota_estado_pagado);

                    tvNum.setText("1");
                    tvConcepto.setText("Saldo restante de Contado");

                    if (adeuda <= 0.001 || pagoSaldo != null) {
                        double montoPagado = pagoSaldo != null ? pagoSaldo.monto : saldoRestante;
                        String fechaP = (pagoSaldo != null && pagoSaldo.fecha != null) ? pagoSaldo.fecha : (contrato.fechaInicio != null ? contrato.fechaInicio : "-");
                        String metodoP = (pagoSaldo != null && pagoSaldo.metodoPago != null) ? pagoSaldo.metodoPago : (contrato.metodoPago != null ? contrato.metodoPago : "Efectivo");
                        tvMonto.setText("$ " + cf.format(montoPagado));
                        tvSub.setText("Liquidado el " + fechaP + " • " + metodoP);
                        btnCobrar.setVisibility(View.GONE);
                        tvPagada.setVisibility(View.VISIBLE);
                    } else {
                        tvMonto.setText("$ " + cf.format(saldoRestante));
                        tvSub.setText("Pendiente de liquidación");
                        tvPagada.setVisibility(View.GONE);
                        btnCobrar.setVisibility(View.VISIBLE);
                        btnCobrar.setText("Liquidar Saldo");
                        btnCobrar.setOnClickListener(v -> {
                            mostrarDialogoRegistrarCobro(contrato, 0, saldoRestante, "Liquidación saldo restante", this);
                        });
                    }
                    layoutCuotas.addView(row);
                }
            }
        };

        refrescarVista.run();
        dialog.show();
    }

    private void mostrarDialogoRegistrarCobro(Contrato contrato, int nroCuota, double montoSugerido, String conceptoDefecto, Runnable onPagoRegistrado) {
        if (getContext() == null || contrato == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_registrar_pago_cuota, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitulo = dialogView.findViewById(R.id.dialog_pago_tv_titulo);
        TextView tvSubtitulo = dialogView.findViewById(R.id.dialog_pago_tv_subtitulo);
        TextInputEditText etMonto = dialogView.findViewById(R.id.et_pago_monto);
        AutoCompleteTextView actvMedio = dialogView.findViewById(R.id.actv_pago_medio);
        TextInputEditText etFecha = dialogView.findViewById(R.id.et_pago_fecha);
        TextInputEditText etConcepto = dialogView.findViewById(R.id.et_pago_concepto);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.dialog_pago_btn_cancelar);
        MaterialButton btnConfirmar = dialogView.findViewById(R.id.dialog_pago_btn_confirmar);

        tvTitulo.setText("Registrar Cobro");
        tvSubtitulo.setText(conceptoDefecto + " • " + contrato.getNombreCompletoCliente());

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(2);
        etMonto.setText(nf.format(montoSugerido));

        String[] medios = {"Efectivo", "Transferencia", "Tarjeta de Débito", "Tarjeta de Crédito", "Cheque"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, medios);
        actvMedio.setAdapter(adapter);
        actvMedio.setText(medios[0], false);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etFecha.setText(sdf.format(new Date()));

        etConcepto.setText(conceptoDefecto);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnConfirmar.setOnClickListener(v -> {
            String montoStr = etMonto.getText() != null ? etMonto.getText().toString().trim() : "";
            if (montoStr.isEmpty()) {
                etMonto.setError("Ingrese un monto");
                return;
            }
            double monto;
            try {
                monto = Double.parseDouble(montoStr.replace(",", "."));
            } catch (Exception e) {
                etMonto.setError("Monto inválido");
                return;
            }

            String medio = actvMedio.getText().toString().trim();
            if (medio.isEmpty()) medio = "Efectivo";

            String fecha = etFecha.getText() != null && !etFecha.getText().toString().trim().isEmpty()
                    ? etFecha.getText().toString().trim()
                    : sdf.format(new Date());

            String concepto = etConcepto.getText() != null && !etConcepto.getText().toString().trim().isEmpty()
                    ? etConcepto.getText().toString().trim()
                    : conceptoDefecto;

            Pago nuevoPago = new Pago(
                    UUID.randomUUID().toString(),
                    contrato.id,
                    nroCuota,
                    monto,
                    fecha,
                    medio,
                    concepto
            );

            if (getActivity() != null) {
                android.app.Application app = requireActivity().getApplication();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(app);
                    db.pagoDao().insertar(nuevoPago);

                    List<Pago> todosPagos = db.pagoDao().obtenerPorContratoSync(contrato.id);
                    contrato.setListaPagos(todosPagos);
                    contrato.actualizarEstadoSegunSaldo();
                    contrato.estadoSincronizacion = "PENDIENTE_SUBIR";
                    db.contratoDao().actualizar(contrato);

                    if (Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(contrato.estado)) {
                        List<com.example.inmobiliariaventa.domain.entity.Propiedad> todas = db.propiedadDao().obtenerTodasSync();
                        if (todas != null) {
                            for (com.example.inmobiliariaventa.domain.entity.Propiedad p : todas) {
                                boolean matchId = contrato.propiedadId != null && contrato.propiedadId.equals(p.id);
                                boolean matchTitulo = contrato.propiedadTitulo != null && p.titulo != null && contrato.propiedadTitulo.trim().equalsIgnoreCase(p.titulo.trim());
                                if (matchId || matchTitulo) {
                                    p.estado = "Vendida";
                                    new com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl(app).update(p);
                                    break;
                                }
                            }
                        }
                    }

                    // Sincronizar pago en Supabase (tabla pagos)
                    SupabaseClient.guardarPago(nuevoPago, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            nuevoPago.estadoSincronizacion = "SINCRONIZADO";
                            AppDatabase.databaseWriteExecutor.execute(() -> db.pagoDao().insertar(nuevoPago));
                        }
                        @Override
                        public void onError(Exception e) {}
                    });

                    // Sincronizar contrato actualizado en Supabase
                    SupabaseClient.actualizarEstadoContrato(contrato.id, contrato.estado, null);
                });
            }

            dialog.dismiss();
            if (onPagoRegistrado != null) {
                onPagoRegistrado.run();
            }
            renderContractsList();

            String msg = Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(contrato.estado)
                    ? "¡Pago registrado! El contrato ha sido COMPLETADO al 100%."
                    : "Pago registrado con éxito.";
            CartelUtils.mostrar(requireContext(), msg);
        });

        dialog.show();
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
            tvTitulo.setText("¿Rechazar el contrato?");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("¿Estás seguro de que deseas rechazar el contrato de " + clienteName
                    + " para \"" + propiedadName + "\"?\n\nEl contrato pasará a estado Rechazado y la propiedad volverá a publicarse en el catálogo.");
        }
        if (btnCancelar != null) {
            btnCancelar.setText("No, volver");
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnConfirmar != null) {
            btnConfirmar.setText("Sí, rechazar");
            btnConfirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BA1A1A")));
            btnConfirmar.setTextColor(Color.WHITE);
            btnConfirmar.setOnClickListener(v -> {
                dialog.dismiss();
                darDeBajaContrato(contrato);
                ActividadRepository.getInstance(requireContext())
                        .registrarModificacionContrato(clienteName, propiedadName, monto, "Rechazado (Baja)");
                CartelUtils.mostrar(requireContext(), "Contrato de " + clienteName + " rechazado");
            });
        }

        dialog.show();
    }

    private void darDeBajaContrato(Contrato contrato) {
        contrato.estado = Contrato.ESTADO_RECHAZADO;
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
                    SupabaseClient.actualizarEstadoContrato(contrato.id, Contrato.ESTADO_RECHAZADO, null);
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
                                        && (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(c.estado) || Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(c.estado))) {
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
                case Contrato.ESTADO_COMPLETADO:
                case "Activo":
                case "Finalizado":
                    color = android.graphics.Color.parseColor("#006C49");
                    break;
                case Contrato.ESTADO_PENDIENTE:
                    color = android.graphics.Color.parseColor("#B45309");
                    break;
                case Contrato.ESTADO_RECHAZADO:
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
        CartelUtils.mostrar(requireContext(), "Página " + currentPage + " de " + maxPages);
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
            CartelUtils.mostrar(requireContext(), "Página " + currentPage + " de " + maxPages);
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
