package com.example.inmobiliariaventa.ui.usuarios;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;

public class LoginFragment extends Fragment {

    private boolean isPasswordVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        
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
            // Move cursor to the end
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
        
        // Simulate login button action
        btnLogin.setOnClickListener(v -> {
            btnLogin.setEnabled(false);
            btnLogin.setText("Accediendo...");
            // Simulate a delay for login
            v.postDelayed(() -> {
                // Here we would authenticate the user and navigate
                if (isAdded()) {
                    btnLogin.setText("Iniciar sesión");
                    btnLogin.setEnabled(true);
                }
            }, 1200);
        });

        return view;
    }
}
