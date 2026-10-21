package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AdminConfiguracionFragment extends Fragment {

    private EmpresaRepository empresaRepository;
    private TextInputEditText etNombreEmpresa;
    private TextInputEditText etTelefonoEmpresa;
    private TextInputEditText etEmailEmpresa;
    private TextInputEditText etDireccionEmpresa;

    public AdminConfiguracionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_configuracion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        empresaRepository = new EmpresaRepository(requireContext());
        etNombreEmpresa = view.findViewById(R.id.et_nombre_empresa);
        etTelefonoEmpresa = view.findViewById(R.id.et_telefono_empresa);
        etEmailEmpresa = view.findViewById(R.id.et_email_empresa);
        etDireccionEmpresa = view.findViewById(R.id.et_direccion_empresa);

        // Pre-llenar valores inmediatos
        if (etNombreEmpresa != null) etNombreEmpresa.setText(empresaRepository.getNombreEmpresaInmediato());
        if (etTelefonoEmpresa != null) etTelefonoEmpresa.setText(empresaRepository.getTelefonoWhatsappInmediato());
        if (etEmailEmpresa != null) etEmailEmpresa.setText(empresaRepository.getEmailInmediato());
        if (etDireccionEmpresa != null) etDireccionEmpresa.setText(empresaRepository.getDireccionInmediata());

        // Cargar configuración guardada desde la nube o base de datos local
        empresaRepository.cargarConfiguracionAsync(config -> {
            if (config != null && isAdded()) {
                if (etNombreEmpresa != null && config.nombreEmpresa != null) etNombreEmpresa.setText(config.nombreEmpresa);
                if (etTelefonoEmpresa != null && config.telefonoWhatsapp != null) etTelefonoEmpresa.setText(config.telefonoWhatsapp);
                if (etEmailEmpresa != null && config.email != null) etEmailEmpresa.setText(config.email);
                if (etDireccionEmpresa != null && config.direccion != null) etDireccionEmpresa.setText(config.direccion);
            }
        });

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).popBackStack();
            });
        }

        MaterialButton btnExportarPdf = view.findViewById(R.id.btn_exportar_pdf);
        if (btnExportarPdf != null) {
            btnExportarPdf.setOnClickListener(v -> mostrarDateRangePicker(btnExportarPdf, "PDF"));
        }

        MaterialButton btnExportarExcel = view.findViewById(R.id.btn_exportar_excel);
        if (btnExportarExcel != null) {
            btnExportarExcel.setOnClickListener(v -> mostrarDateRangePicker(btnExportarExcel, "Excel"));
        }

        MaterialButton btnGestionarUsuarios = view.findViewById(R.id.btn_gestionar_usuarios);
        if (btnGestionarUsuarios != null) {
            btnGestionarUsuarios.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view)
                        .navigate(R.id.action_nav_admin_configuracion_to_nav_admin_gestion_usuarios);
            });
        }

        MaterialButton btnGuardar = view.findViewById(R.id.btn_guardar_cambios);
        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> {
                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando...");

                String nombre = etNombreEmpresa != null && etNombreEmpresa.getText() != null ?
                        etNombreEmpresa.getText().toString().trim() : "Inmobiliaria Venta";
                String tel = etTelefonoEmpresa != null && etTelefonoEmpresa.getText() != null ?
                        etTelefonoEmpresa.getText().toString().trim() : "+54 9 11 5842-1102";
                String email = etEmailEmpresa != null && etEmailEmpresa.getText() != null ?
                        etEmailEmpresa.getText().toString().trim() : "contacto@inmoventas.com";
                String dir = etDireccionEmpresa != null && etDireccionEmpresa.getText() != null ?
                        etDireccionEmpresa.getText().toString().trim() : "Av. del Libertador 2450, Buenos Aires";

                // Guardar en la base de datos de Room y Supabase
                empresaRepository.guardarConfiguracion(nombre, tel, email, dir);
                
                // Confirmación visual
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (getContext() != null) {
                        btnGuardar.setText("Guardado");
                        Toast.makeText(getContext(), "Datos de la empresa guardados correctamente", Toast.LENGTH_SHORT).show();
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            btnGuardar.setText("Guardar Cambios");
                            btnGuardar.setEnabled(true);
                        }, 2000);
                    }
                }, 600);
            });
        }
    }

    private void mostrarDateRangePicker(MaterialButton button, String format) {
        long current = MaterialDatePicker.todayInUtcMilliseconds();
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(current);
        
        calendar.add(java.util.Calendar.YEAR, -5);
        long startDateLimit = calendar.getTimeInMillis();
        
        calendar.add(java.util.Calendar.YEAR, 10);
        long endDateLimit = calendar.getTimeInMillis();

        com.google.android.material.datepicker.CalendarConstraints constraints = 
            new com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setStart(startDateLimit)
                .setEnd(endDateLimit)
                .build();

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Seleccionar rango para exportar")
                        .setPositiveButtonText("Descargar")
                        .setCalendarConstraints(constraints)
                        .setTheme(R.style.CustomDatePickerTheme)
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;
            
            if (startDate != null && endDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String startDateStr = sdf.format(new Date(startDate));
                String endDateStr = sdf.format(new Date(endDate));
                
                button.setText("Exportar " + format + " (" + startDateStr + " - " + endDateStr + ")");
                
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Descargando " + format + "...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dateRangePicker.show(getChildFragmentManager(), "DATE_RANGE_PICKER");
    }
}
