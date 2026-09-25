package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.util.Pair;

import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminVentasFragment extends Fragment {

    public AdminVentasFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_ventas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnCustomRange = view.findViewById(R.id.btn_custom_range);
        if (btnCustomRange != null) {
            btnCustomRange.setOnClickListener(v -> mostrarDateRangePicker(btnCustomRange));
        }

        View tvVerTodas = view.findViewById(R.id.tv_ver_todas_propiedades);
        if (tvVerTodas != null) {
            tvVerTodas.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_ventas_to_nav_admin_propiedades_vendidas);
            });
        }
    }

    private void mostrarDateRangePicker(MaterialButton button) {
        // Reducir el rango de fechas aliviando el RecyclerView (soluciona el lag al hacer scroll)
        long current = MaterialDatePicker.todayInUtcMilliseconds();
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(current);
        
        calendar.add(java.util.Calendar.YEAR, -5); // Hasta 5 años atrás
        long startDateLimit = calendar.getTimeInMillis();
        
        calendar.add(java.util.Calendar.YEAR, 10); // Hasta 5 años en el futuro
        long endDateLimit = calendar.getTimeInMillis();

        com.google.android.material.datepicker.CalendarConstraints constraints = 
            new com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setStart(startDateLimit)
                .setEnd(endDateLimit)
                .build();

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Seleccionar rango de fechas")
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
                
                // Actualizar el texto del botón con el rango seleccionado
                button.setText(startDateStr + " - " + endDateStr);
                
                // TODO: Llamar al ViewModel o a la base de datos para cargar datos en este rango
            }
        });

        dateRangePicker.show(getChildFragmentManager(), "DATE_RANGE_PICKER");
    }
}
