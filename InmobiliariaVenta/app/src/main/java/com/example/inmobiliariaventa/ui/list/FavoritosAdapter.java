package com.example.inmobiliariaventa.ui.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.utils.PropiedadDiffCallback;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ImageLoader;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.ViewHolder> {

    public interface OnFavoritoEliminadoListener {
        void onFavoritoEliminado(Propiedad propiedad, int position);
    }

    public interface OnPropiedadClickListener {
        void onPropiedadClick(Propiedad propiedad);
    }

    private List<Propiedad> propiedades;
    private final OnFavoritoEliminadoListener eliminadoListener;
    private final OnPropiedadClickListener clickListener;

    public FavoritosAdapter(List<Propiedad> propiedades,
                            OnFavoritoEliminadoListener eliminadoListener,
                            OnPropiedadClickListener clickListener) {
        this.propiedades = propiedades != null ? propiedades : new ArrayList<>();
        this.eliminadoListener = eliminadoListener;
        this.clickListener = clickListener;
        setHasStableIds(true);
    }

    public void setPropiedades(List<Propiedad> nuevas) {
        List<Propiedad> nuevaLista = nuevas != null ? new ArrayList<>(nuevas) : new ArrayList<>();
        List<Propiedad> viejaLista = this.propiedades != null ? this.propiedades : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PropiedadDiffCallback(viejaLista, nuevaLista));
        this.propiedades = nuevaLista;
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.propertyImageView != null) {
            ImageLoader.clear(holder.propertyImageView);
        }
    }

    public List<Propiedad> getPropiedades() {
        return propiedades;
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

        // Formato de precio en USD
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMaximumFractionDigits(0);
        holder.priceTextView.setText(format.format(prop.precio));

        // Título de la propiedad
        String tituloReal = prop.titulo != null && !prop.titulo.trim().isEmpty() ? prop.titulo : prop.domicilio;
        holder.titleTextView.setText(tituloReal != null ? tituloReal : "Propiedad");

        // Ubicación
        String ubicacionReal = (prop.domicilio != null ? prop.domicilio : "");
        if (prop.ciudad != null && !prop.ciudad.isEmpty()) {
            ubicacionReal += (ubicacionReal.isEmpty() ? "" : ", ") + prop.ciudad;
        }
        holder.locationTextView.setText(!ubicacionReal.isEmpty() ? ubicacionReal : "Ubicación");

        // Ambientes, baños y superficie
        holder.bedsTextView.setText("🛏 " + prop.ambientes);
        holder.bathsTextView.setText("🚿 " + prop.banos);
        if (prop.superficieM2 != null && prop.superficieM2 > 0) {
            holder.sqftTextView.setText("📐 " + prop.superficieM2.intValue() + " m²");
        } else {
            holder.sqftTextView.setText("📐 N/A");
        }

        // Etiqueta de tipo o estado
        if (holder.tagTextView != null) {
            if (prop.tipoPropiedad != null) {
                holder.tagTextView.setText(prop.tipoPropiedad.name());
            } else if (prop.estado != null) {
                holder.tagTextView.setText(prop.estado);
            } else {
                holder.tagTextView.setText("EN VENTA");
            }
        }

        // Carga optimizada de imagen
        if (holder.propertyImageView != null) {
            if (prop.imagenes != null && !prop.imagenes.isEmpty()) {
                ImageLoader.loadImage(holder.propertyImageView, prop.imagenes.get(0));
            } else {
                holder.propertyImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // Estado visual del botón favorito: Activo / Relleno en la pantalla de Mis Favoritos
        holder.favoriteButton.setImageResource(R.drawable.ic_favorite);
        holder.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        if (holder.favoriteButtonCard != null) {
            holder.favoriteButtonCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.colorPrimaryFixed)
            );
        }

        // Click en el corazón para eliminar de favoritos
        View.OnClickListener favClickListener = v -> {
            holder.favoriteButton.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120)
                    .withEndAction(() -> holder.favoriteButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();

            if (eliminadoListener != null) {
                eliminadoListener.onFavoritoEliminado(prop, holder.getAdapterPosition());
            }
        };

        holder.favoriteButton.setOnClickListener(favClickListener);
        if (holder.favoriteButtonCard != null) {
            holder.favoriteButtonCard.setOnClickListener(favClickListener);
        }

        // Click en la tarjeta completa para navegar al detalle
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPropiedadClick(prop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return propiedades != null ? propiedades.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        CardView tagCardView, favoriteButtonCard;
        TextView tagTextView;
        ImageView favoriteButton;
        TextView priceTextView, titleTextView, locationTextView;
        TextView bedsTextView, bathsTextView, sqftTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            tagCardView = itemView.findViewById(R.id.tagCardView);
            tagTextView = itemView.findViewById(R.id.tagTextView);
            favoriteButtonCard = itemView.findViewById(R.id.favoriteButtonCard);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            bedsTextView = itemView.findViewById(R.id.bedsTextView);
            bathsTextView = itemView.findViewById(R.id.bathsTextView);
            sqftTextView = itemView.findViewById(R.id.sqftTextView);
        }
    }
}
