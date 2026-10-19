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
import com.example.inmobiliariaventa.data.repository.FavoritoRepositoryImpl;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class LoginFragment extends Fragment {

    private boolean isPasswordVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        ImageView btnTogglePassword = view.findViewById(R.id.btnTogglePassword);
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);
        MaterialCheckBox cbRecordarme = view.findViewById(R.id.cbRecordarme);

        // Pre-cargar credenciales si el usuario marcó "Recordarme" previamente
        SharedPreferences loginPrefs = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        boolean rememberMe = loginPrefs.getBoolean("remember_me", false);
        if (rememberMe) {
            String savedEmail = loginPrefs.getString("saved_email", "");
            String savedPassword = loginPrefs.getString("saved_password", "");
            if (!savedEmail.isEmpty()) {
                etEmail.setText(savedEmail);
            }
            if (!savedPassword.isEmpty()) {
                etPassword.setText(savedPassword);
            }
            if (cbRecordarme != null) {
                cbRecordarme.setChecked(true);
            }
        }

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

            boolean isRememberChecked = cbRecordarme != null && cbRecordarme.isChecked();

            btnLogin.setEnabled(false);
            btnLogin.setText("Verificando cuenta...");

            SupabaseClient.obtenerUsuarioPorEmail(email.toLowerCase(), new SupabaseClient.SupabaseCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    if (!isAdded()) return;

                    if (usuario != null) {
                        // Procesar opción Recordarme
                        procesarRecordarme(email, password, isRememberChecked);

                        // Guardar sesión en SharedPreferences
                        guardarSesion(usuario);

                        // Guardar en base local Room y sincronizar sus favoritos
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getInstance(requireContext()).usuarioDao().insertar(usuario);
                        });
                        FavoritoRepositoryImpl.getInstance(requireContext()).sincronizarConNube(usuario.id, null);

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
                        verificarLocal(email, password, isRememberChecked, v, btnLogin);
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (!isAdded()) return;
                    verificarLocal(email, password, isRememberChecked, v, btnLogin);
                }
            });
        });

        return view;
    }

    private void verificarLocal(String email, String password, boolean isRememberChecked, View view, MaterialButton btnLogin) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Usuario local = AppDatabase.getInstance(requireContext()).usuarioDao().obtenerPorEmail(email);
            view.post(() -> {
                if (!isAdded()) return;
                btnLogin.setText("Iniciar sesión");
                btnLogin.setEnabled(true);

                if (local != null) {
                    procesarRecordarme(email, password, isRememberChecked);
                    guardarSesion(local);
                    FavoritoRepositoryImpl.getInstance(requireContext()).sincronizarConNube(local.id, null);
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

    private void procesarRecordarme(String email, String password, boolean isRememberChecked) {
        if (getContext() == null) return;
        SharedPreferences loginPrefs = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        if (isRememberChecked) {
            loginPrefs.edit()
                    .putBoolean("remember_me", true)
                    .putString("saved_email", email)
                    .putString("saved_password", password)
                    .apply();
        } else {
            loginPrefs.edit()
                    .putBoolean("remember_me", false)
                    .remove("saved_email")
                    .remove("saved_password")
                    .apply();
        }
    }

    private void guardarSesion(Usuario usuario) {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean("is_user_logged_in", true)
                .putString("user_id", usuario.id)
                .putString("user_email", usuario.email)
                .putString("user_nombre", usuario.nombre != null ? usuario.nombre : "Usuario")
                .putString("user_telefono", usuario.telefono != null ? usuario.telefono : "")
                .putString("user_rol", usuario.rol != null ? usuario.rol : "CLIENTE");

        if (usuario.fotoPerfil != null && !usuario.fotoPerfil.isEmpty()) {
            editor.putString("user_foto_perfil", usuario.fotoPerfil);
        } else {
            editor.remove("user_foto_perfil");
        }
        editor.apply();
    }
}
