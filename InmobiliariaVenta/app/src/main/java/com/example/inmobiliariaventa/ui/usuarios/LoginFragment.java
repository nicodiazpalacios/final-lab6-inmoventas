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

public class LoginFragment extends Fragment {

    private boolean isPasswordVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        ImageView btnTogglePassword = view.findViewById(R.id.btnTogglePassword);
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);

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

        // Navigate to registration
        TextView txtRegisterLink = view.findViewById(R.id.txtRegisterLink);
        txtRegisterLink.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_registro);
        });

        // Navigate back
        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Acción real de Login conectado a Supabase y Room
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Ingrese su correo electrónico");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Ingrese su contraseña");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Verificando cuenta...");

            SupabaseClient.obtenerUsuarioPorEmail(email.toLowerCase(), new SupabaseClient.SupabaseCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    if (!isAdded()) return;

                    if (usuario != null) {
                        // Guardar sesión en SharedPreferences
                        guardarSesion(usuario);

                        // Guardar en base local Room
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getInstance(requireContext()).usuarioDao().insertar(usuario);
                        });

                        btnLogin.setText("Iniciar sesión");
                        btnLogin.setEnabled(true);

                        if ("ADMIN".equalsIgnoreCase(usuario.rol)) {
                            Toast.makeText(getContext(), "Acceso concedido: Administrador", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(v).navigate(R.id.nav_admin_home);
                        } else {
                            Toast.makeText(getContext(), "Bienvenido " + usuario.nombre, Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(v).navigate(R.id.nav_inicio);
                        }
                    } else {
                        // Buscar en base local si no hay conexión o no existe en Supabase
                        verificarLocal(email, v, btnLogin);
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (!isAdded()) return;
                    verificarLocal(email, v, btnLogin);
                }
            });
        });

        return view;
    }

    private void verificarLocal(String email, View view, MaterialButton btnLogin) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Usuario local = AppDatabase.getInstance(requireContext()).usuarioDao().obtenerPorEmail(email);
            view.post(() -> {
                if (!isAdded()) return;
                btnLogin.setText("Iniciar sesión");
                btnLogin.setEnabled(true);

                if (local != null) {
                    guardarSesion(local);
                    if ("ADMIN".equalsIgnoreCase(local.rol)) {
                        Toast.makeText(getContext(), "Bienvenido Administrador (offline)", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigate(R.id.nav_admin_home);
                    } else {
                        Toast.makeText(getContext(), "Bienvenido " + local.nombre, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigate(R.id.nav_inicio);
                    }
                } else {
                    Toast.makeText(getContext(), "No se encontró una cuenta con ese correo. Puedes registrarte.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void guardarSesion(Usuario usuario) {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_user_logged_in", true)
                .putString("user_id", usuario.id)
                .putString("user_email", usuario.email)
                .putString("user_nombre", usuario.nombre != null ? usuario.nombre : "Usuario")
                .putString("user_rol", usuario.rol != null ? usuario.rol : "CLIENTE")
                .apply();
    }
}
