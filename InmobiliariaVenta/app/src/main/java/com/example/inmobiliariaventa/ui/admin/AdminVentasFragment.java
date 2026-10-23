package com.example.inmobiliariaventa.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ContratoPdfGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class AdminVentasFragment extends Fragment {

    private MaterialButton btnPeriodoMes, btnPeriodo6Meses, btnPeriodoAnio, btnCustomRange;
    private MaterialButton btnExportarPdf, btnExportarExcel;
    private TextView tvVentasTotalesMonto, tvVentasTotalesBadge;
    private TextView tvPropiedadesVendidasCount, tvPropiedadesVendidasBadge;
    private TextView tvClientesNuevosCount, tvClientesNuevosBadge;
    private LinearLayout layoutVentasPropiedadesContainer;
    private TextView tvVentasEmptyPropiedades;

    private final List<Contrato> allContratos = new ArrayList<>();
    private final Map<String, Propiedad> mapPropiedadesById = new HashMap<>();
    private final Map<String, Propiedad> mapPropiedadesByTitulo = new HashMap<>();

    private String currentPeriodLabel = "Este Mes";
    private Long filterStartMillis = null;
    private Long filterEndMillis = null;

    // Datos filtrados actuales para exportar
    private final List<Contrato> currentFilteredVentas = new ArrayList<>();
    private double currentTotalMonto = 0.0;
    private int currentTotalVendidas = 0;
    private int currentTotalClientes = 0;

    public AdminVentasFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_ventas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        btnPeriodoMes = view.findViewById(R.id.btn_periodo_mes);
        btnPeriodo6Meses = view.findViewById(R.id.btn_periodo_6_meses);
        btnPeriodoAnio = view.findViewById(R.id.btn_periodo_anio);
        btnCustomRange = view.findViewById(R.id.btn_custom_range);

        tvVentasTotalesMonto = view.findViewById(R.id.tv_ventas_totales_monto);
        tvVentasTotalesBadge = view.findViewById(R.id.tv_ventas_totales_badge);
        tvPropiedadesVendidasCount = view.findViewById(R.id.tv_propiedades_vendidas_count);
        tvPropiedadesVendidasBadge = view.findViewById(R.id.tv_propiedades_vendidas_badge);
        tvClientesNuevosCount = view.findViewById(R.id.tv_clientes_nuevos_count);
        tvClientesNuevosBadge = view.findViewById(R.id.tv_clientes_nuevos_badge);

        btnExportarPdf = view.findViewById(R.id.btn_exportar_pdf);
        btnExportarExcel = view.findViewById(R.id.btn_exportar_excel);
        layoutVentasPropiedadesContainer = view.findViewById(R.id.layout_ventas_propiedades_container);
        tvVentasEmptyPropiedades = view.findViewById(R.id.tv_ventas_empty_propiedades);

        View tvVerTodas = view.findViewById(R.id.tv_ver_todas_propiedades);
        if (tvVerTodas != null) {
            tvVerTodas.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.action_nav_admin_ventas_to_nav_admin_propiedades_vendidas)
            );
        }

        setupPeriodButtons();
        setupReportButtons();
        aplicarPeriodoPreset(1, "Este Mes", btnPeriodoMes);
        cargarDatosReales();
    }

    private void setupPeriodButtons() {
        if (btnPeriodoMes != null) {
            btnPeriodoMes.setOnClickListener(v -> aplicarPeriodoPreset(1, "Este Mes", btnPeriodoMes));
        }
        if (btnPeriodo6Meses != null) {
            btnPeriodo6Meses.setOnClickListener(v -> aplicarPeriodoPreset(6, "Últimos 6 Meses", btnPeriodo6Meses));
        }
        if (btnPeriodoAnio != null) {
            btnPeriodoAnio.setOnClickListener(v -> aplicarPeriodoPreset(12, "Este Año", btnPeriodoAnio));
        }
        if (btnCustomRange != null) {
            btnCustomRange.setOnClickListener(v -> mostrarDateRangePicker());
        }
    }

    private void aplicarPeriodoPreset(int monthsBack, String label, MaterialButton selectedBtn) {
        currentPeriodLabel = label;
        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        filterEndMillis = calEnd.getTimeInMillis();

        Calendar calStart = Calendar.getInstance();
        if (monthsBack == 1) {
            calStart.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            calStart.add(Calendar.MONTH, -monthsBack);
        }
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        filterStartMillis = calStart.getTimeInMillis();

        if (btnCustomRange != null) {
            btnCustomRange.setText("Personalizar rango (Desde - Hasta)");
        }
        actualizarEstiloBotonesPeriodo(selectedBtn);
        recalcularYRenderizar();
    }

    private void actualizarEstiloBotonesPeriodo(MaterialButton activeBtn) {
        if (getContext() == null) return;
        MaterialButton[] buttons = {btnPeriodoMes, btnPeriodo6Meses, btnPeriodoAnio};
        int primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int onPrimary = ContextCompat.getColor(requireContext(), R.color.colorOnPrimary);
        int onSurfaceVar = ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant);
        int outlineVar = ContextCompat.getColor(requireContext(), R.color.colorOutlineVariant);
        int dp1 = Math.round(getResources().getDisplayMetrics().density);

        for (MaterialButton btn : buttons) {
            if (btn == null) continue;
            if (btn == activeBtn) {
                btn.setBackgroundTintList(ColorStateList.valueOf(primary));
                btn.setTextColor(onPrimary);
                btn.setStrokeWidth(0);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                btn.setTextColor(onSurfaceVar);
                btn.setStrokeColor(ColorStateList.valueOf(outlineVar));
                btn.setStrokeWidth(dp1);
            }
        }
    }

    private void setupReportButtons() {
        if (btnExportarPdf != null) {
            btnExportarPdf.setOnClickListener(v ->
                    ContratoPdfGenerator.mostrarReporteVentasPdf(
                            requireContext(),
                            currentPeriodLabel,
                            currentFilteredVentas,
                            currentTotalMonto,
                            currentTotalVendidas,
                            currentTotalClientes
                    )
            );
        }

        if (btnExportarExcel != null) {
            btnExportarExcel.setOnClickListener(v ->
                    ContratoPdfGenerator.exportarReporteVentasCsv(
                            requireContext(),
                            currentPeriodLabel,
                            currentFilteredVentas
                    )
            );
        }
    }

    private void cargarDatosReales() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Propiedad> props = db.propiedadDao().obtenerTodasSync();
            mapPropiedadesById.clear();
            mapPropiedadesByTitulo.clear();
            if (props != null) {
                for (Propiedad p : props) {
                    if (p.id != null) mapPropiedadesById.put(p.id, p);
                    if (p.titulo != null) mapPropiedadesByTitulo.put(p.titulo.trim().toLowerCase(Locale.ROOT), p);
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::recalcularYRenderizar);
            }
        });

        db.contratoDao().obtenerTodosLiveData().observe(getViewLifecycleOwner(), contratos -> {
            allContratos.clear();
            if (contratos != null) {
                allContratos.addAll(contratos);
            }
            recalcularYRenderizar();
        });

        // Sincronizar también desde Supabase con reconciliación
        SupabaseClient.obtenerContratos(new SupabaseClient.SupabaseCallback<List<Contrato>>() {
            @Override
            public void onSuccess(List<Contrato> remotos) {
                final List<Contrato> listaFinal = (remotos != null) ? remotos : new java.util.ArrayList<>();
                AppDatabase.databaseWriteExecutor.execute(() -> db.contratoDao().sincronizarConServidor(listaFinal));
            }

            @Override
            public void onError(Exception e) { }
        });
    }

    private void recalcularYRenderizar() {
        if (!isAdded() || getContext() == null) return;

        currentFilteredVentas.clear();
        double sumaVentasActivas = 0.0;
        int countActivas = 0;
        int countPendientes = 0;
        Set<String> clientesUnicos = new HashSet<>();

        for (Contrato c : allContratos) {
            String est = c.getEstadoNormalizado();
            if (Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(est)) {
                continue;
            }
            if (!estaEnRango(c)) {
                continue;
            }

            currentFilteredVentas.add(c);
            double monto = c.montoTotal != null ? c.montoTotal : 0.0;

            if ("Pendiente".equalsIgnoreCase(est)) {
                countPendientes++;
                // Si está en reserva/pendiente, suma la seña abonada o el acuerdo en curso
                sumaVentasActivas += monto;
            } else {
                countActivas++;
                sumaVentasActivas += monto;
            }

            String claveCliente = (c.clienteDni != null && !c.clienteDni.trim().isEmpty())
                    ? c.clienteDni.trim()
                    : c.getNombreCompletoCliente().toLowerCase(Locale.ROOT);
            clientesUnicos.add(claveCliente);
        }

        Collections.sort(currentFilteredVentas, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        currentTotalMonto = sumaVentasActivas;
        currentTotalVendidas = countActivas + countPendientes;
        currentTotalClientes = clientesUnicos.size();

        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMaximumFractionDigits(0);

        if (tvVentasTotalesMonto != null) {
            tvVentasTotalesMonto.setText("$ " + currencyFormat.format(currentTotalMonto));
        }
        if (tvVentasTotalesBadge != null) {
            tvVentasTotalesBadge.setText(countActivas + (countActivas == 1 ? " venta cerrada" : " ventas cerradas"));
        }

        if (tvPropiedadesVendidasCount != null) {
            tvPropiedadesVendidasCount.setText(String.valueOf(currentTotalVendidas));
        }
        if (tvPropiedadesVendidasBadge != null) {
            if (countPendientes > 0) {
                tvPropiedadesVendidasBadge.setText(countActivas + " vendidas • " + countPendientes + " en reserva");
            } else {
                tvPropiedadesVendidasBadge.setText(currentPeriodLabel);
            }
        }

        if (tvClientesNuevosCount != null) {
            tvClientesNuevosCount.setText(String.valueOf(currentTotalClientes));
        }
        if (tvClientesNuevosBadge != null) {
            tvClientesNuevosBadge.setText(currentTotalClientes == 1 ? "1 comprador" : currentTotalClientes + " compradores");
        }

        renderizarPropiedadesVendidasRecientes(currencyFormat);
    }

    private void renderizarPropiedadesVendidasRecientes(NumberFormat currencyFormat) {
        if (layoutVentasPropiedadesContainer == null || getContext() == null) return;
        layoutVentasPropiedadesContainer.removeAllViews();

        if (currentFilteredVentas.isEmpty()) {
            if (tvVentasEmptyPropiedades != null) {
                tvVentasEmptyPropiedades.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvVentasEmptyPropiedades != null) {
            tvVentasEmptyPropiedades.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int limit = Math.min(currentFilteredVentas.size(), 5);

        for (int i = 0; i < limit; i++) {
            Contrato c = currentFilteredVentas.get(i);
            View itemView = inflater.inflate(R.layout.item_venta_propiedad_resumen, layoutVentasPropiedadesContainer, false);

            ImageView ivThumb = itemView.findViewById(R.id.iv_venta_prop_thumb);
            TextView tvTitulo = itemView.findViewById(R.id.tv_venta_prop_titulo);
            TextView tvDetalle = itemView.findViewById(R.id.tv_venta_prop_detalle);
            TextView tvMonto = itemView.findViewById(R.id.tv_venta_prop_monto);

            Propiedad prop = buscarPropiedad(c);
            String tituloProp = c.propiedadTitulo != null && !c.propiedadTitulo.isEmpty()
                    ? c.propiedadTitulo
                    : (prop != null && prop.titulo != null ? prop.titulo : c.propiedadId);

            String metodo = c.metodoPago != null && !c.metodoPago.isEmpty() ? c.metodoPago : "Efectivo";
            if ("Crédito".equalsIgnoreCase(metodo) && c.cuotas != null && c.cuotas > 1) {
                metodo = c.cuotas + " cuotas";
            }
            String fecha = c.fechaInicio != null && !c.fechaInicio.isEmpty() ? c.fechaInicio : "";
            String detalle = c.getNombreCompletoCliente() + " • " + metodo + (!fecha.isEmpty() ? " • " + fecha : "");

            double montoVal = c.montoTotal != null ? c.montoTotal : 0.0;

            tvTitulo.setText(tituloProp != null ? tituloProp : "Propiedad");
            tvDetalle.setText(detalle);
            tvMonto.setText("$ " + currencyFormat.format(montoVal));

            if (prop != null && prop.imagenes != null && !prop.imagenes.isEmpty() && prop.imagenes.get(0) != null) {
                ivThumb.setPadding(0, 0, 0, 0);
                ivThumb.setImageTintList(null);
                Glide.with(this)
                        .load(prop.imagenes.get(0))
                        .centerCrop()
                        .placeholder(R.drawable.ic_propiedades)
                        .into(ivThumb);
            }

            itemView.setOnClickListener(v -> ContratoPdfGenerator.mostrarDialogoExportarPdf(requireContext(), c));
            layoutVentasPropiedadesContainer.addView(itemView);
        }
    }

    private Propiedad buscarPropiedad(Contrato c) {
        if (c == null) return null;
        if (c.propiedadId != null && mapPropiedadesById.containsKey(c.propiedadId)) {
            return mapPropiedadesById.get(c.propiedadId);
        }
        if (c.propiedadTitulo != null) {
            Propiedad p = mapPropiedadesByTitulo.get(c.propiedadTitulo.trim().toLowerCase(Locale.ROOT));
            if (p != null) return p;
        }
        if (c.propiedadId != null) {
            return mapPropiedadesByTitulo.get(c.propiedadId.trim().toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private boolean estaEnRango(Contrato c) {
        if (filterStartMillis == null || filterEndMillis == null) return true;
        long fechaContratoMillis = obtenerMillisDeContrato(c);
        return fechaContratoMillis >= filterStartMillis && fechaContratoMillis <= filterEndMillis;
    }

    private long obtenerMillisDeContrato(Contrato c) {
        if (c.fechaInicio != null && !c.fechaInicio.trim().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date d = sdf.parse(c.fechaInicio.trim());
                if (d != null) return d.getTime();
            } catch (Exception ignored) { }
        }
        return c.timestamp > 0 ? c.timestamp : System.currentTimeMillis();
    }

    private void mostrarDateRangePicker() {
        long current = MaterialDatePicker.todayInUtcMilliseconds();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(current);

        calendar.add(Calendar.YEAR, -5);
        long startDateLimit = calendar.getTimeInMillis();

        calendar.add(Calendar.YEAR, 10);
        long endDateLimit = calendar.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDateLimit)
                .setEnd(endDateLimit)
                .build();

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Seleccionar rango de fechas")
                .setCalendarConstraints(constraints)
                .setTheme(R.style.CustomDatePickerTheme)
                .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                filterStartMillis = selection.first;
                filterEndMillis = selection.second + (24L * 60L * 60L * 1000L - 1L);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String startDateStr = sdf.format(new Date(selection.first));
                String endDateStr = sdf.format(new Date(selection.second));

                currentPeriodLabel = startDateStr + " - " + endDateStr;
                if (btnCustomRange != null) {
                    btnCustomRange.setText(currentPeriodLabel);
                }
                actualizarEstiloBotonesPeriodo(null);
                recalcularYRenderizar();
            }
        });

        dateRangePicker.show(getChildFragmentManager(), "DATE_RANGE_PICKER");
    }
}
