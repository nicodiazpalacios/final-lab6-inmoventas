package com.example.inmobiliariaventa.ui.admin;

import com.example.inmobiliariaventa.utils.CartelUtils;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.ActividadRepository;
import com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Pago;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class AdminAgregarContratoFragment extends Fragment {

    // Header
    private TextView tvHeaderTitle, tvHeaderSubtitle;

    // Inputs: Cliente
    private TextInputLayout tilFirstName, tilLastName, tilDni, tilPhone, tilEmail;
    private TextInputEditText etFirstName, etLastName, etDni, etPhone, etEmail;

    // Inputs: Contrato
    private TextInputLayout tilProperty, tilAmount, tilPaymentMethod, tilInstallments, tilDownPayment, tilStartDate, tilEndDate, tilStatus, tilDescription;
    private AutoCompleteTextView etProperty, actvPaymentMethod, actvStatus;
    private TextInputEditText etAmount, etInstallments, etDownPayment, etStartDate, etEndDate, etDescription;
    private View layoutFinanciacion;

    // Preview
    private TextView tvPreviewAmount, tvPreviewProp, tvPreviewMethod, tvPreviewInstallments, tvReferencePropertyTitle;
    private View layoutPreviewInstallments, dividerPreviewInstallments;
    private ImageView ivReferencePropertyBg;

    // Buttons
    private ImageButton btnBack;
    private MaterialButton btnCancelar, btnGuardarContrato;

    // Estado interno
    private final Map<String, Propiedad> mapaPropiedades = new HashMap<>();
    private final List<Contrato> listaContratosExistentes = new ArrayList<>();
    private Long selectedStartDateMillis = null;
    private String attachedPdfUriStr = null;
    private String editingContratoId = null;
    private Contrato contratoEnEdicion = null;
    private boolean isPopulatingEditData = false;

    public AdminAgregarContratoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_agregar_contrato, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            editingContratoId = getArguments().getString("contrato_id");
        }

        initViews(view);
        setupDropdowns();
        setupDatePickers();
        setupLivePreview();
        setupActions(view);
        cargarPropiedadesYContratoExistente();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        tvHeaderTitle = view.findViewById(R.id.tv_header_title);
        tvHeaderSubtitle = view.findViewById(R.id.tv_header_subtitle);

        // Client info
        tilFirstName = view.findViewById(R.id.til_first_name);
        etFirstName = view.findViewById(R.id.et_first_name);
        tilLastName = view.findViewById(R.id.til_last_name);
        etLastName = view.findViewById(R.id.et_last_name);
        tilDni = view.findViewById(R.id.til_dni);
        etDni = view.findViewById(R.id.et_dni);
        tilPhone = view.findViewById(R.id.til_phone);
        etPhone = view.findViewById(R.id.et_phone);
        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);

        // Contract details
        tilProperty = view.findViewById(R.id.til_property);
        etProperty = view.findViewById(R.id.et_property);
        tilAmount = view.findViewById(R.id.til_amount);
        etAmount = view.findViewById(R.id.et_amount);
        tilPaymentMethod = view.findViewById(R.id.til_payment_method);
        actvPaymentMethod = view.findViewById(R.id.actv_payment_method);
        layoutFinanciacion = view.findViewById(R.id.layout_financiacion);
        tilInstallments = view.findViewById(R.id.til_installments);
        etInstallments = view.findViewById(R.id.et_installments);
        tilDownPayment = view.findViewById(R.id.til_down_payment);
        etDownPayment = view.findViewById(R.id.et_down_payment);
        tilStartDate = view.findViewById(R.id.til_start_date);
        etStartDate = view.findViewById(R.id.et_start_date);
        tilEndDate = view.findViewById(R.id.til_end_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        tilStatus = view.findViewById(R.id.til_status);
        actvStatus = view.findViewById(R.id.actv_status);
        tilDescription = view.findViewById(R.id.til_description);
        etDescription = view.findViewById(R.id.et_description);

        // Live preview
        tvPreviewAmount = view.findViewById(R.id.tv_preview_amount);
        tvPreviewProp = view.findViewById(R.id.tv_preview_prop);
        tvPreviewMethod = view.findViewById(R.id.tv_preview_method);
        tvPreviewInstallments = view.findViewById(R.id.tv_preview_installments);
        layoutPreviewInstallments = view.findViewById(R.id.layout_preview_installments);
        dividerPreviewInstallments = view.findViewById(R.id.divider_preview_installments);
        tvReferencePropertyTitle = view.findViewById(R.id.tv_reference_property_title);
        ivReferencePropertyBg = view.findViewById(R.id.iv_reference_property_bg);

        // Action buttons
        btnCancelar = view.findViewById(R.id.btn_cancelar);
        btnGuardarContrato = view.findViewById(R.id.btn_guardar_contrato);

        if (editingContratoId != null && !editingContratoId.isEmpty()) {
            if (tvHeaderTitle != null) tvHeaderTitle.setText("Modificar Contrato");
            if (tvHeaderSubtitle != null) tvHeaderSubtitle.setText("Modifique los datos del acuerdo inmobiliario seleccionado.");
            if (btnGuardarContrato != null) btnGuardarContrato.setText("Guardar Cambios");
        }
    }

    private void setupDropdowns() {
        if (getContext() == null) return;

        // Payment Method Dropdown
        String[] paymentMethods = {"Efectivo", "Transferencia", "Crédito"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                paymentMethods
        );
        actvPaymentMethod.setAdapter(methodAdapter);
        actvPaymentMethod.setText(paymentMethods[0], false);
        actualizarVisibilidadCuotas(paymentMethods[0]);

        // Status Dropdown
        String[] statusOptions = {Contrato.ESTADO_PENDIENTE, Contrato.ESTADO_COMPLETADO, Contrato.ESTADO_RECHAZADO};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
        );
        actvStatus.setAdapter(statusAdapter);
        actvStatus.setText(statusOptions[0], false);
    }

    private void actualizarVisibilidadCuotas(String metodoPago) {
        boolean tieneCuotas = "Crédito".equalsIgnoreCase(metodoPago);
        int dp6 = Math.round(6 * getResources().getDisplayMetrics().density);

        if (layoutFinanciacion != null) {
            layoutFinanciacion.setVisibility(View.VISIBLE);
        }
        if (tilInstallments != null) {
            tilInstallments.setVisibility(tieneCuotas ? View.VISIBLE : View.GONE);
        }
        if (tilDownPayment != null && tilDownPayment.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tilDownPayment.getLayoutParams();
            lp.setMarginStart(tieneCuotas ? dp6 : 0);
            tilDownPayment.setLayoutParams(lp);
        }
        if (tilEndDate != null) {
            tilEndDate.setVisibility(tieneCuotas ? View.VISIBLE : View.GONE);
        }
        if (tilStartDate != null) {
            tilStartDate.setHint(tieneCuotas ? "Fecha de Inicio" : "Fecha del Contrato");
            if (tilStartDate.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lpStart = (ViewGroup.MarginLayoutParams) tilStartDate.getLayoutParams();
                lpStart.setMarginEnd(tieneCuotas ? dp6 : 0);
                tilStartDate.setLayoutParams(lpStart);
            }
        }
        if (layoutPreviewInstallments != null) {
            layoutPreviewInstallments.setVisibility(tieneCuotas ? View.VISIBLE : View.GONE);
        }
        if (dividerPreviewInstallments != null) {
            dividerPreviewInstallments.setVisibility(tieneCuotas ? View.VISIBLE : View.GONE);
        }
        if (!tieneCuotas) {
            if (etInstallments != null) etInstallments.setText("");
            if (tilInstallments != null) tilInstallments.setError(null);
            if (tilEndDate != null) tilEndDate.setError(null);
            if (selectedStartDateMillis != null && etEndDate != null && etStartDate != null) {
                etEndDate.setText(etStartDate.getText());
            }
        } else if (!isPopulatingEditData) {
            recalcularFechaVencimiento();
        }
    }

    private void cargarPropiedadesYContratoExistente() {
        if (getContext() == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Propiedad> propiedades = db.propiedadDao().obtenerTodasSync();
            List<Contrato> contratos = db.contratoDao().obtenerTodosSync();

            listaContratosExistentes.clear();
            if (contratos != null) {
                listaContratosExistentes.addAll(contratos);
            }

            if (editingContratoId != null && !editingContratoId.isEmpty()) {
                contratoEnEdicion = db.contratoDao().obtenerPorIdSync(editingContratoId);
            }

            List<String> titulosDisponibles = new ArrayList<>();
            mapaPropiedades.clear();

            if (propiedades != null) {
                for (Propiedad p : propiedades) {
                    if (p.titulo != null && !p.titulo.trim().isEmpty()) {
                        String t = p.titulo.trim();
                        if (!mapaPropiedades.containsKey(t)) {
                            mapaPropiedades.put(t, p);
                            Contrato ocupadoPorOtro = buscarOtroContratoVigente(p, editingContratoId);
                            if (ocupadoPorOtro == null) {
                                titulosDisponibles.add(t);
                            }
                        }
                    }
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    ArrayAdapter<String> propAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            titulosDisponibles
                    );
                    etProperty.setAdapter(propAdapter);
                    etProperty.setOnClickListener(v -> etProperty.showDropDown());
                    etProperty.setOnItemClickListener((parent, view, position, id) -> {
                        String seleccion = (String) parent.getItemAtPosition(position);
                        tilProperty.setError(null);
                        aplicarPropiedadSeleccionada(seleccion, true);
                    });

                    if (contratoEnEdicion != null) {
                        poblarDatosContrato(contratoEnEdicion);
                    }
                });
            }
        });
    }

    private Propiedad buscarPropiedadEnCatalogo(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        String buscado = texto.trim();
        if (mapaPropiedades.containsKey(buscado)) {
            return mapaPropiedades.get(buscado);
        }
        for (Map.Entry<String, Propiedad> entry : mapaPropiedades.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(buscado)) {
                return entry.getValue();
            }
            if (entry.getValue().id != null && entry.getValue().id.equals(buscado)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Contrato buscarOtroContratoVigente(Propiedad prop, String excludeContratoId) {
        if (prop == null) return null;
        for (Contrato c : listaContratosExistentes) {
            if (excludeContratoId != null && excludeContratoId.equals(c.id)) {
                continue;
            }
            if (c.estado != null && (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(c.estado) || Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(c.estado) || "Activo".equalsIgnoreCase(c.estado))) {
                boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(prop.id);
                boolean matchTitulo = c.propiedadTitulo != null && prop.titulo != null
                        && c.propiedadTitulo.trim().equalsIgnoreCase(prop.titulo.trim());
                if (matchId || matchTitulo) {
                    return c;
                }
            }
        }
        return null;
    }

    private void poblarDatosContrato(Contrato c) {
        isPopulatingEditData = true;

        if (etFirstName != null && c.clienteNombre != null) etFirstName.setText(c.clienteNombre);
        if (etLastName != null && c.clienteApellido != null) etLastName.setText(c.clienteApellido);
        if (etDni != null && c.clienteDni != null) etDni.setText(c.clienteDni);
        if (etPhone != null && c.clienteTelefono != null) etPhone.setText(c.clienteTelefono);
        if (etEmail != null && c.clienteEmail != null) etEmail.setText(c.clienteEmail);

        String propTitulo = c.propiedadTitulo != null && !c.propiedadTitulo.isEmpty() ? c.propiedadTitulo : c.propiedadId;
        if (etProperty != null && propTitulo != null) {
            etProperty.setText(propTitulo, false);
            tvPreviewProp.setText(propTitulo);
            tvReferencePropertyTitle.setText(propTitulo);
            aplicarPropiedadSeleccionada(propTitulo, false);
        }

        if (etAmount != null && c.montoTotal != null) {
            if (c.montoTotal == c.montoTotal.longValue()) {
                etAmount.setText(String.valueOf(c.montoTotal.longValue()));
            } else {
                etAmount.setText(String.valueOf(c.montoTotal));
            }
        }

        String metodo = c.metodoPago != null && !c.metodoPago.isEmpty() ? c.metodoPago : "Efectivo";
        if (actvPaymentMethod != null) {
            actvPaymentMethod.setText(metodo, false);
        }
        if (tvPreviewMethod != null) {
            tvPreviewMethod.setText(metodo);
        }
        actualizarVisibilidadCuotas(metodo);

        if ("Crédito".equalsIgnoreCase(metodo) && etInstallments != null && c.cuotas != null) {
            etInstallments.setText(String.valueOf(c.cuotas));
        }

        if (etDownPayment != null && c.seniaMonto != null && c.seniaMonto > 0) {
            if (c.seniaMonto == c.seniaMonto.longValue()) {
                etDownPayment.setText(String.valueOf(c.seniaMonto.longValue()));
            } else {
                etDownPayment.setText(String.valueOf(c.seniaMonto));
            }
        }

        if (etStartDate != null && c.fechaInicio != null && !c.fechaInicio.isEmpty()) {
            etStartDate.setText(c.fechaInicio);
        }
        if (etEndDate != null && c.fechaVencimiento != null && !c.fechaVencimiento.isEmpty()) {
            etEndDate.setText(c.fechaVencimiento);
        }

        if (actvStatus != null) {
            String norm = c.getEstadoNormalizado();
            actvStatus.setText(norm, false);
            if (Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(norm)) {
                String[] statusOptionsCompletado = {Contrato.ESTADO_COMPLETADO};
                android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, statusOptionsCompletado);
                actvStatus.setAdapter(statusAdapter);
                actvStatus.setEnabled(false);
            }
        }

        if (etDescription != null && c.descripcion != null) {
            etDescription.setText(c.descripcion);
        }

        if (c.rutaLocalPdf != null && !c.rutaLocalPdf.isEmpty()) {
            attachedPdfUriStr = c.rutaLocalPdf;
        }

        isPopulatingEditData = false;
    }

    private boolean tieneContratoVigenteEnLista(Propiedad prop, List<Contrato> lista) {
        if (prop == null || lista == null) return false;
        for (Contrato c : lista) {
            if (c.estado != null && (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(c.estado) || Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(c.estado) || "Activo".equalsIgnoreCase(c.estado))) {
                boolean matchId = c.propiedadId != null && !c.propiedadId.isEmpty() && c.propiedadId.equals(prop.id);
                boolean matchTitulo = c.propiedadTitulo != null && prop.titulo != null
                        && c.propiedadTitulo.trim().equalsIgnoreCase(prop.titulo.trim());
                if (matchId || matchTitulo) {
                    return true;
                }
            }
        }
        return false;
    }

    private void aplicarPropiedadSeleccionada(String titulo, boolean actualizarMonto) {
        Propiedad prop = buscarPropiedadEnCatalogo(titulo);
        if (prop != null) {
            if (actualizarMonto && prop.precio > 0 && etAmount != null) {
                if (prop.precio == (long) prop.precio) {
                    etAmount.setText(String.valueOf((long) prop.precio));
                } else {
                    etAmount.setText(String.valueOf(prop.precio));
                }
            }
            if (ivReferencePropertyBg != null && prop.imagenes != null && !prop.imagenes.isEmpty()) {
                String imgUrl = prop.imagenes.get(0);
                if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                    ivReferencePropertyBg.setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(imgUrl)
                            .centerCrop()
                            .into(ivReferencePropertyBg);
                    return;
                }
            }
        }
        if (ivReferencePropertyBg != null) {
            ivReferencePropertyBg.setVisibility(View.GONE);
        }
    }

    private void setupDatePickers() {
        // Fecha por defecto: Hoy
        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();
        selectedStartDateMillis = todayUtc;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String hoyStr = sdf.format(new Date(todayUtc));
        if (etStartDate != null) etStartDate.setText(hoyStr);
        if (etEndDate != null) etEndDate.setText(hoyStr);

        View.OnClickListener startDateClickListener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Seleccionar fecha")
                    .setSelection(selectedStartDateMillis != null ? selectedStartDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                    .setTheme(R.style.CustomDatePickerTheme)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    selectedStartDateMillis = selection;
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDate = format.format(new Date(selection));
                    etStartDate.setText(formattedDate);
                    tilStartDate.setError(null);
                    recalcularFechaVencimiento();
                }
            });

            datePicker.show(getParentFragmentManager(), "START_DATE_PICKER");
        };

        etStartDate.setOnClickListener(startDateClickListener);
        tilStartDate.setEndIconOnClickListener(startDateClickListener);

        View.OnClickListener endDateClickListener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Seleccionar fecha de vencimiento")
                    .setTheme(R.style.CustomDatePickerTheme)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    etEndDate.setText(format.format(new Date(selection)));
                    tilEndDate.setError(null);
                }
            });

            datePicker.show(getParentFragmentManager(), "END_DATE_PICKER");
        };

        etEndDate.setOnClickListener(endDateClickListener);
        tilEndDate.setEndIconOnClickListener(endDateClickListener);
    }

    private void recalcularFechaVencimiento() {
        if (selectedStartDateMillis == null || etEndDate == null) return;
        String metodo = actvPaymentMethod != null && actvPaymentMethod.getText() != null
                ? actvPaymentMethod.getText().toString() : "Efectivo";

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(selectedStartDateMillis);

        if ("Crédito".equalsIgnoreCase(metodo) && etInstallments != null && etInstallments.getText() != null) {
            String cuotasStr = etInstallments.getText().toString().trim();
            if (!cuotasStr.isEmpty()) {
                try {
                    int meses = Integer.parseInt(cuotasStr);
                    if (meses > 1) {
                        cal.add(Calendar.MONTH, meses);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        etEndDate.setText(format.format(cal.getTime()));
    }

    private void setupLivePreview() {
        // Amount preview
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    try {
                        double val = Double.parseDouble(s.toString());
                        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                        DecimalFormat df = new DecimalFormat("#,##0.##", symbols);
                        tvPreviewAmount.setText(df.format(val));
                    } catch (NumberFormatException e) {
                        tvPreviewAmount.setText(s.toString());
                    }
                } else {
                    tvPreviewAmount.setText("0.00");
                }
                actualizarPreviewCuotas();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Property preview
        etProperty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.toString().trim().length() > 0) {
                    String prop = s.toString().trim();
                    tvPreviewProp.setText(prop);
                    tvReferencePropertyTitle.setText(prop);
                    if (mapaPropiedades.containsKey(prop)) {
                        aplicarPropiedadSeleccionada(prop, !isPopulatingEditData);
                    }
                } else {
                    tvPreviewProp.setText("No seleccionada");
                    tvReferencePropertyTitle.setText("Skyline Luxury Residences");
                    if (ivReferencePropertyBg != null) {
                        ivReferencePropertyBg.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Payment method preview + ocultar/mostrar cuotas
        actvPaymentMethod.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            tvPreviewMethod.setText(selected);
            actualizarVisibilidadCuotas(selected);
            actualizarPreviewCuotas();
        });

        // Installments preview
        etInstallments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarPreviewCuotas();
                if (!isPopulatingEditData) {
                    recalcularFechaVencimiento();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (etDownPayment != null) {
            etDownPayment.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tilDownPayment != null) tilDownPayment.setError(null);
                    actualizarPreviewCuotas();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void actualizarPreviewCuotas() {
        if (tvPreviewInstallments == null) return;
        String cuotasStr = etInstallments != null && etInstallments.getText() != null
                ? etInstallments.getText().toString().trim() : "";
        if (cuotasStr.isEmpty()) {
            tvPreviewInstallments.setText("-");
            return;
        }
        try {
            int cuotas = Integer.parseInt(cuotasStr);
            if (cuotas < 1) {
                tvPreviewInstallments.setText("-");
                return;
            }
            double monto = 0.0;
            if (etAmount != null && etAmount.getText() != null && !etAmount.getText().toString().trim().isEmpty()) {
                try { monto = Double.parseDouble(etAmount.getText().toString().trim()); } catch (NumberFormatException ignored) {}
            }
            double senia = 0.0;
            if (etDownPayment != null && etDownPayment.getText() != null && !etDownPayment.getText().toString().trim().isEmpty()) {
                try { senia = Double.parseDouble(etDownPayment.getText().toString().trim()); } catch (NumberFormatException ignored) {}
            }
            double saldoFinanciado = Math.max(0.0, monto - senia);
            if (saldoFinanciado > 0) {
                double valorCuota = saldoFinanciado / cuotas;
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                DecimalFormat df = new DecimalFormat("#,##0", symbols);
                tvPreviewInstallments.setText(cuotas + (cuotas == 1 ? " cuota de $ " : " cuotas de $ ") + df.format(valorCuota));
            } else {
                tvPreviewInstallments.setText(cuotas + (cuotas == 1 ? " Cuota" : " Cuotas"));
            }
        } catch (NumberFormatException e) {
            tvPreviewInstallments.setText(cuotasStr + " Cuotas");
        }
    }

    private void setupActions(View root) {
        View.OnClickListener goBackListener = v -> Navigation.findNavController(v).popBackStack();

        if (btnBack != null) {
            btnBack.setOnClickListener(goBackListener);
        }
        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(goBackListener);
        }
        if (btnGuardarContrato != null) {
            btnGuardarContrato.setOnClickListener(v -> handleGuardarContrato(root));
        }
    }

    private void handleGuardarContrato(View root) {
        // Clear previous errors
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilDni.setError(null);
        tilPhone.setError(null);
        tilEmail.setError(null);
        tilProperty.setError(null);
        tilAmount.setError(null);
        tilInstallments.setError(null);
        tilStartDate.setError(null);
        tilEndDate.setError(null);

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String dni = etDni.getText() != null ? etDni.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String property = etProperty.getText() != null ? etProperty.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String paymentMethod = actvPaymentMethod.getText() != null ? actvPaymentMethod.getText().toString().trim() : "Efectivo";
        String installmentsStr = etInstallments.getText() != null ? etInstallments.getText().toString().trim() : "";
        String downPaymentStr = etDownPayment.getText() != null ? etDownPayment.getText().toString().trim() : "";
        String startDate = etStartDate.getText() != null ? etStartDate.getText().toString().trim() : "";
        String endDate = etEndDate.getText() != null ? etEndDate.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean tieneCuotas = "Crédito".equalsIgnoreCase(paymentMethod);
        boolean hasError = false;

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError("Ingrese el nombre");
            hasError = true;
        }
        if (TextUtils.isEmpty(lastName)) {
            tilLastName.setError("Ingrese el apellido");
            hasError = true;
        }
        if (TextUtils.isEmpty(dni)) {
            tilDni.setError("Ingrese el DNI");
            hasError = true;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Ingrese el teléfono");
            hasError = true;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un correo válido");
            hasError = true;
        }
        if (TextUtils.isEmpty(property)) {
            tilProperty.setError("Seleccione o ingrese la propiedad");
            hasError = true;
        }
        double montoTotal = 0;
        if (TextUtils.isEmpty(amountStr)) {
            tilAmount.setError("Ingrese el monto total");
            hasError = true;
        } else {
            try {
                montoTotal = Double.parseDouble(amountStr);
                if (montoTotal <= 0) {
                    tilAmount.setError("El monto debe ser mayor a 0");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                tilAmount.setError("Monto inválido");
                hasError = true;
            }
        }

        int cuotas = 1;
        if (tieneCuotas) {
            if (TextUtils.isEmpty(installmentsStr)) {
                tilInstallments.setError("Ingrese las cuotas");
                hasError = true;
            } else {
                try {
                    cuotas = Integer.parseInt(installmentsStr);
                    if (cuotas < 1) {
                        tilInstallments.setError("Mínimo 1 cuota");
                        hasError = true;
                    }
                } catch (NumberFormatException e) {
                    tilInstallments.setError("Número inválido");
                    hasError = true;
                }
            }
        }

        if (TextUtils.isEmpty(startDate)) {
            tilStartDate.setError("Seleccione fecha");
            hasError = true;
        }
        if (tieneCuotas && TextUtils.isEmpty(endDate)) {
            tilEndDate.setError("Seleccione vencimiento");
            hasError = true;
        }
        if (!tieneCuotas) {
            endDate = startDate;
        }

        String situacion = actvStatus != null && actvStatus.getText() != null && !actvStatus.getText().toString().isEmpty()
                ? actvStatus.getText().toString().trim()
                : Contrato.ESTADO_PENDIENTE;

        boolean esEdicion = (contratoEnEdicion != null);
        if (esEdicion && Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(contratoEnEdicion.getEstadoNormalizado())
                && Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(situacion)) {
            if (tilStatus != null) tilStatus.setError("Un contrato completado no puede pasar a estado Rechazado");
            hasError = true;
        }
        String contratoId = esEdicion ? contratoEnEdicion.id : UUID.randomUUID().toString();
        long timestamp = esEdicion ? contratoEnEdicion.timestamp : System.currentTimeMillis();

        // Validar que la propiedad exista en el catálogo y no tenga ya otro contrato Activo/Pendiente
        Propiedad propSeleccionada = buscarPropiedadEnCatalogo(property);
        if (!TextUtils.isEmpty(property)) {
            if (propSeleccionada == null) {
                tilProperty.setError("Seleccione una propiedad existente del catálogo");
                hasError = true;
            } else if (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(situacion) || Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(situacion) || "Activo".equalsIgnoreCase(situacion)) {
                Contrato otroVigente = buscarOtroContratoVigente(propSeleccionada, contratoId);
                if (otroVigente != null) {
                    tilProperty.setError("Esta propiedad ya posee un contrato " + otroVigente.estado
                            + " a nombre de " + otroVigente.getNombreCompletoCliente());
                    hasError = true;
                }
            }
        }

        double senia = 0.0;
        if (!TextUtils.isEmpty(downPaymentStr)) {
            try {
                senia = Double.parseDouble(downPaymentStr);
                if (senia < 0) {
                    if (tilDownPayment != null) tilDownPayment.setError("La seña no puede ser negativa");
                    hasError = true;
                } else if (montoTotal > 0 && senia >= montoTotal) {
                    if (tilDownPayment != null) tilDownPayment.setError("La seña debe ser menor al monto total");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                if (tilDownPayment != null) tilDownPayment.setError("Monto de seña inválido");
                hasError = true;
            }
        }

        if (hasError) {
            CartelUtils.mostrar(requireContext(), "Por favor, complete correctamente los campos obligatorios");
            return;
        }

        String tituloOficialPropiedad = propSeleccionada != null && propSeleccionada.titulo != null
                ? propSeleccionada.titulo.trim()
                : property;

        final String oldPropId = esEdicion ? contratoEnEdicion.propiedadId : null;
        final String oldPropTitulo = esEdicion ? contratoEnEdicion.propiedadTitulo : null;

        Contrato contratoGuardado = new Contrato(
                contratoId,
                firstName,
                lastName,
                dni,
                phone,
                email,
                tituloOficialPropiedad,
                montoTotal,
                senia,
                paymentMethod,
                tieneCuotas ? cuotas : 1,
                startDate,
                endDate,
                situacion,
                description,
                attachedPdfUriStr,
                timestamp
        );

        if (propSeleccionada != null && propSeleccionada.id != null) {
            contratoGuardado.propiedadId = propSeleccionada.id;
        }

        android.app.Application appContext = requireActivity().getApplication();

        // Guardar en Room Database y sincronizar con Supabase y con el estado de la propiedad
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            db.contratoDao().insertar(contratoGuardado);

            // Si hay seña inicial en un contrato nuevo, registrar el Pago en la tabla 'pagos'
            if (!esEdicion && contratoGuardado.seniaMonto != null && contratoGuardado.seniaMonto > 0) {
                Pago pagoSenia = new Pago(
                        UUID.randomUUID().toString(),
                        contratoGuardado.id,
                        0,
                        contratoGuardado.seniaMonto,
                        startDate,
                        paymentMethod,
                        "Seña / Anticipo Inicial"
                );
                db.pagoDao().insertar(pagoSenia);
                SupabaseClient.guardarPago(pagoSenia, null);
            }

            SupabaseClient.guardarContrato(contratoGuardado, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        contratoGuardado.estadoSincronizacion = "SINCRONIZADO";
                        db.contratoDao().insertar(contratoGuardado);
                    });
                }

                @Override
                public void onError(Exception e) {
                    // Queda como PENDIENTE_SUBIR en Room para sincronizar automáticamente después
                }
            });

            List<Propiedad> todasProps = db.propiedadDao().obtenerTodasSync();
            List<Contrato> todosContratos = db.contratoDao().obtenerTodosSync();
            if (todasProps == null) return;

            PropiedadRepositoryImpl repo = new PropiedadRepositoryImpl(appContext);

            // 1. Si era una edición y se cambió de propiedad, liberar la propiedad anterior si no tiene otro contrato vigente
            if (esEdicion && (oldPropId != null || oldPropTitulo != null)) {
                boolean cambioPropiedad = (propSeleccionada == null)
                        || (oldPropId != null && !oldPropId.equals(propSeleccionada.id)
                            && (oldPropTitulo == null || !oldPropTitulo.trim().equalsIgnoreCase(tituloOficialPropiedad)));
                if (cambioPropiedad) {
                    for (Propiedad p : todasProps) {
                        boolean eraLaAnterior = (oldPropId != null && oldPropId.equals(p.id))
                                || (oldPropTitulo != null && p.titulo != null && oldPropTitulo.trim().equalsIgnoreCase(p.titulo.trim()));
                        if (eraLaAnterior && p.isPausada() && !tieneContratoVigenteEnLista(p, todosContratos)) {
                            p.estado = "Publicado";
                            repo.update(p);
                        }
                    }
                }
            }

            // 2. Actualizar la propiedad afectada por el contrato actual
            Propiedad propAfectada = null;
            for (Propiedad p : todasProps) {
                boolean matchId = contratoGuardado.propiedadId != null && contratoGuardado.propiedadId.equals(p.id);
                boolean matchTitulo = p.titulo != null && p.titulo.trim().equalsIgnoreCase(tituloOficialPropiedad);
                if (matchId || matchTitulo) {
                    propAfectada = p;
                    break;
                }
            }

            if (propAfectada != null) {
                if (Contrato.ESTADO_PENDIENTE.equalsIgnoreCase(situacion)) {
                    if (!propAfectada.isPausada() && !"Vendida".equalsIgnoreCase(propAfectada.estado)) {
                        propAfectada.estado = "Pausado";
                        repo.update(propAfectada);
                    }
                } else if (Contrato.ESTADO_COMPLETADO.equalsIgnoreCase(situacion)) {
                    if (!propAfectada.isPausada()) {
                        propAfectada.estado = "Pausado";
                        repo.update(propAfectada);
                    }
                } else if (Contrato.ESTADO_RECHAZADO.equalsIgnoreCase(situacion) || "Cancelado".equalsIgnoreCase(situacion)) {
                    if (propAfectada.isPausada() && !tieneContratoVigenteEnLista(propAfectada, todosContratos)) {
                        propAfectada.estado = "Publicado";
                        repo.update(propAfectada);
                    }
                }
            }
        });

        // Registrar en actividad reciente / notificaciones admin
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        String montoFormateado = "$ " + df.format(montoTotal);
        if (esEdicion) {
            ActividadRepository.getInstance(requireContext())
                    .registrarModificacionContrato(firstName + " " + lastName, property, montoFormateado, situacion);
        } else {
            ActividadRepository.getInstance(requireContext())
                    .registrarNuevoContrato(firstName + " " + lastName, property, montoFormateado, situacion);
        }

        btnGuardarContrato.setEnabled(false);
        String msgExito = esEdicion
                ? "Contrato actualizado exitosamente para " + firstName + " " + lastName
                : "Contrato guardado exitosamente para " + firstName + " " + lastName;
        CartelUtils.mostrarLargo(requireContext(), msgExito);

        Navigation.findNavController(root).popBackStack();
    }
}
