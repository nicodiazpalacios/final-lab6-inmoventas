package com.example.inmobiliariaventa.ui.admin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AdminHomeFragment extends Fragment {

    private ActividadRecienteAdapter actividadAdapter;
    private EmpresaRepository empresaRepository;

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Banner Modo Sin Conexión
        View cardModoOffline = view.findViewById(R.id.cardModoOffline);
        if (cardModoOffline != null && getContext() != null) {
            com.example.inmobiliariaventa.utils.NetworkUtils.getNetworkLiveData(requireContext())
                    .observe(getViewLifecycleOwner(), isOnline -> {
                        cardModoOffline.setVisibility(Boolean.TRUE.equals(isOnline) ? View.GONE : View.VISIBLE);
                    });
        }

        // 2. Search Container (idéntico a Home Usuario)
        View searchContainer = view.findViewById(R.id.searchContainer);
        if (searchContainer != null) {
            searchContainer.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_propiedades);
            });
        }

        // 3. Botón Publicar Nueva Propiedad
        View btnAdminNuevaPropiedad = view.findViewById(R.id.btnAdminNuevaPropiedad);
        if (btnAdminNuevaPropiedad != null) {
            btnAdminNuevaPropiedad.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }

        // 4. Botón Alternar Modo Usuario
        View btnSwitchUserMode = view.findViewById(R.id.btnSwitchUserMode);
        if (btnSwitchUserMode != null) {
            btnSwitchUserMode.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_inicio);
            });
        }

        // 6. Módulos de Gestión (Tarjetas de 100dp con bg_icon_circle)
        View cardPropiedades = view.findViewById(R.id.card_propiedades);
        if (cardPropiedades != null) {
            cardPropiedades.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_propiedades);
            });
        }

        View cardMensajes = view.findViewById(R.id.card_mensajes);
        android.widget.TextView tvMensajesSub = view.findViewById(R.id.tv_admin_mensajes_subtitulo);
        if (cardMensajes != null) {
            cardMensajes.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_mensajes);
            });

            if (tvMensajesSub != null && getContext() != null) {
                com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl.getInstance(requireContext())
                        .sincronizarConNube(null);

                com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl.getInstance(requireContext())
                        .getMensajesNoLeidosCount()
                        .observe(getViewLifecycleOwner(), unreadCount -> {
                            if (unreadCount != null && unreadCount > 0) {
                                tvMensajesSub.setText(unreadCount + " nuevos");
                                tvMensajesSub.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                            } else {
                                tvMensajesSub.setText("Mensajes");
                                tvMensajesSub.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
                            }
                        });
            }
        }

        View cardVentas = view.findViewById(R.id.card_ventas);
        if (cardVentas != null) {
            cardVentas.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_ventas);
            });
        }

        View cardContratos = view.findViewById(R.id.card_contratos);
        if (cardContratos != null) {
            cardContratos.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_contratos);
            });
            if (getContext() != null) {
                android.content.Context appCtx = requireContext().getApplicationContext();
                com.example.inmobiliariaventa.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                    com.example.inmobiliariaventa.data.local.AppDatabase db =
                            com.example.inmobiliariaventa.data.local.AppDatabase.getInstance(appCtx);
                    List<com.example.inmobiliariaventa.domain.entity.Contrato> locales = db.contratoDao().obtenerTodosSync();
                    if (locales != null) {
                        for (com.example.inmobiliariaventa.domain.entity.Contrato local : locales) {
                            com.example.inmobiliariaventa.data.remote.SupabaseClient.guardarContrato(local, null);
                        }
                    }
                });
            }
        }

        // 7. Cabecera Botón de Notificaciones
        View notificationsButton = view.findViewById(R.id.notificationsButton);
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }

        // 8. Botón Ver Todas las Notificaciones
        View btnVerTodas = view.findViewById(R.id.btnVerTodasNotificaciones);
        if (btnVerTodas != null) {
            btnVerTodas.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }

        // 9. Lista de Actividad Reciente (Novedades de inventario)
        RecyclerView rvActividadReciente = view.findViewById(R.id.rvActividadReciente);
        if (rvActividadReciente != null && getContext() != null) {
            rvActividadReciente.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            rvActividadReciente.setHasFixedSize(true);
            rvActividadReciente.setNestedScrollingEnabled(false);

            List<ActividadAdmin> actividades = com.example.inmobiliariaventa.data.repository.ActividadRepository
                    .getInstance(requireContext())
                    .getActividades();
            actividadAdapter = new ActividadRecienteAdapter(actividades, actividad -> {
                // Al tocar una actividad, navegar al centro de notificaciones de admin
                try {
                    Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
                } catch (Exception ignored) {}
            });
            rvActividadReciente.setAdapter(actividadAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (actividadAdapter != null && getContext() != null) {
            actividadAdapter.setActividades(
                    com.example.inmobiliariaventa.data.repository.ActividadRepository
                            .getInstance(requireContext())
                            .getActividades()
            );
        }
    }

    private void mostrarDialogoEditarDatosEmpresa() {
        if (!isAdded() || getContext() == null || getActivity() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_editar_datos_empresa, null);

        EditText etNombre = dialogView.findViewById(R.id.et_dialog_empresa_nombre);
        EditText etTelefono = dialogView.findViewById(R.id.et_dialog_empresa_telefono);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_empresa_email);
        EditText etDireccion = dialogView.findViewById(R.id.et_dialog_empresa_direccion);
        TextView tvError = dialogView.findViewById(R.id.dialog_empresa_tv_error);

        android.widget.ImageButton btnCerrar = dialogView.findViewById(R.id.dialog_empresa_btn_cerrar);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.dialog_empresa_btn_cancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.dialog_empresa_btn_guardar);

        if (etNombre != null) etNombre.setText(empresaRepository.getNombreEmpresaInmediato());
        if (etTelefono != null) etTelefono.setText(empresaRepository.getTelefonoWhatsappInmediato());
        if (etEmail != null) etEmail.setText(empresaRepository.getEmailInmediato());
        if (etDireccion != null) etDireccion.setText(empresaRepository.getDireccionInmediata());

        empresaRepository.cargarConfiguracionAsync(config -> {
            if (config != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (etNombre != null && config.nombreEmpresa != null) etNombre.setText(config.nombreEmpresa);
                    if (etTelefono != null && config.telefonoWhatsapp != null) etTelefono.setText(config.telefonoWhatsapp);
                    if (etEmail != null && config.email != null) etEmail.setText(config.email);
                    if (etDireccion != null && config.direccion != null) etDireccion.setText(config.direccion);
                });
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View.OnClickListener dismissWithKeyboard = v -> {
            View focus = dialog.getCurrentFocus();
            if (focus != null && getContext() != null) {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            dialog.dismiss();
        };

        if (btnCerrar != null) btnCerrar.setOnClickListener(dismissWithKeyboard);
        if (btnCancelar != null) btnCancelar.setOnClickListener(dismissWithKeyboard);

        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> {
                String nuevoNombre = etNombre != null && etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                String nuevoTelefono = etTelefono != null && etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
                String nuevoEmail = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String nuevaDireccion = etDireccion != null && etDireccion.getText() != null ? etDireccion.getText().toString().trim() : "";

                if (nuevoNombre.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa el nombre de la empresa");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    if (etNombre != null) etNombre.requestFocus();
                    return;
                }

                if (nuevoEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa un correo electrónico oficial válido");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    if (etEmail != null) etEmail.requestFocus();
                    return;
                }

                if (nuevoTelefono.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa un teléfono o WhatsApp de contacto");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    if (etTelefono != null) etTelefono.requestFocus();
                    return;
                }

                if (nuevaDireccion.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa la dirección de la inmobiliaria");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    if (etDireccion != null) etDireccion.requestFocus();
                    return;
                }

                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando...");

                empresaRepository.guardarConfiguracion(nuevoNombre, nuevoTelefono, nuevoEmail, nuevaDireccion);

                View focus = dialog.getCurrentFocus();
                if (focus != null && getContext() != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
                dialog.dismiss();
                mostrarSnackbarElegante("Datos de la empresa actualizados correctamente");
            });
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
            marginParams.setMargins(marginH, 0, marginH, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        if (lp instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }
        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNav);
        }

        snackbar.show();
    }
}
