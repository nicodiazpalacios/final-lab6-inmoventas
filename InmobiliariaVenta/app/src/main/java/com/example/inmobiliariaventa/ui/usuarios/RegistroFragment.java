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

public class RegistroFragment extends Fragment {

    private boolean isPasswordVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);
        
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
            // Move cursor to the end
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
        
        // Simulate registration button action
        btnRegister.setOnClickListener(v -> {
            btnRegister.setEnabled(false);
            btnRegister.setText("Registrando...");
            v.postDelayed(() -> {
                if (isAdded()) {
                    btnRegister.setText("Registrarse");
                    btnRegister.setEnabled(true);
                    // Navigate back to login or auto-login
                    Navigation.findNavController(v).navigateUp();
                }
            }, 1500);
        });

        return view;
    }
}
