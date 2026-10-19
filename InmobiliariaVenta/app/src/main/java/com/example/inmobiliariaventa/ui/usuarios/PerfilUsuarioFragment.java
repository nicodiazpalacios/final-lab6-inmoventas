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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.example.inmobiliariaventa.domain.entity.Usuario;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PerfilUsuarioFragment extends Fragment {

    private EmpresaRepository empresaRepository;
    private ImageView ivFotoPerfil;

    // Launcher para el recortador de imagen
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    if (croppedUri != null) {
                        guardarYAplicarFotoPerfil(croppedUri);
                    }
                } else if (result.getError() != null) {
                    Toast.makeText(getContext(), "Error al recortar la imagen", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Error al procesar la foto de cámara", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Tu cuenta se encuentra protegida con contraseña.", Toast.LENGTH_SHORT).show();
            });
        }

        // Notificaciones
        View btnNotificaciones = view.findViewById(R.id.btn_perfil_notificaciones);
        if (btnNotificaciones != null) {
            btnNotificaciones.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Notificaciones del sistema activadas.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).navigate(R.id.nav_inicio);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }

    private boolean tieneFotoPerfilPersonalizada() {
        if (getContext() == null) return false;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String path = prefs.getString("user_foto_perfil", null);
        if (path != null && !path.isEmpty()) {
            return new File(path).exists();
        }
        String userId = prefs.getString("user_id", "local_user");
        return new File(requireContext().getFilesDir(), "profile_photo_" + userId + ".jpg").exists();
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

                prefs.edit().putString("user_foto_perfil", destFile.getAbsolutePath()).apply();

                mostrarFotoPerfil(destFile.getAbsolutePath());
                Toast.makeText(requireContext(), "Foto de perfil actualizada correctamente", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo guardar la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarFotoPerfil(String path) {
        if (ivFotoPerfil == null || getContext() == null) return;
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                ivFotoPerfil.setImageTintList(null);
                Glide.with(this)
                        .load(file)
                        .circleCrop()
                        .into(ivFotoPerfil);
                return;
            }
        }
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
        mostrarFotoPerfil(null);
        Toast.makeText(requireContext(), "Foto de perfil eliminada correctamente", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "No se pudo abrir WhatsApp. Verifique tener la aplicación instalada.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "No se pudo iniciar la llamada.", Toast.LENGTH_SHORT).show();
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
}
