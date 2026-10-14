package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.inmobiliariaventa.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class AdminHomeFragment extends Fragment {

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.admin_home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fabAddProperty = view.findViewById(R.id.fab_add_property);
        if (fabAddProperty != null) {
            fabAddProperty.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }
        
        View cardMensajes = view.findViewById(R.id.card_mensajes);
        if (cardMensajes != null) {
            cardMensajes.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_mensajes);
            });
        }

        View cardPropiedades = view.findViewById(R.id.card_propiedades);
        if (cardPropiedades != null) {
            cardPropiedades.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_propiedades);
            });
        }

        View cardVentas = view.findViewById(R.id.card_ventas);
        if (cardVentas != null) {
            cardVentas.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_ventas);
            });
        }

        View cardAjustes = view.findViewById(R.id.card_ajustes);
        if (cardAjustes != null) {
            cardAjustes.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_configuracion);
            });
        }

        View btnVerTodoActividad = view.findViewById(R.id.btn_ver_todo_actividad);
        if (btnVerTodoActividad != null) {
            btnVerTodoActividad.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }

        View notificationsButton = view.findViewById(R.id.notificationsButton);
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }
    }
}
