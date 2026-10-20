package com.example.inmobiliariaventa.ui.usuarios;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import com.example.inmobiliariaventa.utils.SecurityUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;
import java.util.regex.Pattern;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.example.inmobiliariaventa.domain.entity.Usuario;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PerfilUsuarioFragment extends Fragment {

    private EmpresaRepository empresaRepository;
    private ImageView ivFotoPerfil;
    private boolean tieneFotoPersonalizada = false;

    // Launcher para el recortador de imagen
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    if (croppedUri != null) {
                        guardarYAplicarFotoPerfil(croppedUri);
                    }
                } else if (result.getError() != null) {
                    mostrarSnackbarElegante("Error al recortar la imagen");
                }
            });

    // Launcher para seleccionar imagen de la galería
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    iniciarRecorte(uri);
                }
            });

    // Launcher para tomar foto rápida con la cámara
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null && getContext() != null) {
                    try {
                        File tempFile = new File(requireContext().getCacheDir(), "temp_camera_photo.jpg");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                        fos.flush();
                        fos.close();
                        iniciarRecorte(Uri.fromFile(tempFile));
                    } catch (Exception e) {
                        mostrarSnackbarElegante("Error al procesar la foto de cámara");
                    }
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_usuario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        empresaRepository = new EmpresaRepository(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String nombre = prefs.getString("user_nombre", "Usuario");
        String email = prefs.getString("user_email", "");

        ivFotoPerfil = view.findViewById(R.id.iv_foto_perfil);
        TextView tvNombre = view.findViewById(R.id.tv_nombre_usuario_perfil);
        TextView tvEmail = view.findViewById(R.id.tv_email_usuario_perfil);
        TextView tvSoporteTelefono = view.findViewById(R.id.tv_soporte_telefono);

        if (tvNombre != null) {
            tvNombre.setText(nombre);
        }
        if (tvEmail != null && !email.isEmpty()) {
            tvEmail.setText(email);
        }

        // Cargar foto de perfil previamente guardada si existe
        String fotoPath = prefs.getString("user_foto_perfil", null);
        mostrarFotoPerfil(fotoPath);

        // Click en la foto de perfil para cambiarla y recortarla
        View btnFoto = view.findViewById(R.id.btn_cambiar_foto_perfil);
        if (btnFoto != null) {
            btnFoto.setOnClickListener(v -> mostrarDialogoSeleccionFoto());
        }

        // Cargar teléfono oficial de la empresa de forma asíncrona
        empresaRepository.cargarConfiguracionAsync(config -> {
            if (config != null && config.telefonoWhatsapp != null && !config.telefonoWhatsapp.isEmpty()) {
                if (getActivity() != null && tvSoporteTelefono != null) {
                    getActivity().runOnUiThread(() -> tvSoporteTelefono.setText(config.telefonoWhatsapp));
                }
            }
        });

        // Consultar base de datos local para mantener datos sincronizados
        if (!userId.isEmpty()) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Usuario u = AppDatabase.getInstance(requireContext()).usuarioDao().obtenerUsuarioDirectoPorId(userId);
                if (u != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (tvNombre != null && u.nombre != null && !u.nombre.isEmpty()) {
                            tvNombre.setText(u.nombre);
                        }
                        if (tvEmail != null && u.email != null && !u.email.isEmpty()) {
                            tvEmail.setText(u.email);
                        }
                        if (u.fotoPerfil != null && !u.fotoPerfil.isEmpty()) {
                            mostrarFotoPerfil(u.fotoPerfil);
                        }
                    });
                }
            });
        }

        // Navegar a Mis Favoritos
        View btnFavoritos = view.findViewById(R.id.btn_perfil_favoritos);
        if (btnFavoritos != null) {
            btnFavoritos.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(v).navigate(R.id.action_nav_perfil_to_nav_guardados);
                } catch (Exception e) {
                    Navigation.findNavController(v).navigate(R.id.nav_guardados);
                }
            });
        }

        // Información Personal
        View btnInfoPersonal = view.findViewById(R.id.btn_perfil_info_personal);
        if (btnInfoPersonal != null) {
            btnInfoPersonal.setOnClickListener(v -> mostrarDialogoEditarInformacionPersonal());
        }

        // Seguridad
        View btnSeguridad = view.findViewById(R.id.btn_perfil_seguridad);
        if (btnSeguridad != null) {
            btnSeguridad.setOnClickListener(v -> mostrarDialogoCambiarContrasena());
        }

        // Notificaciones
        View btnNotificaciones = view.findViewById(R.id.btn_perfil_notificaciones);
        if (btnNotificaciones != null) {
            btnNotificaciones.setOnClickListener(v -> {
                mostrarSnackbarElegante("Notificaciones del sistema activadas.");
            });
        }

        // Botones rápidos de Centro de Ayuda
        View btnSoporteLlamar = view.findViewById(R.id.btn_soporte_llamar);
        if (btnSoporteLlamar != null) {
            btnSoporteLlamar.setOnClickListener(v -> llamarEmpresa());
        }

        View btnSoporteWhatsapp = view.findViewById(R.id.btn_soporte_whatsapp);
        if (btnSoporteWhatsapp != null) {
            btnSoporteWhatsapp.setOnClickListener(v -> abrirWhatsapp());
        }

        // Centro de Ayuda completo (click en la fila)
        View btnAyuda = view.findViewById(R.id.btn_perfil_ayuda);
        if (btnAyuda != null) {
            btnAyuda.setOnClickListener(v -> mostrarDialogoCentroAyuda());
        }

        // Cerrar Sesión
        CardView btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cerrar Sesión")
                        .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            prefs.edit().clear().apply();
                            mostrarSnackbarElegante("Sesión cerrada correctamente");
                            Navigation.findNavController(view).navigate(R.id.nav_inicio);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }

    private boolean tieneFotoPerfilPersonalizada() {
        if (tieneFotoPersonalizada) {
            return true;
        }
        if (getContext() == null) return false;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String path = prefs.getString("user_foto_perfil", null);
        if (path != null && !path.trim().isEmpty()) {
            if (path.startsWith("data:image/") || path.startsWith("/9j/") || path.length() > 200) {
                return true;
            }
            if (new File(path).exists()) {
                return true;
            }
        }
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty() && new File(requireContext().getFilesDir(), "profile_photo_" + userId + ".jpg").exists()) {
            return true;
        }
        return false;
    }

    private void mostrarDialogoSeleccionFoto() {
        if (getContext() == null) return;

        boolean tieneFoto = tieneFotoPerfilPersonalizada();

        String[] opciones = tieneFoto ?
                new String[]{"Elegir de la galería", "Tomar una foto", "Quitar foto de perfil"} :
                new String[]{"Elegir de la galería", "Tomar una foto"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Foto de perfil")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else if (which == 1) {
                        cameraLauncher.launch(null);
                    } else if (which == 2) {
                        confirmarQuitarFoto();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarQuitarFoto() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quitar foto")
                .setMessage("¿Estás seguro de que deseas quitar tu foto de perfil?")
                .setPositiveButton("Sí, quitar", (dialog, which) -> eliminarFotoPerfil())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void iniciarRecorte(Uri sourceUri) {
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.cropShape = CropImageView.CropShape.OVAL;
        cropOptions.guidelines = CropImageView.Guidelines.ON;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;
        cropOptions.fixAspectRatio = true; // Proporción circular 1:1 estricta para evitar deformaciones o bordes negros
        cropOptions.outputRequestWidth = 512;
        cropOptions.outputRequestHeight = 512;
        cropOptions.outputCompressFormat = Bitmap.CompressFormat.JPEG;
        cropOptions.activityTitle = "Recortar foto de perfil";
        cropOptions.cropMenuCropButtonTitle = "Guardar";

        CropImageContractOptions contractOptions = new CropImageContractOptions(sourceUri, cropOptions);
        cropImageLauncher.launch(contractOptions);
    }

    private void guardarYAplicarFotoPerfil(Uri uri) {
        if (getContext() == null) return;
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "local_user");
            File destFile = new File(requireContext().getFilesDir(), "profile_photo_" + userId + ".jpg");

            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                // Convertir y comprimir a Base64 para almacenar en base de datos (Room y Supabase)
                String base64Image = null;
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(destFile.getAbsolutePath());
                    if (bitmap != null) {
                        int maxDim = 320;
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        if (w > maxDim || h > maxDim) {
                            float ratio = (float) w / (float) h;
                            if (ratio > 1) {
                                w = maxDim;
                                h = (int) (maxDim / ratio);
                            } else {
                                h = maxDim;
                                w = (int) (maxDim * ratio);
                            }
                            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos);
                        byte[] imageBytes = baos.toByteArray();
                        base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    }
                } catch (Exception e) {
                    Log.e("PerfilUsuario", "Error al codificar imagen a base64", e);
                }

                final String finalFotoData = base64Image != null ? base64Image : destFile.getAbsolutePath();

                // 1. Guardar en SharedPreferences para acceso inmediato
                prefs.edit().putString("user_foto_perfil", destFile.getAbsolutePath()).apply();

                // 2. Guardar en Room (base de datos local)
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        AppDatabase.getInstance(requireContext())
                                .usuarioDao().actualizarFotoPerfil(userId, finalFotoData);
                    } catch (Exception e) {
                        Log.e("PerfilUsuario", "Error al guardar foto en Room", e);
                    }
                });

                // 3. Sincronizar en Supabase (base de datos remota)
                if (base64Image != null && !userId.isEmpty() && !"local_user".equals(userId)) {
                    SupabaseClient.actualizarFotoPerfil(userId, base64Image, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Log.d("PerfilUsuario", "Foto de perfil actualizada en Supabase.");
                        }

                        @Override
                        public void onError(Exception error) {
                            Log.w("PerfilUsuario", "Aviso al guardar en Supabase: " + error.getMessage());
                        }
                    });
                }

                mostrarFotoPerfil(destFile.getAbsolutePath());
                mostrarSnackbarElegante("Foto de perfil actualizada correctamente");
            }
        } catch (Exception e) {
            mostrarSnackbarElegante("No se pudo guardar la imagen: " + e.getMessage());
        }
    }

    private void mostrarFotoPerfil(String pathOrData) {
        if (ivFotoPerfil == null || getContext() == null) return;
        if (pathOrData != null && !pathOrData.trim().isEmpty()) {
            if (pathOrData.startsWith("data:image/") || pathOrData.startsWith("/9j/")) {
                try {
                    String base64Data = pathOrData.contains(",") ? pathOrData.substring(pathOrData.indexOf(",") + 1) : pathOrData;
                    byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    ivFotoPerfil.setImageTintList(null);
                    ivFotoPerfil.clearColorFilter();
                    ivFotoPerfil.setColorFilter(null);
                    Glide.with(this)
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .circleCrop()
                            .into(ivFotoPerfil);
                    tieneFotoPersonalizada = true;
                    return;
                } catch (Exception ignored) {}
            }
            File file = new File(pathOrData);
            if (file.exists()) {
                ivFotoPerfil.setImageTintList(null);
                ivFotoPerfil.clearColorFilter();
                ivFotoPerfil.setColorFilter(null);
                Glide.with(this)
                        .load(file)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(ivFotoPerfil);
                tieneFotoPersonalizada = true;
                return;
            }
        }
        tieneFotoPersonalizada = false;
        Glide.with(this).clear(ivFotoPerfil);
        ivFotoPerfil.clearColorFilter();
        ivFotoPerfil.setImageResource(R.drawable.ic_person);
        ivFotoPerfil.setImageTintList(ColorStateList.valueOf(0xFF1A1B21));
    }

    private void eliminarFotoPerfil() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "local_user");
        File destFile = new File(requireContext().getFilesDir(), "profile_photo_" + userId + ".jpg");
        if (destFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destFile.delete();
        }
        prefs.edit().remove("user_foto_perfil").apply();

        // 1. Eliminar en Room (base de datos local)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase.getInstance(requireContext())
                        .usuarioDao().actualizarFotoPerfil(userId, null);
            } catch (Exception ignored) {}
        });

        // 2. Eliminar en Supabase (base de datos remota)
        if (!userId.isEmpty() && !"local_user".equals(userId)) {
            SupabaseClient.actualizarFotoPerfil(userId, null, null);
        }

        mostrarFotoPerfil(null);
        mostrarSnackbarElegante("Foto de perfil eliminada correctamente");
    }

    private String getTelefonoEmpresa() {
        String tel = empresaRepository != null ? empresaRepository.getTelefonoWhatsappInmediato() : null;
        if (tel == null || tel.trim().isEmpty()) {
            tel = "+54 9 11 5842-1102";
        }
        return tel;
    }

    private void abrirWhatsapp() {
        try {
            String telefonoEmpresa = getTelefonoEmpresa();
            String cleanPhone = telefonoEmpresa.replaceAll("[^0-9]", "");
            String mensaje = "Hola, me comunico desde la aplicación Home Finder para realizar una consulta.";
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(mensaje);

            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(url));
            startActivity(whatsappIntent);
        } catch (Exception e) {
            mostrarSnackbarElegante("No se pudo abrir WhatsApp. Verifique tener la aplicación instalada.");
        }
    }

    private void llamarEmpresa() {
        try {
            String telefonoEmpresa = getTelefonoEmpresa();
            String cleanPhone = telefonoEmpresa.replaceAll("[^0-9+]", "");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + cleanPhone));
            startActivity(dialIntent);
        } catch (Exception e) {
            mostrarSnackbarElegante("No se pudo iniciar la llamada.");
        }
    }

    private void mostrarDialogoCentroAyuda() {
        if (getContext() == null) return;
        String tel = getTelefonoEmpresa();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_centro_ayuda, null);
        TextView tvTelefono = dialogView.findViewById(R.id.dialog_ayuda_tv_telefono);
        if (tvTelefono != null) {
            tvTelefono.setText(tel);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnCerrar = dialogView.findViewById(R.id.dialog_ayuda_btn_cerrar);
        if (btnCerrar != null) btnCerrar.setOnClickListener(v -> dialog.dismiss());

        View btnCancelar = dialogView.findViewById(R.id.dialog_ayuda_btn_cancelar);
        if (btnCancelar != null) btnCancelar.setOnClickListener(v -> dialog.dismiss());

        View btnLlamar = dialogView.findViewById(R.id.dialog_ayuda_btn_llamar);
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> {
                dialog.dismiss();
                llamarEmpresa();
            });
        }

        View btnWhatsapp = dialogView.findViewById(R.id.dialog_ayuda_btn_whatsapp);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> {
                dialog.dismiss();
                abrirWhatsapp();
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
            int marginB = (int) (12 * getResources().getDisplayMetrics().density);
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

    private void mostrarDialogoEditarInformacionPersonal() {
        if (!isAdded() || getContext() == null || getActivity() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String currentNombre = prefs.getString("user_nombre", "");
        String currentEmail = prefs.getString("user_email", "");
        String currentTelefono = prefs.getString("user_telefono", "");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_informacion_personal, null);
        EditText etNombre = dialogView.findViewById(R.id.et_dialog_info_nombre);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_info_email);
        EditText etTelefono = dialogView.findViewById(R.id.et_dialog_info_telefono);
        TextView tvError = dialogView.findViewById(R.id.tv_dialog_info_error);
        View btnCerrar = dialogView.findViewById(R.id.dialog_info_btn_cerrar);
        View btnCancelar = dialogView.findViewById(R.id.dialog_info_btn_cancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.dialog_info_btn_guardar);

        etNombre.setText(currentNombre);
        etEmail.setText(currentEmail);
        etTelefono.setText(currentTelefono);

        // Pre-cargar datos si existen en Room y los de prefs estuvieran incompletos
        if (!userId.isEmpty()) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Usuario u = AppDatabase.getInstance(requireContext()).usuarioDao().obtenerUsuarioDirectoPorId(userId);
                if (u != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (etNombre.getText().toString().trim().isEmpty() && u.nombre != null) {
                            etNombre.setText(u.nombre);
                        }
                        if (etEmail.getText().toString().trim().isEmpty() && u.email != null) {
                            etEmail.setText(u.email);
                        }
                        if (etTelefono.getText().toString().trim().isEmpty() && u.telefono != null) {
                            etTelefono.setText(u.telefono);
                        }
                    });
                }
            });
        }

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
                String nuevoNombre = etNombre.getText().toString().trim();
                String nuevoEmail = etEmail.getText().toString().trim();
                String nuevoTelefono = etTelefono.getText().toString().trim();

                if (nuevoNombre.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa tu nombre y apellido");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etNombre.requestFocus();
                    return;
                }

                if (nuevoEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa un correo electrónico válido");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etEmail.requestFocus();
                    return;
                }

                if (tvError != null) tvError.setVisibility(View.GONE);
                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando...");

                // 1. Guardar en SharedPreferences
                prefs.edit()
                        .putString("user_nombre", nuevoNombre)
                        .putString("user_email", nuevoEmail)
                        .putString("user_telefono", nuevoTelefono)
                        .apply();

                try {
                    SharedPreferences loginPrefs = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
                    if (loginPrefs.getBoolean("remember_me", false)) {
                        loginPrefs.edit().putString("saved_email", nuevoEmail).apply();
                    }
                } catch (Exception ignored) {}

                // 2. Actualizar vistas en la pantalla del Perfil
                if (getView() != null) {
                    TextView tvNombre = getView().findViewById(R.id.tv_nombre_usuario_perfil);
                    TextView tvEmail = getView().findViewById(R.id.tv_email_usuario_perfil);
                    if (tvNombre != null) tvNombre.setText(nuevoNombre);
                    if (tvEmail != null) tvEmail.setText(nuevoEmail);
                }

                // 3. Guardar en Base de Datos Local Room
                if (!userId.isEmpty()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            AppDatabase.getInstance(requireContext())
                                    .usuarioDao().actualizarDatosPersonales(userId, nuevoNombre, nuevoEmail, nuevoTelefono);
                        } catch (Exception e) {
                            Log.e("PerfilUsuarioFragment", "Error al actualizar en Room: " + e.getMessage());
                        }
                    });
                }

                // 4. Sincronizar en Supabase
                if (!userId.isEmpty() && !"local_user".equals(userId)) {
                    SupabaseClient.actualizarDatosUsuario(userId, nuevoNombre, nuevoEmail, nuevoTelefono, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Log.d("PerfilUsuarioFragment", "Información de usuario actualizada en Supabase");
                        }

                        @Override
                        public void onError(Exception error) {
                            Log.w("PerfilUsuarioFragment", "Aviso al sincronizar con Supabase: " + error.getMessage());
                        }
                    });
                }

                // Ocultar teclado virtual antes de descartar el diálogo
                View focus = dialog.getCurrentFocus();
                if (focus != null && getContext() != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }

                dialog.dismiss();

                // Mostrar el snackbar flotante con retardo para permitir que el teclado se cierre
                // y el bottom_navigation recupere su posición real en la parte inferior
                if (getView() != null) {
                    getView().postDelayed(() -> {
                        if (isAdded()) {
                            mostrarSnackbarElegante("Información personal actualizada correctamente");
                        }
                    }, 250);
                } else {
                    mostrarSnackbarElegante("Información personal actualizada correctamente");
                }
            });
        }

        dialog.show();
    }

    private void mostrarDialogoCambiarContrasena() {
        if (!isAdded() || getContext() == null || getActivity() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        SharedPreferences loginPrefs = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cambiar_contrasena, null);

        EditText etActual = dialogView.findViewById(R.id.et_dialog_pass_actual);
        EditText etNueva = dialogView.findViewById(R.id.et_dialog_pass_nueva);
        EditText etConfirmar = dialogView.findViewById(R.id.et_dialog_pass_confirmar);

        ImageView btnToggleActual = dialogView.findViewById(R.id.btn_toggle_pass_actual);
        ImageView btnToggleNueva = dialogView.findViewById(R.id.btn_toggle_pass_nueva);
        ImageView btnToggleConfirmar = dialogView.findViewById(R.id.btn_toggle_pass_confirmar);

        ImageView ivReqLength = dialogView.findViewById(R.id.iv_pass_req_length);
        TextView tvReqLength = dialogView.findViewById(R.id.tv_pass_req_length);
        ImageView ivReqUpper = dialogView.findViewById(R.id.iv_pass_req_uppercase);
        TextView tvReqUpper = dialogView.findViewById(R.id.tv_pass_req_uppercase);
        ImageView ivReqNumber = dialogView.findViewById(R.id.iv_pass_req_number);
        TextView tvReqNumber = dialogView.findViewById(R.id.tv_pass_req_number);
        ImageView ivReqMatch = dialogView.findViewById(R.id.iv_pass_req_match);
        TextView tvReqMatch = dialogView.findViewById(R.id.tv_pass_req_match);

        TextView tvError = dialogView.findViewById(R.id.tv_dialog_pass_error);
        View btnCerrar = dialogView.findViewById(R.id.dialog_pass_btn_cerrar);
        View btnCancelar = dialogView.findViewById(R.id.dialog_pass_btn_cancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.dialog_pass_btn_guardar);

        setupPasswordToggle(etActual, btnToggleActual);
        setupPasswordToggle(etNueva, btnToggleNueva);
        setupPasswordToggle(etConfirmar, btnToggleConfirmar);

        TextWatcher passWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = etNueva.getText() != null ? etNueva.getText().toString() : "";
                String confirm = etConfirmar.getText() != null ? etConfirmar.getText().toString() : "";
                actualizarRequisitosContrasena(pass, confirm, ivReqLength, tvReqLength, ivReqUpper, tvReqUpper, ivReqNumber, tvReqNumber, ivReqMatch, tvReqMatch);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNueva.addTextChangedListener(passWatcher);
        etConfirmar.addTextChangedListener(passWatcher);

        // Estado inicial de las condiciones
        actualizarRequisitosContrasena("", "", ivReqLength, tvReqLength, ivReqUpper, tvReqUpper, ivReqNumber, tvReqNumber, ivReqMatch, tvReqMatch);

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
                String passActual = etActual.getText() != null ? etActual.getText().toString().trim() : "";
                String passNueva = etNueva.getText() != null ? etNueva.getText().toString().trim() : "";
                String passConfirmar = etConfirmar.getText() != null ? etConfirmar.getText().toString().trim() : "";

                if (passActual.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Por favor, ingresa tu contraseña actual");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etActual.requestFocus();
                    return;
                }

                String actualHash = SecurityUtils.hashPassword(passActual);
                String savedPass = prefs.getString("user_password", "");
                if (savedPass.isEmpty()) {
                    String savedEmail = loginPrefs.getString("saved_email", "");
                    String currentEmail = prefs.getString("user_email", "");
                    if (savedEmail.equalsIgnoreCase(currentEmail) || savedEmail.isEmpty()) {
                        savedPass = loginPrefs.getString("saved_password", "");
                    }
                }

                if (!savedPass.isEmpty()) {
                    String normActual = passActual.replaceAll("[\\s\\-_.]", "").toLowerCase();
                    String normSaved = savedPass.replaceAll("[\\s\\-_.]", "").toLowerCase();
                    boolean coincide = passActual.equals(savedPass) 
                            || normActual.equals(normSaved)
                            || actualHash.equalsIgnoreCase(savedPass);
                    boolean tieneClaveEstricta = prefs.contains("user_password");
                    if (!coincide && tieneClaveEstricta) {
                        if (tvError != null) {
                            tvError.setText("La contraseña actual no es correcta");
                            tvError.setVisibility(View.VISIBLE);
                        }
                        etActual.requestFocus();
                        return;
                    }
                }

                if (passNueva.length() < 6) {
                    if (tvError != null) {
                        tvError.setText("La nueva contraseña debe tener al menos 6 caracteres");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etNueva.requestFocus();
                    return;
                }

                if (!Pattern.compile("[A-Z]").matcher(passNueva).find()) {
                    if (tvError != null) {
                        tvError.setText("La nueva contraseña debe incluir al menos una letra mayúscula");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etNueva.requestFocus();
                    return;
                }

                if (!Pattern.compile("[0-9]").matcher(passNueva).find()) {
                    if (tvError != null) {
                        tvError.setText("La nueva contraseña debe incluir al menos un número");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etNueva.requestFocus();
                    return;
                }

                if (!passNueva.equals(passConfirmar)) {
                    if (tvError != null) {
                        tvError.setText("Las contraseñas no coinciden");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etConfirmar.requestFocus();
                    return;
                }

                if (passNueva.equals(passActual)) {
                    if (tvError != null) {
                        tvError.setText("La nueva contraseña no puede ser igual a la actual");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    etNueva.requestFocus();
                    return;
                }

                if (tvError != null) tvError.setVisibility(View.GONE);
                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando...");

                // 1. Guardar nueva contraseña en preferencias
                prefs.edit().putString("user_password", passNueva).apply();
                loginPrefs.edit().putString("saved_password", passNueva).apply();

                String newPassHash = SecurityUtils.hashPassword(passNueva);
                String userId = prefs.getString("user_id", "local_user");

                // Sincronizar en la base de datos Supabase
                if (!userId.isEmpty() && !"local_user".equals(userId)) {
                    SupabaseClient.actualizarPassword(userId, newPassHash, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Log.d("PerfilUsuario", "Contraseña sincronizada en Supabase con éxito.");
                        }

                        @Override
                        public void onError(Exception error) {
                            Log.w("PerfilUsuario", "Aviso al guardar contraseña en Supabase: " + error.getMessage());
                        }
                    });
                }

                // 2. Cerrar teclado virtual
                View focus = dialog.getCurrentFocus();
                if (focus != null && getContext() != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }

                dialog.dismiss();

                // 3. Mostrar snackbar flotante con retardo estético
                if (getView() != null) {
                    getView().postDelayed(() -> {
                        if (isAdded()) {
                            mostrarSnackbarElegante("Contraseña actualizada correctamente");
                        }
                    }, 250);
                } else {
                    mostrarSnackbarElegante("Contraseña actualizada correctamente");
                }
            });
        }

        dialog.show();
    }

    private void setupPasswordToggle(EditText et, ImageView btnToggle) {
        if (et == null || btnToggle == null) return;
        final boolean[] isVisible = {false};
        btnToggle.setOnClickListener(v -> {
            isVisible[0] = !isVisible[0];
            if (isVisible[0]) {
                et.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggle.setImageResource(R.drawable.ic_visibility);
                if (getContext() != null) {
                    btnToggle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                }
            } else {
                et.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggle.setImageResource(R.drawable.ic_visibility_off);
                if (getContext() != null) {
                    btnToggle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorOutline));
                }
            }
            if (et.getText() != null) {
                et.setSelection(et.getText().length());
            }
        });
    }

    private void actualizarRequisitosContrasena(String password, String confirmPassword,
                                                ImageView ivLength, TextView tvLength,
                                                ImageView ivUpper, TextView tvUpper,
                                                ImageView ivNumber, TextView tvNumber,
                                                ImageView ivMatch, TextView tvMatch) {
        if (!isAdded() || getContext() == null) return;

        boolean hasMinLength = password.length() >= 6;
        boolean hasUppercase = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasNumber = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasMatch = !password.isEmpty() && password.equals(confirmPassword);

        int colorOk = ContextCompat.getColor(requireContext(), R.color.colorSecondary);
        int colorPending = ContextCompat.getColor(requireContext(), R.color.colorOutline);

        setRequirementState(ivLength, tvLength, hasMinLength, colorOk, colorPending);
        setRequirementState(ivUpper, tvUpper, hasUppercase, colorOk, colorPending);
        setRequirementState(ivNumber, tvNumber, hasNumber, colorOk, colorPending);
        setRequirementState(ivMatch, tvMatch, hasMatch, colorOk, colorPending);
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
}
