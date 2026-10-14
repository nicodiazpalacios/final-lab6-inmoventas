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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminConfiguracionFragment extends Fragment {

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
                
                // Simulate saving delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (getContext() != null) {
                        btnGuardar.setText("Guardado");
                        Toast.makeText(getContext(), "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            btnGuardar.setText("Guardar Cambios");
                            btnGuardar.setEnabled(true);
                        }, 2000);
                    }
                }, 1200);
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
