package com.example.inmobiliariaventa.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.example.inmobiliariaventa.data.repository.FavoritoRepositoryImpl;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropiedadDestacadaAdapter extends RecyclerView.Adapter<PropiedadDestacadaAdapter.ViewHolder> {

    private List<Propiedad> propiedades;
    private final java.util.Set<String> favoritosUsuarioIds = new java.util.HashSet<>();
    private boolean favoritosInicializados = false;
    private View coordinatorLayout;

    public PropiedadDestacadaAdapter(List<Propiedad> propiedades) {
        this.propiedades = propiedades != null ? propiedades : new ArrayList<>();
        setHasStableIds(true);
    }

    public void setCoordinatorLayout(View coordinatorLayout) {
        this.coordinatorLayout = coordinatorLayout;
    }

    private View obtenerTargetCoordinator(View fallbackView) {
        if (this.coordinatorLayout != null) {
            return this.coordinatorLayout;
        }
        if (fallbackView != null) {
            View root = fallbackView.getRootView();
            if (root != null) {
                View coordHome = root.findViewById(R.id.coordinator_home);
                if (coordHome instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
                    return coordHome;
                }
                View coordFav = root.findViewById(R.id.coordinator_favoritos);
                if (coordFav instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
                    return coordFav;
                }
            }
            android.view.ViewParent parent = fallbackView.getParent();
            while (parent != null) {
                if (parent instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
                    return (View) parent;
                }
                parent = parent.getParent();
            }
        }
        return fallbackView;
    }

    public void setPropiedades(List<Propiedad> nuevas) {
        this.propiedades = nuevas != null ? new ArrayList<>(nuevas) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void agregarPropiedades(List<Propiedad> mas) {
        if (mas == null || mas.isEmpty()) return;
        int startPos = this.propiedades.size();
        this.propiedades.addAll(mas);
        notifyItemRangeInserted(startPos, mas.size());
    }

    public void recargarFavoritos(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");
        if (isLoggedIn && !userId.isEmpty()) {
            // Sincronizar en background con Supabase y refrescar si hubo cambios
            FavoritoRepositoryImpl.getInstance(context).sincronizarConNube(userId, () -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    List<String> ids = FavoritoRepositoryImpl.getInstance(context).obtenerIdsFavoritosDeUsuario(userId);
                    synchronized (favoritosUsuarioIds) {
                        favoritosUsuarioIds.clear();
                        if (ids != null) {
                            favoritosUsuarioIds.addAll(ids);
                        }
                        favoritosInicializados = true;
                    }
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(this::notifyDataSetChanged);
                });
            });

            // Cargar de Room de inmediato para respuesta visual instantánea
            AppDatabase.databaseWriteExecutor.execute(() -> {
                List<String> ids = AppDatabase.getInstance(context).favoritoDao().obtenerIdsFavoritosDeUsuario(userId);
                synchronized (favoritosUsuarioIds) {
                    favoritosUsuarioIds.clear();
                    if (ids != null) {
                        favoritosUsuarioIds.addAll(ids);
                    }
                    favoritosInicializados = true;
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(this::notifyDataSetChanged);
            });
        } else {
            synchronized (favoritosUsuarioIds) {
                favoritosUsuarioIds.clear();
                favoritosInicializados = true;
            }
            notifyDataSetChanged();
        }
    }

    @Override
    public long getItemId(int position) {
        if (propiedades != null && position >= 0 && position < propiedades.size()) {
            Propiedad p = propiedades.get(position);
            if (p != null && p.id != null) {
                return p.id.hashCode();
            }
        }
        return position;
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

        // Cargar conjunto de favoritos en memoria una única vez de forma asíncrona
        if (!favoritosInicializados) {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
            String userId = prefs.getString("user_id", "");
            if (isLoggedIn && !userId.isEmpty()) {
                favoritosInicializados = true;
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    List<String> ids = AppDatabase.getInstance(context).favoritoDao().obtenerIdsFavoritosDeUsuario(userId);
                    synchronized (favoritosUsuarioIds) {
                        favoritosUsuarioIds.clear();
                        if (ids != null) favoritosUsuarioIds.addAll(ids);
                    }
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(this::notifyDataSetChanged);
                });
            } else {
                favoritosInicializados = true;
            }
        }

        // Consulta en memoria O(1) sin consultar la base de datos en cada scroll para 0 lag
        boolean esFav = prop.id != null && favoritosUsuarioIds.contains(prop.id);
        actualizarEstadoVisualFavorito(holder, esFav);

        // Manejar interacción con el botón de favorito
        View.OnClickListener favClickListener = v -> {
            SharedPreferences p = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean logged = p.getBoolean("is_user_logged_in", false);
            String uId = p.getString("user_id", "");

            if (!logged || uId.isEmpty()) {
                mostrarDialogoRequerirLogin(context, v);
                return;
            }

            boolean esActualFav = prop.id != null && favoritosUsuarioIds.contains(prop.id);
            final boolean nuevoEstado = !esActualFav;
            synchronized (favoritosUsuarioIds) {
                if (nuevoEstado) {
                    favoritosUsuarioIds.add(prop.id);
                } else {
                    favoritosUsuarioIds.remove(prop.id);
                }
            }
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
                    FavoritoRepositoryImpl.getInstance(context).agregarFavorito(fav);

                    // Precargar imágenes en la caché local de disco para visualización sin conexión
                    if (prop.imagenes != null && !prop.imagenes.isEmpty()) {
                        for (String url : prop.imagenes) {
                            if (url != null && !url.trim().isEmpty()) {
                                com.bumptech.glide.Glide.with(context.getApplicationContext())
                                        .load(url.trim())
                                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                        .preload();
                            }
                        }
                    }

                    holder.itemView.post(() -> {
                        View targetView = obtenerTargetCoordinator(holder.itemView);

                        Snackbar snackbar = Snackbar.make(targetView, "¡Propiedad guardada en Favoritos!", Snackbar.LENGTH_SHORT)
                                .setTextColor(ContextCompat.getColor(context, R.color.white));

                        View snackbarView = snackbar.getView();
                        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
                        snackbarView.setBackgroundTintList(null);

                        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
                            int marginH = (int) (16 * context.getResources().getDisplayMetrics().density);
                            int marginB = (int) (8 * context.getResources().getDisplayMetrics().density);
                            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
                            snackbarView.setLayoutParams(marginParams);
                        }
                        snackbarView.setElevation(8 * context.getResources().getDisplayMetrics().density);

                        snackbar.show();
                    });
                } else {
                    Favorito existente = db.favoritoDao().obtenerFavorito(uId, prop.id);
                    final Favorito favRespaldo = new Favorito();
                    favRespaldo.usuarioId = uId;
                    favRespaldo.propiedadId = prop.id;
                    favRespaldo.fechaGuardado = (existente != null && existente.fechaGuardado > 0)
                            ? existente.fechaGuardado : System.currentTimeMillis();
                    favRespaldo.estadoSincronizacion = "PENDIENTE_SUBIR";

                    FavoritoRepositoryImpl.getInstance(context).eliminarFavorito(uId, prop.id);

                    holder.itemView.post(() -> {
                        View targetView = obtenerTargetCoordinator(holder.itemView);

                        Snackbar snackbar = Snackbar.make(targetView, "Propiedad eliminada de Favoritos", Snackbar.LENGTH_LONG)
                                .setAction("DESHACER", vUndo -> {
                                    synchronized (favoritosUsuarioIds) {
                                        if (prop.id != null) {
                                            favoritosUsuarioIds.add(prop.id);
                                        }
                                    }
                                    actualizarEstadoVisualFavorito(holder, true);
                                    FavoritoRepositoryImpl.getInstance(context).agregarFavorito(favRespaldo);
                                })
                                .setActionTextColor(ContextCompat.getColor(context, R.color.colorSnackbarAction))
                                .setTextColor(ContextCompat.getColor(context, R.color.white));

                        View snackbarView = snackbar.getView();
                        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
                        snackbarView.setBackgroundTintList(null);

                        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
                            int marginH = (int) (16 * context.getResources().getDisplayMetrics().density);
                            int marginB = (int) (8 * context.getResources().getDisplayMetrics().density);
                            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
                            snackbarView.setLayoutParams(marginParams);
                        }
                        snackbarView.setElevation(8 * context.getResources().getDisplayMetrics().density);

                        TextView actionTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
                        if (actionTextView != null) {
                            actionTextView.setTypeface(Typeface.DEFAULT_BOLD);
                        }

                        snackbar.show();
                    });
                }
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
