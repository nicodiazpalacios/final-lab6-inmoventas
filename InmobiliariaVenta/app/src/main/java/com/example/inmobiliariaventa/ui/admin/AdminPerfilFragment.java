package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;

public class AdminPerfilFragment extends Fragment {

    public AdminPerfilFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración de Cuenta
        view.findViewById(R.id.btn_personal_info).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Información Personal", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_security).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Seguridad", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notificaciones", Toast.LENGTH_SHORT).show();
        });

        // Logout
        View btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            // Assuming nav_login is the destination for login
            Navigation.findNavController(view).navigate(R.id.nav_login);
        });
    }
}
