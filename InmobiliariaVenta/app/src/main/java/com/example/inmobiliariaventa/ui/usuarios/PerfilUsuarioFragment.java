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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.bumptech.glide.Glide;
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
            btnInfoPersonal.setOnClickListener(v -> {
                String uNombre = prefs.getString("user_nombre", "Usuario");
                String uEmail = prefs.getString("user_email", "Sin correo");
                new AlertDialog.Builder(requireContext())
                        .setTitle("Información Personal")
                        .setMessage("Nombre: " + uNombre + "\nCorreo: " + uEmail + "\nRol: Cliente")
                        .setPositiveButton("Aceptar", null)
                        .show();
            });
        }

        // Seguridad
        View btnSeguridad = view.findViewById(R.id.btn_perfil_seguridad);
        if (btnSeguridad != null) {
            btnSeguridad.setOnClickListener(v -> {
                mostrarSnackbarElegante("Tu cuenta se encuentra protegida con contraseña.");
            });
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
        cropOptions.fixAspectRatio = false; // Permite recortar libremente o ajustar según el gusto del cliente
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
                    Glide.with(this)
                            .load(imageBytes)
                            .circleCrop()
                            .into(ivFotoPerfil);
                    tieneFotoPersonalizada = true;
                    return;
                } catch (Exception ignored) {}
            }
            File file = new File(pathOrData);
            if (file.exists()) {
                ivFotoPerfil.setImageTintList(null);
                Glide.with(this)
                        .load(file)
                        .circleCrop()
                        .into(ivFotoPerfil);
                tieneFotoPersonalizada = true;
                return;
            }
        }
        tieneFotoPersonalizada = false;
        Glide.with(this).clear(ivFotoPerfil);
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
