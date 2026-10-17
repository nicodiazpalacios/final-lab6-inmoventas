package com.example.inmobiliariaventa.ui.usuarios;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.android.material.button.MaterialButton;

import java.util.UUID;

public class RegistroFragment extends Fragment {

    private boolean isPasswordVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        EditText etFullName = view.findViewById(R.id.etFullName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        ImageView btnTogglePassword = view.findViewById(R.id.btnTogglePassword);
        MaterialButton btnRegister = view.findViewById(R.id.btnRegister);
        TextView txtLoginLink = view.findViewById(R.id.txtLoginLink);

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Navigate back to login
        txtLoginLink.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Navigate back via top arrow
        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Registro real con Supabase y Room
        btnRegister.setOnClickListener(v -> {
            String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(fullName)) {
                if (etFullName != null) etFullName.setError("Ingrese su nombre completo");
                return;
            }

            if (TextUtils.isEmpty(email)) {
                if (etEmail != null) etEmail.setError("Ingrese su correo");
                return;
            }

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                if (etPassword != null) etPassword.setError("La contraseña debe tener al menos 6 caracteres");
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Creando cuenta...");

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.id = UUID.randomUUID().toString();
            nuevoUsuario.nombre = fullName;
            nuevoUsuario.email = email.toLowerCase();
            nuevoUsuario.rol = "CLIENTE";
            nuevoUsuario.estadoSincronizacion = "SINCRONIZADO";

            // Guardar en Supabase
            SupabaseClient.registrarUsuario(nuevoUsuario, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (!isAdded()) return;

                    // Guardar en Room local
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getInstance(requireContext()).usuarioDao().insertar(nuevoUsuario);
                    });

                    // Guardar sesión activa
                    SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putBoolean("is_user_logged_in", true)
                            .putString("user_id", nuevoUsuario.id)
                            .putString("user_email", nuevoUsuario.email)
                            .putString("user_nombre", nuevoUsuario.nombre)
                            .putString("user_rol", nuevoUsuario.rol)
                            .apply();

                    btnRegister.setText("Registrarse");
                    btnRegister.setEnabled(true);

                    Toast.makeText(getContext(), "¡Bienvenido, " + nuevoUsuario.nombre + "!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.nav_inicio);
                }

                @Override
                public void onError(Exception error) {
                    if (!isAdded()) return;
                    btnRegister.setText("Registrarse");
                    btnRegister.setEnabled(true);
                    Toast.makeText(getContext(), "No se pudo registrar en la nube. Verifique su conexión.", Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
    }
}
