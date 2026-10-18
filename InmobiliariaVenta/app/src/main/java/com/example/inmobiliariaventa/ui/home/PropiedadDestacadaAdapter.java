package com.example.inmobiliariaventa.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ImageLoader;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import androidx.navigation.Navigation;

public class PropiedadDestacadaAdapter extends RecyclerView.Adapter<PropiedadDestacadaAdapter.ViewHolder> {

    private List<Propiedad> propiedades;

    public PropiedadDestacadaAdapter(List<Propiedad> propiedades) {
        this.propiedades = propiedades != null ? propiedades : new java.util.ArrayList<>();
    }

    public void setPropiedades(List<Propiedad> nuevas) {
        this.propiedades = nuevas != null ? new java.util.ArrayList<>(nuevas) : new java.util.ArrayList<>();
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
        
        // Toggle logic for favorite button
        holder.favoriteButton.setOnClickListener(v -> {
            boolean isFavorite = (v.getTag() != null) && (boolean) v.getTag();
            if (isFavorite) {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                v.setTag(false);
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite);
                v.setTag(true);
            }
        });
        // Default state
        holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        holder.favoriteButton.setTag(false);
        
        // Navigate to details screen on click con propiedad_id
        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("propiedad_id", prop.id);
            Navigation.findNavController(v).navigate(R.id.nav_detalle_propiedad, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return propiedades != null ? propiedades.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView priceTextView, titleTextView, locationTextView;
        TextView bedsTextView, bathsTextView, sqftTextView, tagTextView;
        android.widget.ImageView favoriteButton;

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
        }
    }
}
