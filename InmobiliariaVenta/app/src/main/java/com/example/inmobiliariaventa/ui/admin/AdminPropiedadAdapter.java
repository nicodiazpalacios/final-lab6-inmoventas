package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminPropiedadAdapter extends RecyclerView.Adapter<AdminPropiedadAdapter.ViewHolder> {

    public interface OnAdminPropiedadListener {
        void onSwitchVisibilidad(Propiedad propiedad, boolean isChecked);
        void onModificar(Propiedad propiedad);
        void onBorrar(Propiedad propiedad);
        void onDetalle(Propiedad propiedad);
    }

    private List<Propiedad> lista = new ArrayList<>();
    private final OnAdminPropiedadListener listener;

    public AdminPropiedadAdapter(OnAdminPropiedadListener listener) {
        this.listener = listener;
    }

    public void setPropiedades(List<Propiedad> nuevas) {
        this.lista = nuevas != null ? new ArrayList<>(nuevas) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_propiedad, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Propiedad prop = lista.get(position);
        if (prop == null) return;

        // Título
        holder.tvTitulo.setText(prop.titulo != null ? prop.titulo : "Sin título");

        // Ubicación
        StringBuilder ubicacion = new StringBuilder();
        if (prop.domicilio != null && !prop.domicilio.trim().isEmpty()) {
            ubicacion.append(prop.domicilio.trim());
        }
        if (prop.ciudad != null && !prop.ciudad.trim().isEmpty()) {
            if (ubicacion.length() > 0) ubicacion.append(", ");
            ubicacion.append(prop.ciudad.trim());
        }
        if (ubicacion.length() == 0) {
            ubicacion.append("Ubicación no especificada");
        }
        holder.tvUbicacion.setText(ubicacion.toString());

        // Características
        List<String> specs = new ArrayList<>();
        if (prop.ambientes > 0) {
            specs.add(prop.ambientes + " amb");
        }
        if (prop.banos > 0) {
            specs.add(prop.banos + (prop.banos == 1 ? " baño" : " baños"));
        }
        if (prop.superficieM2 != null && prop.superficieM2 > 0) {
            specs.add((int) Math.round(prop.superficieM2) + " m²");
        }
        holder.tvCaracteristicas.setText(!specs.isEmpty() ? TextUtils.join(" • ", specs) : "Características generales");

        // Precio formateado
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        holder.tvPrecio.setText("$ " + nf.format((long) prop.precio) + " USD");

        // Foto portada
        if (prop.imagenes != null && !prop.imagenes.isEmpty() && prop.imagenes.get(0) != null && !prop.imagenes.get(0).isEmpty()) {
            ImageLoader.loadImage(holder.ivFoto, prop.imagenes.get(0));
        } else {
            holder.ivFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Estado y Slide Switch
        boolean isPausado = false;
        boolean isFinalizado = false;
        if (prop.estado != null) {
            String est = prop.estado.trim();
            if (est.equalsIgnoreCase("Pausado") || est.equalsIgnoreCase("PAUSADO") || est.equalsIgnoreCase("PAUSADA") || est.equalsIgnoreCase("NO_VISIBLE") || est.equalsIgnoreCase("Inactivo")) {
                isPausado = true;
            } else if (est.equalsIgnoreCase("Finalizado") || est.equalsIgnoreCase("VENDIDA") || est.equalsIgnoreCase("Vendido") || est.equalsIgnoreCase("FINALIZADO")) {
                isFinalizado = true;
            }
        }

        // Evitar disparo indeseado del listener durante el reciclaje
        holder.switchVisibilidad.setOnCheckedChangeListener(null);

        if (isPausado) {
            holder.switchVisibilidad.setChecked(false);
            holder.cardBadge.setCardBackgroundColor(Color.parseColor("#BA1A1A"));
            holder.tvBadge.setText("Pausado");
        } else if (isFinalizado) {
            holder.switchVisibilidad.setChecked(false);
            holder.cardBadge.setCardBackgroundColor(Color.parseColor("#49454F"));
            holder.tvBadge.setText("Finalizado");
        } else {
            holder.switchVisibilidad.setChecked(true);
            holder.cardBadge.setCardBackgroundColor(Color.parseColor("#006C49"));
            holder.tvBadge.setText("Publicado");
        }

        holder.switchVisibilidad.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSwitchVisibilidad(prop, isChecked);
            }
        });

        // Botones de acción
        holder.btnModificar.setOnClickListener(v -> {
            if (listener != null) listener.onModificar(prop);
        });

        holder.btnBorrar.setOnClickListener(v -> {
            if (listener != null) listener.onBorrar(prop);
        });

        holder.cardItem.setOnClickListener(v -> {
            if (listener != null) listener.onDetalle(prop);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardItem;
        final ImageView ivFoto;
        final MaterialCardView cardBadge;
        final TextView tvBadge;
        final TextView tvTitulo;
        final SwitchCompat switchVisibilidad;
        final TextView tvUbicacion;
        final TextView tvCaracteristicas;
        final TextView tvPrecio;
        final MaterialButton btnModificar;
        final MaterialButton btnBorrar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.card_admin_propiedad_item);
            ivFoto = itemView.findViewById(R.id.iv_admin_propiedad_foto);
            cardBadge = itemView.findViewById(R.id.card_badge_admin_propiedad);
            tvBadge = itemView.findViewById(R.id.tv_badge_admin_propiedad);
            tvTitulo = itemView.findViewById(R.id.tv_admin_propiedad_titulo);
            switchVisibilidad = itemView.findViewById(R.id.switch_admin_propiedad_visibilidad);
            tvUbicacion = itemView.findViewById(R.id.tv_admin_propiedad_ubicacion);
            tvCaracteristicas = itemView.findViewById(R.id.tv_admin_propiedad_caracteristicas);
            tvPrecio = itemView.findViewById(R.id.tv_admin_propiedad_precio);
            btnModificar = itemView.findViewById(R.id.btn_admin_modificar);
            btnBorrar = itemView.findViewById(R.id.btn_admin_borrar);
        }
    }
}
