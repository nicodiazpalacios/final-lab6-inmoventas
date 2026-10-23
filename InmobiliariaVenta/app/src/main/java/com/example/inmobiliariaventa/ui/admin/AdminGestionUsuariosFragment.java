package com.example.inmobiliariaventa.ui.admin;

import com.example.inmobiliariaventa.utils.CartelUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminGestionUsuariosFragment extends Fragment {

    private LinearLayout layoutUsersContainer;
    private LinearLayout layoutEmptyUsers;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private TextInputEditText etBuscarUsuario;
    private ChipGroup chipGroupRoles;

    private static volatile List<Usuario> cachedUsuariosGlobal = null;
    private final List<Usuario> allUsers = new ArrayList<>();

    public AdminGestionUsuariosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_gestion_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutUsersContainer = view.findViewById(R.id.layout_users_container);
        layoutEmptyUsers = view.findViewById(R.id.layout_empty_users);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_users_title);
        tvEmptySubtitle = view.findViewById(R.id.tv_empty_users_subtitle);

        etBuscarUsuario = view.findViewById(R.id.et_buscar_usuario);
        chipGroupRoles = view.findViewById(R.id.chip_group_roles);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).popBackStack();
            });
        }

        setupSearchAndFilter();

        // Precarga instantánea en memoria (0 ms) para evitar parpadeos
        if (cachedUsuariosGlobal != null && !cachedUsuariosGlobal.isEmpty()) {
            allUsers.clear();
            allUsers.addAll(cachedUsuariosGlobal);
            renderUsers();
        }

        observarYSincronizarUsuarios();
    }

    private void observarYSincronizarUsuarios() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());

        // Observar usuarios registrados en la base de datos local (Room)
        db.usuarioDao().obtenerTodos().observe(getViewLifecycleOwner(), usuarios -> {
            if (usuarios == null) return;
            if (sonListasUsuariosIguales(allUsers, usuarios)) {
                return;
            }
            allUsers.clear();
            allUsers.addAll(usuarios);
            cachedUsuariosGlobal = new ArrayList<>(usuarios);
            renderUsers();
        });

        // Sincronizar con Supabase para traer todos los usuarios registrados en la nube
        SupabaseClient.obtenerUsuarios(new SupabaseClient.SupabaseCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> remotos) {
                if (!isAdded() || getContext() == null || remotos == null) return;
                for (Usuario u : remotos) {
                    u.password = null;
                    u.estadoSincronizacion = "SINCRONIZADO";
                }
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    db.usuarioDao().insertarTodas(remotos);
                });
            }

            @Override
            public void onError(Exception error) {
                // En modo offline se muestran los usuarios almacenados en Room
            }
        });
    }

    private void setupSearchAndFilter() {
        if (etBuscarUsuario != null) {
            etBuscarUsuario.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderUsers();
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }

        if (chipGroupRoles != null) {
            chipGroupRoles.setOnCheckedChangeListener((group, checkedId) -> renderUsers());
        }
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.rol != null && usuario.rol.trim().toUpperCase().contains("ADMIN");
    }

    private boolean esUsuarioEnSesion(Usuario usuario) {
        if (usuario == null || getContext() == null) return false;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String currentId = prefs.getString("user_id", "");
        String currentEmail = prefs.getString("user_email", "");

        if (currentId != null && !currentId.isEmpty() && usuario.id != null && currentId.equals(usuario.id)) {
            return true;
        }
        return currentEmail != null && !currentEmail.isEmpty() && usuario.email != null
                && currentEmail.trim().equalsIgnoreCase(usuario.email.trim());
    }

    private void renderUsers() {
        if (layoutUsersContainer == null || getContext() == null) return;

        String query = "";
        if (etBuscarUsuario != null && etBuscarUsuario.getText() != null) {
            query = etBuscarUsuario.getText().toString().trim().toLowerCase();
        }

        int checkedId = chipGroupRoles != null ? chipGroupRoles.getCheckedChipId() : R.id.chip_todos;

        List<Usuario> filtered = new ArrayList<>();
        for (Usuario u : allUsers) {
            boolean isAdminRole = esAdmin(u);

            boolean matchesRole = true;
            if (checkedId == R.id.chip_usuarios) {
                matchesRole = !isAdminRole;
            } else if (checkedId == R.id.chip_admins) {
                matchesRole = isAdminRole;
            }

            String nombre = u.nombre != null ? u.nombre.toLowerCase() : "";
            String email = u.email != null ? u.email.toLowerCase() : "";
            String tel = u.telefono != null ? u.telefono.toLowerCase() : "";
            boolean matchesQuery = query.isEmpty() || nombre.contains(query) || email.contains(query) || tel.contains(query);

            if (matchesRole && matchesQuery) {
                filtered.add(u);
            }
        }

        if (filtered.isEmpty()) {
            layoutUsersContainer.removeAllViews();
            if (layoutEmptyUsers != null) {
                layoutEmptyUsers.setVisibility(View.VISIBLE);
                if (allUsers.isEmpty()) {
                    if (tvEmptyTitle != null) tvEmptyTitle.setText("Sin usuarios registrados");
                    if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Todavía no hay cuentas registradas en la base de datos.");
                } else {
                    if (tvEmptyTitle != null) tvEmptyTitle.setText("No se encontraron usuarios");
                    if (tvEmptySubtitle != null) tvEmptySubtitle.setText("No hay usuarios que coincidan con la búsqueda o el filtro seleccionado.");
                }
            }
            return;
        }

        if (layoutEmptyUsers != null) {
            layoutEmptyUsers.setVisibility(View.GONE);
        }

        // Reciclado inteligente de vistas para mantener las fotos cargadas en memoria sin parpadeos
        Map<String, View> existingViews = new HashMap<>();
        for (int i = 0; i < layoutUsersContainer.getChildCount(); i++) {
            View child = layoutUsersContainer.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String) {
                existingViews.put((String) tag, child);
            }
        }
        layoutUsersContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Usuario usuario : filtered) {
            String userId = usuario.id != null ? usuario.id : (usuario.email != null ? usuario.email : "");
            View cardView = existingViews.remove(userId);
            if (cardView == null) {
                cardView = inflater.inflate(R.layout.item_usuario_admin_card, layoutUsersContainer, false);
                cardView.setTag(userId);
            }

            ImageView ivAvatar = cardView.findViewById(R.id.iv_avatar_usuario);
            TextView tvNombre = cardView.findViewById(R.id.tv_nombre_usuario);
            TextView tvBadgeTu = cardView.findViewById(R.id.tv_badge_tu);
            TextView tvRol = cardView.findViewById(R.id.tv_rol_usuario);
            TextView tvEmail = cardView.findViewById(R.id.tv_email_usuario);
            View layoutTelefono = cardView.findViewById(R.id.layout_telefono_usuario);
            TextView tvTelefono = cardView.findViewById(R.id.tv_telefono_usuario);
            View btnEdit = cardView.findViewById(R.id.btn_edit_usuario);
            View btnDelete = cardView.findViewById(R.id.btn_delete_usuario);

            String nombreDisplay = (usuario.nombre != null && !usuario.nombre.trim().isEmpty())
                    ? usuario.nombre
                    : "Usuario sin nombre";
            String emailDisplay = (usuario.email != null && !usuario.email.trim().isEmpty())
                    ? usuario.email
                    : "Sin correo registrado";

            tvNombre.setText(nombreDisplay);
            tvEmail.setText(emailDisplay);

            // Mostrar teléfono si el usuario lo tiene registrado
            if (layoutTelefono != null && tvTelefono != null) {
                if (usuario.telefono != null && !usuario.telefono.trim().isEmpty()) {
                    tvTelefono.setText(usuario.telefono.trim());
                    layoutTelefono.setVisibility(View.VISIBLE);
                } else {
                    layoutTelefono.setVisibility(View.GONE);
                }
            }

            boolean isCurrentSessionUser = esUsuarioEnSesion(usuario);
            if (tvBadgeTu != null) {
                tvBadgeTu.setVisibility(isCurrentSessionUser ? View.VISIBLE : View.GONE);
            }

            boolean isAdmin = esAdmin(usuario);
            if (isAdmin) {
                tvRol.setText("ADMIN");
                tvRol.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dce1ff")));
                tvRol.setTextColor(Color.parseColor("#00164e"));
            } else {
                tvRol.setText("USUARIO");
                tvRol.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant)));
                tvRol.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant));
            }

            cargarAvatar(ivAvatar, usuario.fotoPerfil);

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (isCurrentSessionUser) {
                        mostrarDialogoAutoProteccion("No podés modificar tu propio rol de Administrador mientras tenés la sesión activa.");
                    } else {
                        mostrarDialogoCambiarRol(usuario, nombreDisplay, isAdmin);
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (isCurrentSessionUser) {
                        mostrarDialogoAutoProteccion("No podés eliminar tu propia cuenta de Administrador mientras tenés la sesión activa.");
                    } else {
                        mostrarDialogoEliminarUsuario(usuario, nombreDisplay);
                    }
                });
            }

            layoutUsersContainer.addView(cardView);
        }
    }

    private void cargarAvatar(ImageView ivAvatar, String pathOrData) {
        if (ivAvatar == null || getContext() == null) return;

        // Evitar recargar o reiniciar la imagen si ya tiene exactamente esta foto cargada
        Object currentTag = ivAvatar.getTag(R.id.iv_avatar_usuario);
        if (pathOrData != null && !pathOrData.trim().isEmpty() && pathOrData.equals(currentTag)) {
            return;
        }

        if (pathOrData != null && !pathOrData.trim().isEmpty()) {
            ivAvatar.setTag(R.id.iv_avatar_usuario, pathOrData);
            ivAvatar.setImageTintList(null);
            ivAvatar.clearColorFilter();
            Context appCtx = ivAvatar.getContext().getApplicationContext();

            if (pathOrData.startsWith("data:image/") || pathOrData.startsWith("/9j/")) {
                try {
                    String base64Data = pathOrData.contains(",")
                            ? pathOrData.substring(pathOrData.indexOf(",") + 1)
                            : pathOrData;
                    byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    Glide.with(appCtx)
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .dontAnimate()
                            .placeholder(ivAvatar.getDrawable())
                            .circleCrop()
                            .into(ivAvatar);
                    return;
                } catch (Exception ignored) { }
            }

            if (pathOrData.startsWith("http://") || pathOrData.startsWith("https://")) {
                Glide.with(appCtx)
                        .load(pathOrData)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .dontAnimate()
                        .placeholder(ivAvatar.getDrawable())
                        .circleCrop()
                        .into(ivAvatar);
                return;
            }

            File file = new File(pathOrData);
            if (file.exists()) {
                Glide.with(appCtx)
                        .load(file)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .dontAnimate()
                        .placeholder(ivAvatar.getDrawable())
                        .circleCrop()
                        .into(ivAvatar);
                return;
            }
        }

        ivAvatar.setTag(R.id.iv_avatar_usuario, null);
        ivAvatar.setImageResource(R.drawable.ic_person);
        if (getContext() != null) {
            ivAvatar.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant)));
        }
    }

    private boolean sonListasUsuariosIguales(List<Usuario> l1, List<Usuario> l2) {
        if (l1 == l2) return true;
        if (l1 == null || l2 == null) return false;
        if (l1.size() != l2.size()) return false;
        for (int i = 0; i < l1.size(); i++) {
            Usuario u1 = l1.get(i);
            Usuario u2 = l2.get(i);
            if (u1 == null || u2 == null) return false;
            if (!equalsSafe(u1.id, u2.id)) return false;
            if (!equalsSafe(u1.nombre, u2.nombre)) return false;
            if (!equalsSafe(u1.email, u2.email)) return false;
            if (!equalsSafe(u1.rol, u2.rol)) return false;
            if (!equalsSafe(u1.telefono, u2.telefono)) return false;
            if (!equalsSafe(u1.fotoPerfil, u2.fotoPerfil)) return false;
        }
        return true;
    }

    private static boolean equalsSafe(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }

    private void mostrarDialogoAutoProteccion(String mensaje) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_aviso_propiedad_contrato, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_mensaje);
        MaterialButton btnCerrar = dialogView.findViewById(R.id.btn_dialog_aviso_cerrar);
        MaterialButton btnSecundario = dialogView.findViewById(R.id.btn_dialog_aviso_ver_contratos);

        if (tvTitulo != null) {
            tvTitulo.setText("Acción no permitida");
        }
        if (tvMensaje != null) {
            tvMensaje.setText(mensaje);
        }
        if (btnCerrar != null) {
            btnCerrar.setVisibility(View.GONE);
        }
        if (btnSecundario != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnSecundario.getLayoutParams();
            params.weight = 2f;
            params.setMarginStart(0);
            btnSecundario.setLayoutParams(params);
            btnSecundario.setText("Entendido");
            btnSecundario.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void mostrarDialogoCambiarRol(Usuario usuario, String nombreDisplay, boolean eraAdmin) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_cambiar_rol_usuario, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvSubtitulo = dialogView.findViewById(R.id.tv_dialog_rol_subtitulo);
        MaterialCardView cardRolUsuario = dialogView.findViewById(R.id.card_opcion_rol_usuario);
        MaterialCardView cardRolAdmin = dialogView.findViewById(R.id.card_opcion_rol_admin);
        ImageView ivCheckUsuario = dialogView.findViewById(R.id.iv_check_rol_usuario);
        ImageView ivCheckAdmin = dialogView.findViewById(R.id.iv_check_rol_admin);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_rol_cancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btn_dialog_rol_guardar);

        if (tvSubtitulo != null) {
            tvSubtitulo.setText("Selecciona el nivel de permisos para " + nombreDisplay + ":");
        }

        final boolean[] seleccionAdmin = { eraAdmin };

        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int colorOutline = ContextCompat.getColor(requireContext(), R.color.colorOutlineVariant);
        int colorIconOutline = ContextCompat.getColor(requireContext(), R.color.colorOutline);
        int colorSelectedBg = Color.parseColor("#F4F6FF");
        int colorNormalBg = ContextCompat.getColor(requireContext(), R.color.colorSurfaceContainerLowest);

        Runnable actualizarSeleccionVisual = () -> {
            if (seleccionAdmin[0]) {
                if (cardRolAdmin != null) {
                    cardRolAdmin.setStrokeColor(colorPrimary);
                    cardRolAdmin.setCardBackgroundColor(colorSelectedBg);
                }
                if (ivCheckAdmin != null) {
                    ivCheckAdmin.setImageResource(R.drawable.ic_check_circle);
                    ivCheckAdmin.setImageTintList(ColorStateList.valueOf(colorPrimary));
                }
                if (cardRolUsuario != null) {
                    cardRolUsuario.setStrokeColor(colorOutline);
                    cardRolUsuario.setCardBackgroundColor(colorNormalBg);
                }
                if (ivCheckUsuario != null) {
                    ivCheckUsuario.setImageResource(R.drawable.ic_circle_outline);
                    ivCheckUsuario.setImageTintList(ColorStateList.valueOf(colorIconOutline));
                }
            } else {
                if (cardRolUsuario != null) {
                    cardRolUsuario.setStrokeColor(colorPrimary);
                    cardRolUsuario.setCardBackgroundColor(colorSelectedBg);
                }
                if (ivCheckUsuario != null) {
                    ivCheckUsuario.setImageResource(R.drawable.ic_check_circle);
                    ivCheckUsuario.setImageTintList(ColorStateList.valueOf(colorPrimary));
                }
                if (cardRolAdmin != null) {
                    cardRolAdmin.setStrokeColor(colorOutline);
                    cardRolAdmin.setCardBackgroundColor(colorNormalBg);
                }
                if (ivCheckAdmin != null) {
                    ivCheckAdmin.setImageResource(R.drawable.ic_circle_outline);
                    ivCheckAdmin.setImageTintList(ColorStateList.valueOf(colorIconOutline));
                }
            }
        };

        actualizarSeleccionVisual.run();

        if (cardRolUsuario != null) {
            cardRolUsuario.setOnClickListener(v -> {
                seleccionAdmin[0] = false;
                actualizarSeleccionVisual.run();
            });
        }
        if (cardRolAdmin != null) {
            cardRolAdmin.setOnClickListener(v -> {
                seleccionAdmin[0] = true;
                actualizarSeleccionVisual.run();
            });
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> {
                dialog.dismiss();
                if (seleccionAdmin[0] == eraAdmin) return;

                String nuevoRol = seleccionAdmin[0] ? "ADMIN" : "CLIENTE";
                String nuevoRolTexto = seleccionAdmin[0] ? "Administrador" : "Usuario";
                usuario.rol = nuevoRol;

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (getContext() != null) {
                        AppDatabase.getInstance(requireContext()).usuarioDao().actualizar(usuario);
                    }
                });
                SupabaseClient.actualizarRolUsuario(usuario.id, nuevoRol, null);
                CartelUtils.mostrar(requireContext(), "Rol de " + nombreDisplay + " actualizado a " + nuevoRolTexto);
            });
        }

        dialog.show();
    }

    private void mostrarDialogoEliminarUsuario(Usuario usuario, String nombreDisplay) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_aviso_propiedad_contrato, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_aviso_contrato_mensaje);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_aviso_cerrar);
        MaterialButton btnConfirmar = dialogView.findViewById(R.id.btn_dialog_aviso_ver_contratos);

        if (tvTitulo != null) {
            tvTitulo.setText("¿Eliminar usuario?");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("¿Estás seguro de que deseas eliminar la cuenta de \"" + nombreDisplay + "\" (" + (usuario.email != null ? usuario.email : "") + ")?\n\nEsta acción no se puede deshacer.");
        }
        if (btnCancelar != null) {
            btnCancelar.setText("No, volver");
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnConfirmar != null) {
            btnConfirmar.setText("Sí, eliminar");
            btnConfirmar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BA1A1A")));
            btnConfirmar.setTextColor(Color.WHITE);
            btnConfirmar.setOnClickListener(v -> {
                dialog.dismiss();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (getContext() != null) {
                        AppDatabase.getInstance(requireContext()).usuarioDao().eliminar(usuario);
                    }
                });
                SupabaseClient.eliminarUsuario(usuario.id, null);
                CartelUtils.mostrar(requireContext(), "Usuario " + nombreDisplay + " eliminado");
            });
        }

        dialog.show();
    }
}
