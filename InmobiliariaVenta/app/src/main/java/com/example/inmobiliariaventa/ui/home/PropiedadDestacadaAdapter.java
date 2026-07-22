package com.example.inmobiliariaventa.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class PropiedadDestacadaAdapter extends RecyclerView.Adapter<PropiedadDestacadaAdapter.ViewHolder> {

    private List<Propiedad> propiedades;

    public PropiedadDestacadaAdapter(List<Propiedad> propiedades) {
        this.propiedades = propiedades;
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
        
        // Usamos domicilio como titulo por ahora
        holder.titleTextView.setText(prop.domicilio);
        holder.locationTextView.setText(prop.descripcion != null ? prop.descripcion : "Ubicación");
        
        holder.bedsTextView.setText(String.valueOf(prop.ambientes));
        holder.bathsTextView.setText(String.valueOf(prop.banos));
        holder.sqftTextView.setText("N/A"); // No hay m2 en la entidad actual
        
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
    }

    @Override
    public int getItemCount() {
        return propiedades != null ? propiedades.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView priceTextView, titleTextView, locationTextView;
        TextView bedsTextView, bathsTextView, sqftTextView;
        android.widget.ImageButton favoriteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            bedsTextView = itemView.findViewById(R.id.bedsTextView);
            bathsTextView = itemView.findViewById(R.id.bathsTextView);
            sqftTextView = itemView.findViewById(R.id.sqftTextView);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}
