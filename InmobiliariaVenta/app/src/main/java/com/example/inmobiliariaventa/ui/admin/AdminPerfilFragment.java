package com.example.inmobiliariaventa.ui.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.google.android.material.snackbar.Snackbar;

public class AdminPerfilFragment extends Fragment {

    public AdminPerfilFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración de Cuenta
        view.findViewById(R.id.btn_personal_info).setOnClickListener(v -> {
            mostrarSnackbarElegante("Información Personal");
        });

        view.findViewById(R.id.btn_security).setOnClickListener(v -> {
            mostrarSnackbarElegante("Seguridad de la cuenta");
        });

        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            mostrarSnackbarElegante("Notificaciones del sistema");
        });

        View btnMensajes = view.findViewById(R.id.btn_mensajes_clientes);
        if (btnMensajes != null) {
            btnMensajes.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_mensajes);
            });
        }

        // Logout con confirmación y snackbar elegante
        View btnLogout = view.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> mostrarDialogoCerrarSesion(view));
        }
    }

    private void mostrarDialogoCerrarSesion(View rootView) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_cerrar_sesion, null);

        com.google.android.material.button.MaterialButton btnSi = dialogView.findViewById(R.id.btn_dialog_logout_si);
        com.google.android.material.button.MaterialButton btnNo = dialogView.findViewById(R.id.btn_dialog_logout_no);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnSi != null) {
            btnSi.setOnClickListener(v -> {
                dialog.dismiss();
                SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                mostrarSnackbarElegante("Sesión cerrada correctamente");
                Navigation.findNavController(rootView).navigate(R.id.nav_inicio);
            });
        }

        if (btnNo != null) {
            btnNo.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void mostrarSnackbarElegante(String mensaje) {
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
            int marginB = (int) (8 * getResources().getDisplayMetrics().density);
            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNav);
        }

        snackbar.show();
    }
}
