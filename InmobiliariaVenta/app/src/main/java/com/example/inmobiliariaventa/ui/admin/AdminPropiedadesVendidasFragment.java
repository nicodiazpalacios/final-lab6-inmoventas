package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ContratoPdfGenerator;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminPropiedadesVendidasFragment extends Fragment {

    private LinearLayout layoutPropiedadesVendidasContainer;
    private View layoutEmptyVendidas;
    private TextView tvSubtituloVendidasCount;
    private TextInputEditText etBuscarPropiedadVendida;
    private ChipGroup chipGroupMetodoPago;

    private final List<Contrato> allContratos = new ArrayList<>();
    private final Map<String, Propiedad> mapPropiedadesById = new HashMap<>();
    private final Map<String, Propiedad> mapPropiedadesByTitulo = new HashMap<>();

    private String currentSearchQuery = "";
    private int currentCheckedChipId = R.id.chip_filtro_todas;

    public AdminPropiedadesVendidasFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_propiedades_vendidas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        layoutPropiedadesVendidasContainer = view.findViewById(R.id.layout_propiedades_vendidas_container);
        layoutEmptyVendidas = view.findViewById(R.id.layout_empty_vendidas);
        tvSubtituloVendidasCount = view.findViewById(R.id.tv_subtitulo_vendidas_count);
        etBuscarPropiedadVendida = view.findViewById(R.id.et_buscar_propiedad_vendida);
        chipGroupMetodoPago = view.findViewById(R.id.chip_group_metodo_pago);

        if (etBuscarPropiedadVendida != null) {
            etBuscarPropiedadVendida.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                    renderizarLista();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (chipGroupMetodoPago != null) {
            chipGroupMetodoPago.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    currentCheckedChipId = checkedIds.get(0);
                } else {
                    currentCheckedChipId = R.id.chip_filtro_todas;
                }
                renderizarLista();
            });
        }

        cargarDatos();
    }

    private void cargarDatos() {
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
                getActivity().runOnUiThread(this::renderizarLista);
            }
        });

        db.contratoDao().obtenerTodosLiveData().observe(getViewLifecycleOwner(), contratos -> {
            allContratos.clear();
            if (contratos != null) {
                allContratos.addAll(contratos);
            }
            renderizarLista();
        });
    }

    private void renderizarLista() {
        if (!isAdded() || getContext() == null || layoutPropiedadesVendidasContainer == null) return;

        List<Contrato> filtradas = new ArrayList<>();
        for (Contrato c : allContratos) {
            String est = c.getEstadoNormalizado();
            if (Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(est)) {
                continue;
            }

            String metodo = c.metodoPago != null ? c.metodoPago.trim() : "Efectivo";

            // Filtro por Método de Pago: Todas | Contado (Efectivo) | Transferencia | Créditos
            if (currentCheckedChipId == R.id.chip_filtro_efectivo) {
                if (!"Efectivo".equalsIgnoreCase(metodo)) {
                    continue;
                }
            } else if (currentCheckedChipId == R.id.chip_filtro_transferencia) {
                if (!"Transferencia".equalsIgnoreCase(metodo)) {
                    continue;
                }
            } else if (currentCheckedChipId == R.id.chip_filtro_credito) {
                if (!"Crédito".equalsIgnoreCase(metodo) && !"Credito".equalsIgnoreCase(metodo)) {
                    continue;
                }
            }

            // Buscador en tiempo real (Propiedad, Dirección/Ciudad, Cliente o DNI)
            if (!currentSearchQuery.isEmpty()) {
                Propiedad prop = buscarPropiedad(c);
                String tituloProp = (c.propiedadTitulo != null && !c.propiedadTitulo.isEmpty()
                        ? c.propiedadTitulo
                        : (prop != null && prop.titulo != null ? prop.titulo : c.propiedadId));
                String tLower = tituloProp != null ? tituloProp.toLowerCase(Locale.ROOT) : "";
                String domLower = prop != null && prop.domicilio != null ? prop.domicilio.toLowerCase(Locale.ROOT) : "";
                String ciuLower = prop != null && prop.ciudad != null ? prop.ciudad.toLowerCase(Locale.ROOT) : "";
                String cliLower = c.getNombreCompletoCliente().toLowerCase(Locale.ROOT);
                String dniLower = c.clienteDni != null ? c.clienteDni.toLowerCase(Locale.ROOT) : "";

                if (!tLower.contains(currentSearchQuery)
                        && !domLower.contains(currentSearchQuery)
                        && !ciuLower.contains(currentSearchQuery)
                        && !cliLower.contains(currentSearchQuery)
                        && !dniLower.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filtradas.add(c);
        }

        Collections.sort(filtradas, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        if (tvSubtituloVendidasCount != null) {
            int n = filtradas.size();
            tvSubtituloVendidasCount.setText(n == 1 ? "1 propiedad encontrada" : n + " propiedades encontradas");
        }

        layoutPropiedadesVendidasContainer.removeAllViews();

        if (filtradas.isEmpty()) {
            if (layoutEmptyVendidas != null) layoutEmptyVendidas.setVisibility(View.VISIBLE);
            return;
        }
        if (layoutEmptyVendidas != null) layoutEmptyVendidas.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMaximumFractionDigits(0);

        for (Contrato c : filtradas) {
            View card = inflater.inflate(R.layout.item_propiedad_vendida_card, layoutPropiedadesVendidasContainer, false);

            ImageView ivImagen = card.findViewById(R.id.iv_vendida_imagen);
            TextView tvTitulo = card.findViewById(R.id.tv_vendida_titulo);
            MaterialCardView cardBadge = card.findViewById(R.id.card_vendida_badge);
            TextView tvBadge = card.findViewById(R.id.tv_vendida_badge);
            TextView tvSubtitulo = card.findViewById(R.id.tv_vendida_subtitulo);
            TextView tvPrecio = card.findViewById(R.id.tv_vendida_precio);
            TextView tvFecha = card.findViewById(R.id.tv_vendida_fecha);
            TextView tvModalidad = card.findViewById(R.id.tv_vendida_modalidad);

            Propiedad prop = buscarPropiedad(c);
            String tituloProp = c.propiedadTitulo != null && !c.propiedadTitulo.isEmpty()
                    ? c.propiedadTitulo
                    : (prop != null && prop.titulo != null ? prop.titulo : c.propiedadId);

            String direccion = "";
            if (prop != null) {
                if (prop.domicilio != null && !prop.domicilio.isEmpty()) {
                    direccion = prop.domicilio;
                }
                if (prop.ciudad != null && !prop.ciudad.isEmpty()) {
                    direccion = direccion.isEmpty() ? prop.ciudad : direccion + ", " + prop.ciudad;
                }
            }
            String comprador = "Comprador: " + c.getNombreCompletoCliente();
            String subtitulo = direccion.isEmpty() ? comprador : direccion + "  •  " + comprador;

            double montoTotal = c.montoTotal != null ? c.montoTotal : 0.0;
            double senia = c.seniaMonto != null ? c.seniaMonto : 0.0;
            double saldo = Math.max(0.0, montoTotal - senia);
            int cuotas = c.cuotas != null && c.cuotas > 0 ? c.cuotas : 1;
            boolean esPendiente = Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(c.getEstadoNormalizado());

            String modalidadStr;
            if ("Crédito".equalsIgnoreCase(c.metodoPago)) {
                double valorCuota = saldo / cuotas;
                modalidadStr = "Crédito • " + cuotas + (cuotas == 1 ? " cuota ($ " : " cuotas ($ ") + currencyFormat.format(valorCuota) + ")";
            } else if (esPendiente && senia > 0 && saldo > 0) {
                modalidadStr = "Reserva (" + c.metodoPago + ") • Resta $ " + currencyFormat.format(saldo);
            } else if ("Transferencia".equalsIgnoreCase(c.metodoPago)) {
                modalidadStr = "Transferencia";
            } else {
                modalidadStr = "Contado (Efectivo)";
            }

            tvTitulo.setText(tituloProp != null ? tituloProp : "Propiedad");
            tvSubtitulo.setText(subtitulo);
            tvPrecio.setText("$ " + currencyFormat.format(montoTotal));
            tvFecha.setText(c.fechaInicio != null && !c.fechaInicio.isEmpty() ? c.fechaInicio : "-");
            tvModalidad.setText(modalidadStr);

            if (esPendiente) {
                cardBadge.setCardBackgroundColor(Color.parseColor("#FFDEA8"));
                tvBadge.setText("Reservado");
                tvBadge.setTextColor(Color.parseColor("#7A5300"));
            } else {
                cardBadge.setCardBackgroundColor(Color.parseColor("#6CF8BB"));
                tvBadge.setText("Vendido");
                tvBadge.setTextColor(Color.parseColor("#00714D"));
            }

            if (prop != null && prop.imagenes != null && !prop.imagenes.isEmpty() && prop.imagenes.get(0) != null) {
                ivImagen.setPadding(0, 0, 0, 0);
                ivImagen.setImageTintList(null);
                Glide.with(this)
                        .load(prop.imagenes.get(0))
                        .centerCrop()
                        .placeholder(R.drawable.ic_propiedades)
                        .into(ivImagen);
            }

            card.setOnClickListener(v -> ContratoPdfGenerator.mostrarDialogoExportarPdf(requireContext(), c));
            layoutPropiedadesVendidasContainer.addView(card);
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
}
