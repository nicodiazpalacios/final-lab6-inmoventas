package com.example.inmobiliariaventa.ui.usuarios;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.example.inmobiliariaventa.R;

public class PerfilUsuarioFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil_usuario, container, false);

        CardView btnLogout = root.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        Toast.makeText(requireContext(), "Cerrando sesión...", Toast.LENGTH_SHORT).show();
                        // Simulando el cierre de sesión
                        new Handler().postDelayed(() -> {
                            if (getActivity() != null) {
                                Toast.makeText(requireContext(), "Sesión cerrada con éxito", Toast.LENGTH_SHORT).show();
                            }
                        }, 1000);
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return root;
    }
}
