package com.example.inmobiliariaventa.ui.usuarios;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;

public class PerfilUsuarioFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_usuario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String nombre = prefs.getString("user_nombre", "Santiago Mayorga");

        TextView tvNombre = view.findViewById(R.id.tv_nombre_usuario_perfil);
        if (tvNombre != null) {
            tvNombre.setText(nombre);
        }

        CardView btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cerrar Sesión")
                        .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            prefs.edit().clear().apply();
                            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).navigate(R.id.nav_inicio);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }
}
