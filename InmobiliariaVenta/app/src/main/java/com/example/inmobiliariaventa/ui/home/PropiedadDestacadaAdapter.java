package com.example.inmobiliariaventa.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropiedadDestacadaAdapter extends RecyclerView.Adapter<PropiedadDestacadaAdapter.ViewHolder> {

    private List<Propiedad> propiedades;

    public PropiedadDestacadaAdapter(List<Propiedad> propiedades) {
        this.propiedades = propiedades != null ? propiedades : new ArrayList<>();
    }

    public void setPropiedades(List<Propiedad> nuevas) {
        this.propiedades = nuevas != null ? new ArrayList<>(nuevas) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_propiedad_destacada, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Propiedad prop = propiedades.get(position);
        Context context = holder.itemView.getContext();
        
        // Formatear precio sin decimales
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMaximumFractionDigits(0);
        holder.priceTextView.setText(format.format(prop.precio));
        
        // Título y ubicación reales
        String tituloReal = prop.titulo != null && !prop.titulo.trim().isEmpty() ? prop.titulo : prop.domicilio;
        holder.titleTextView.setText(tituloReal != null ? tituloReal : "Propiedad");

        String ubicacionReal = (prop.domicilio != null ? prop.domicilio : "");
        if (prop.ciudad != null && !prop.ciudad.isEmpty()) {
            ubicacionReal += (ubicacionReal.isEmpty() ? "" : ", ") + prop.ciudad;
        }
        holder.locationTextView.setText(!ubicacionReal.isEmpty() ? ubicacionReal : "Ubicación");
        
        holder.bedsTextView.setText("🛏 " + prop.ambientes);
        holder.bathsTextView.setText("🚿 " + prop.banos);
        if (prop.superficieM2 != null && prop.superficieM2 > 0) {
            holder.sqftTextView.setText("📐 " + prop.superficieM2.intValue() + " m²");
        } else {
            holder.sqftTextView.setText("📐 N/A");
        }

        if (holder.tagTextView != null) {
            if (prop.tipoPropiedad != null) {
                holder.tagTextView.setText(prop.tipoPropiedad.name());
            } else if (prop.estado != null) {
                holder.tagTextView.setText(prop.estado);
            } else {
                holder.tagTextView.setText("EN VENTA");
            }
        }

        // Cargar imagen de la propiedad si existe
        if (holder.propertyImageView != null) {
            if (prop.imagenes != null && !prop.imagenes.isEmpty()) {
                ImageLoader.loadImage(holder.propertyImageView, prop.imagenes.get(0));
            } else {
                holder.propertyImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // Estado inicial por defecto (desmarcado neutral)
        actualizarEstadoVisualFavorito(holder, false);

        // Si el usuario está registrado, verificar estado real en Room
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");

        if (isLoggedIn && !userId.isEmpty() && prop.id != null) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                boolean esFav = AppDatabase.getInstance(context).favoritoDao().esFavorito(userId, prop.id);
                holder.itemView.post(() -> {
                    actualizarEstadoVisualFavorito(holder, esFav);
                });
            });
        }

        // Manejar interacción con el botón de favorito
        View.OnClickListener favClickListener = v -> {
            SharedPreferences p = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean logged = p.getBoolean("is_user_logged_in", false);
            String uId = p.getString("user_id", "");

            if (!logged || uId.isEmpty()) {
                mostrarDialogoRequerirLogin(context, v);
                return;
            }

            boolean esActualFav = Boolean.TRUE.equals(holder.favoriteButton.getTag());
            final boolean nuevoEstado = !esActualFav;
            actualizarEstadoVisualFavorito(holder, nuevoEstado);

            // Micro-animación de pulso táctil
            holder.favoriteButton.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120)
                    .withEndAction(() -> holder.favoriteButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();

            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);

                Usuario u = db.usuarioDao().obtenerUsuarioDirectoPorId(uId);
                if (u == null) {
                    Usuario nuevoU = new Usuario();
                    nuevoU.id = uId;
                    nuevoU.nombre = p.getString("user_nombre", "Usuario");
                    nuevoU.email = p.getString("user_email", "");
                    nuevoU.telefono = p.getString("user_telefono", "");
                    nuevoU.rol = p.getString("user_rol", "CLIENTE");
                    db.usuarioDao().insertar(nuevoU);
                }

                if (nuevoEstado) {
                    Favorito fav = new Favorito();
                    fav.usuarioId = uId;
                    fav.propiedadId = prop.id;
                    fav.fechaGuardado = System.currentTimeMillis();
                    fav.estadoSincronizacion = "SINCRONIZADO";
                    db.favoritoDao().insertar(fav);
                } else {
                    db.favoritoDao().eliminarPorIds(uId, prop.id);
                }

                holder.itemView.post(() -> {
                    Toast.makeText(context,
                            nuevoEstado ? "¡Propiedad guardada en Favoritos!" : "Propiedad eliminada de Favoritos",
                            Toast.LENGTH_SHORT).show();
                });
            });
        };

        holder.favoriteButton.setOnClickListener(favClickListener);
        if (holder.favoriteButtonCard != null) {
            holder.favoriteButtonCard.setOnClickListener(favClickListener);
        }

        // Navegar al detalle de la propiedad al hacer click en la tarjeta
        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("propiedad_id", prop.id);
            Navigation.findNavController(v).navigate(R.id.nav_detalle_propiedad, bundle);
        });
    }

    private void actualizarEstadoVisualFavorito(ViewHolder holder, boolean fav) {
        if (holder == null || holder.favoriteButton == null) return;
        Context ctx = holder.itemView.getContext();
        holder.favoriteButton.setTag(fav);
        if (fav) {
            holder.favoriteButton.setImageResource(R.drawable.ic_favorite);
            holder.favoriteButton.setColorFilter(ContextCompat.getColor(ctx, R.color.colorPrimary));
            if (holder.favoriteButtonCard != null) {
                holder.favoriteButtonCard.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.colorPrimaryFixed)
                );
            }
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            holder.favoriteButton.setColorFilter(ContextCompat.getColor(ctx, R.color.colorOnSurfaceVariant));
            if (holder.favoriteButtonCard != null) {
                holder.favoriteButtonCard.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.colorSurfaceContainerLowest)
                );
            }
        }
    }

    private void mostrarDialogoRequerirLogin(Context context, View anchorView) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_requerir_login, null);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_login_icon);
        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_login_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_login_mensaje);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btn_dialog_login);
        MaterialButton btnRegistro = dialogView.findViewById(R.id.btn_dialog_registro);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_cancelar);

        if (ivIcon != null) {
            ivIcon.setImageResource(R.drawable.ic_favorite);
        }
        if (tvTitulo != null) {
            tvTitulo.setText("Guardá tus favoritos");
        }
        if (tvMensaje != null) {
            tvMensaje.setText("Para guardar esta propiedad en tu lista de favoritos y acceder a ella cuando quieras, necesitás ingresar con tu cuenta de cliente en Home Finder.");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Navigation.findNavController(anchorView).navigate(R.id.nav_login);
                } catch (Exception e) {
                    Toast.makeText(context, "Por favor ingresa desde la sección Perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnRegistro != null) {
            btnRegistro.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Navigation.findNavController(anchorView).navigate(R.id.nav_registro);
                } catch (Exception e) {
                    Toast.makeText(context, "Por favor registrate desde la sección Perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return propiedades != null ? propiedades.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView priceTextView, titleTextView, locationTextView;
        TextView bedsTextView, bathsTextView, sqftTextView, tagTextView;
        ImageView favoriteButton;
        CardView favoriteButtonCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            bedsTextView = itemView.findViewById(R.id.bedsTextView);
            bathsTextView = itemView.findViewById(R.id.bathsTextView);
            sqftTextView = itemView.findViewById(R.id.sqftTextView);
            tagTextView = itemView.findViewById(R.id.tagTextView);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            favoriteButtonCard = itemView.findViewById(R.id.favoriteButtonCard);
        }
    }
}
