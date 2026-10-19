package com.example.inmobiliariaventa.ui.usuarios;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.android.material.button.MaterialButton;

import java.util.UUID;
import java.util.regex.Pattern;

public class RegistroFragment extends Fragment {

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private ImageView ivReqLength;
    private TextView tvReqLength;
    private ImageView ivReqUppercase;
    private TextView tvReqUppercase;
    private ImageView ivReqNumber;
    private TextView tvReqNumber;
    private ImageView ivReqMatch;
    private TextView tvReqMatch;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        EditText etFullName = view.findViewById(R.id.etFullName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        ImageView btnTogglePassword = view.findViewById(R.id.btnTogglePassword);
        ImageView btnToggleConfirmPassword = view.findViewById(R.id.btnToggleConfirmPassword);
        MaterialButton btnRegister = view.findViewById(R.id.btnRegister);
        TextView txtLoginLink = view.findViewById(R.id.txtLoginLink);

        // Requisitos views
        ivReqLength = view.findViewById(R.id.ivReqLength);
        tvReqLength = view.findViewById(R.id.tvReqLength);
        ivReqUppercase = view.findViewById(R.id.ivReqUppercase);
        tvReqUppercase = view.findViewById(R.id.tvReqUppercase);
        ivReqNumber = view.findViewById(R.id.ivReqNumber);
        tvReqNumber = view.findViewById(R.id.tvReqNumber);
        ivReqMatch = view.findViewById(R.id.ivReqMatch);
        tvReqMatch = view.findViewById(R.id.tvReqMatch);

        // Toggle visibilidad de contraseña
        if (btnTogglePassword != null && etPassword != null) {
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
        }

        // Toggle visibilidad de confirmar contraseña
        if (btnToggleConfirmPassword != null && etConfirmPassword != null) {
            btnToggleConfirmPassword.setOnClickListener(v -> {
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                if (isConfirmPasswordVisible) {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility);
                } else {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            });
        }

        // TextWatcher para chequear requisitos en tiempo real
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString() : "";
                String confirm = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
                updateRequirements(pass, confirm);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        if (etPassword != null) etPassword.addTextChangedListener(passwordWatcher);
        if (etConfirmPassword != null) etConfirmPassword.addTextChangedListener(passwordWatcher);

        // Estado inicial de los requisitos
        updateRequirements("", "");

        // Navegar a login
        if (txtLoginLink != null) {
            txtLoginLink.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Flecha de regreso
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Registro real con Supabase y Room
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
                String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                String confirmPassword = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

                if (TextUtils.isEmpty(fullName)) {
                    if (etFullName != null) etFullName.setError("Ingrese su nombre completo");
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    if (etEmail != null) etEmail.setError("Ingrese su correo");
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (etEmail != null) etEmail.setError("Ingrese un correo válido");
                    return;
                }

                if (password.length() < 6) {
                    if (etPassword != null) etPassword.setError("La contraseña debe tener al menos 6 caracteres");
                    return;
                }

                if (!Pattern.compile("[A-Z]").matcher(password).find()) {
                    if (etPassword != null) etPassword.setError("La contraseña debe incluir al menos una letra mayúscula");
                    return;
                }

                if (!Pattern.compile("[0-9]").matcher(password).find()) {
                    if (etPassword != null) etPassword.setError("La contraseña debe incluir al menos un número");
                    return;
                }

                if (TextUtils.isEmpty(confirmPassword)) {
                    if (etConfirmPassword != null) etConfirmPassword.setError("Confirme su contraseña");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    if (etConfirmPassword != null) etConfirmPassword.setError("Las contraseñas no coinciden");
                    return;
                }

                btnRegister.setEnabled(false);
                btnRegister.setText("Creando cuenta...");

                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.id = UUID.randomUUID().toString();
                nuevoUsuario.nombre = fullName;
                nuevoUsuario.email = email.toLowerCase();
                nuevoUsuario.rol = "CLIENTE";
                nuevoUsuario.estadoSincronizacion = "PENDIENTE_SUBIR";

                // Guardar en Supabase
                SupabaseClient.registrarUsuario(nuevoUsuario, new SupabaseClient.SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (!isAdded()) return;

                        nuevoUsuario.estadoSincronizacion = "SINCRONIZADO";

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

                        mostrarSnackbarElegante("¡Bienvenido, " + nuevoUsuario.nombre + "!", true);
                        Navigation.findNavController(v).navigate(R.id.nav_inicio);
                    }

                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        btnRegister.setText("Registrarse");
                        btnRegister.setEnabled(true);

                        String msg = error != null && error.getMessage() != null ? error.getMessage() : "";
                        if (msg.contains("23505") || msg.contains("duplicate") || msg.contains("409")) {
                            if (etEmail != null) etEmail.setError("Este correo ya está registrado");
                            mostrarSnackbarElegante("Ya existe una cuenta con este correo electrónico", false);
                        } else {
                            mostrarSnackbarElegante("No se pudo registrar en la nube. Verifique su conexión.", false);
                        }
                    }
                });
            });
        }

        return view;
    }

    private void updateRequirements(String password, String confirmPassword) {
        if (!isAdded() || getContext() == null) return;

        boolean hasMinLength = password.length() >= 6;
        boolean hasUppercase = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasNumber = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasMatch = !password.isEmpty() && password.equals(confirmPassword);

        int colorOk = ContextCompat.getColor(requireContext(), R.color.colorSecondary);
        int colorPending = ContextCompat.getColor(requireContext(), R.color.colorOutline);

        setRequirementState(ivReqLength, tvReqLength, hasMinLength, colorOk, colorPending);
        setRequirementState(ivReqUppercase, tvReqUppercase, hasUppercase, colorOk, colorPending);
        setRequirementState(ivReqNumber, tvReqNumber, hasNumber, colorOk, colorPending);
        setRequirementState(ivReqMatch, tvReqMatch, hasMatch, colorOk, colorPending);
    }

    private void setRequirementState(ImageView iv, TextView tv, boolean fulfilled, int colorOk, int colorPending) {
        if (iv != null) {
            iv.setImageResource(fulfilled ? R.drawable.ic_check_circle : R.drawable.ic_circle_outline);
            iv.setColorFilter(fulfilled ? colorOk : colorPending);
        }
        if (tv != null) {
            tv.setTextColor(fulfilled ? colorOk : colorPending);
        }
    }

    private void mostrarSnackbarElegante(String mensaje, boolean tieneBottomNav) {
        if (!isAdded() || getActivity() == null) return;
        View rootView = requireActivity().findViewById(R.id.main);
        if (rootView == null) {
            rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        }
        if (rootView == null) {
            rootView = getView();
        }
        if (rootView == null) return;

        Snackbar snackbar = Snackbar.make(rootView, mensaje, Snackbar.LENGTH_SHORT)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
        snackbarView.setBackgroundTintList(null);

        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setMaxLines(3);
        }

        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
            int marginH = (int) (16 * getResources().getDisplayMetrics().density);
            int marginB = (int) (12 * getResources().getDisplayMetrics().density);
            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null && tieneBottomNav) {
            snackbar.setAnchorView(bottomNav);
        }

        snackbar.show();
    }
}
