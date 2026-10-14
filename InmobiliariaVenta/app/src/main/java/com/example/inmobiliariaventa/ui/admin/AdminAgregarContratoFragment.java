package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminAgregarContratoFragment extends Fragment {

    // Inputs
    private TextInputLayout tilFirstName, tilLastName, tilDni, tilEmail;
    private TextInputEditText etFirstName, etLastName, etDni, etEmail;

    private TextInputLayout tilProperty, tilAmount, tilPaymentMethod, tilInstallments, tilStartDate, tilStatus, tilDescription;
    private TextInputEditText etProperty, etAmount, etInstallments, etStartDate, etDescription;
    private AutoCompleteTextView actvPaymentMethod, actvStatus;

    // Preview
    private TextView tvPreviewAmount, tvPreviewProp, tvPreviewMethod, tvPreviewInstallments, tvReferencePropertyTitle;

    // Buttons
    private ImageButton btnBack;
    private MaterialButton btnCancelar, btnGuardarContrato;

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

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupLivePreview();
        setupActions(view);
    }

    private void initViews(View view) {
        // Header
        btnBack = view.findViewById(R.id.btn_back);

        // Client info
        tilFirstName = view.findViewById(R.id.til_first_name);
        etFirstName = view.findViewById(R.id.et_first_name);
        tilLastName = view.findViewById(R.id.til_last_name);
        etLastName = view.findViewById(R.id.et_last_name);
        tilDni = view.findViewById(R.id.til_dni);
        etDni = view.findViewById(R.id.et_dni);
        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);

        // Contract details
        tilProperty = view.findViewById(R.id.til_property);
        etProperty = view.findViewById(R.id.et_property);
        tilAmount = view.findViewById(R.id.til_amount);
        etAmount = view.findViewById(R.id.et_amount);
        tilPaymentMethod = view.findViewById(R.id.til_payment_method);
        actvPaymentMethod = view.findViewById(R.id.actv_payment_method);
        tilInstallments = view.findViewById(R.id.til_installments);
        etInstallments = view.findViewById(R.id.et_installments);
        tilStartDate = view.findViewById(R.id.til_start_date);
        etStartDate = view.findViewById(R.id.et_start_date);
        tilStatus = view.findViewById(R.id.til_status);
        actvStatus = view.findViewById(R.id.actv_status);
        tilDescription = view.findViewById(R.id.til_description);
        etDescription = view.findViewById(R.id.et_description);

        // Live preview
        tvPreviewAmount = view.findViewById(R.id.tv_preview_amount);
        tvPreviewProp = view.findViewById(R.id.tv_preview_prop);
        tvPreviewMethod = view.findViewById(R.id.tv_preview_method);
        tvPreviewInstallments = view.findViewById(R.id.tv_preview_installments);
        tvReferencePropertyTitle = view.findViewById(R.id.tv_reference_property_title);

        // Action buttons
        btnCancelar = view.findViewById(R.id.btn_cancelar);
        btnGuardarContrato = view.findViewById(R.id.btn_guardar_contrato);
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

        // Status Dropdown
        String[] statusOptions = {"Activo", "Pendiente", "Rechazado"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
        );
        actvStatus.setAdapter(statusAdapter);
        actvStatus.setText(statusOptions[0], false);
    }

    private void setupDatePicker() {
        View.OnClickListener dateClickListener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Seleccionar fecha de inicio")
                    .setTheme(R.style.CustomDatePickerTheme)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(new Date(selection));
                    etStartDate.setText(formattedDate);
                }
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        };

        etStartDate.setOnClickListener(dateClickListener);
        tilStartDate.setEndIconOnClickListener(dateClickListener);
    }

    private void setupLivePreview() {
        // Amount preview
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    tvPreviewAmount.setText(s.toString());
                } else {
                    tvPreviewAmount.setText("0.00");
                }
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
                } else {
                    tvPreviewProp.setText("No seleccionada");
                    tvReferencePropertyTitle.setText("Skyline Luxury Residences");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Payment method preview
        actvPaymentMethod.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            tvPreviewMethod.setText(selected);
        });

        // Installments preview
        etInstallments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    tvPreviewInstallments.setText(s.toString() + " pagos");
                } else {
                    tvPreviewInstallments.setText("-");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupActions(View root) {
        // Back / Cancel
        View.OnClickListener goBackListener = v -> Navigation.findNavController(v).popBackStack();

        if (btnBack != null) {
            btnBack.setOnClickListener(goBackListener);
        }
        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(goBackListener);
        }

        // Save contract
        if (btnGuardarContrato != null) {
            btnGuardarContrato.setOnClickListener(v -> handleGuardarContrato(root));
        }
    }

    private void handleGuardarContrato(View root) {
        // Clear previous errors
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilProperty.setError(null);
        tilAmount.setError(null);

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String property = etProperty.getText() != null ? etProperty.getText().toString().trim() : "";
        String amount = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

        boolean hasError = false;

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError("Ingrese el nombre");
            hasError = true;
        }
        if (TextUtils.isEmpty(lastName)) {
            tilLastName.setError("Ingrese el apellido");
            hasError = true;
        }
        if (TextUtils.isEmpty(property)) {
            tilProperty.setError("Ingrese la propiedad");
            hasError = true;
        }
        if (TextUtils.isEmpty(amount)) {
            tilAmount.setError("Ingrese el monto total");
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(requireContext(), "Por favor, complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate save progress / success
        btnGuardarContrato.setEnabled(false);
        Toast.makeText(requireContext(), "Contrato guardado exitosamente para " + firstName + " " + lastName, Toast.LENGTH_LONG).show();

        // Return to contracts management screen
        Navigation.findNavController(root).popBackStack();
    }
}
