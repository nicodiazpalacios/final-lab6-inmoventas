package com.example.inmobiliariaventa.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.R;
import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder> {

    private List<Categoria> categorias;

    public CategoriaAdapter(List<Categoria> categorias) {
        this.categorias = categorias;
    }

    @NonNull
    @Override
    public CategoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria, parent, false);
        return new CategoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position) {
        Categoria cat = categorias.get(position);
        holder.titleTextView.setText(cat.getNombre());
        holder.iconImageView.setImageResource(cat.getIconoRes());
    }

    @Override
    public int getItemCount() {
        return categorias != null ? categorias.size() : 0;
    }

    static class CategoriaViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;

        public CategoriaViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.categoryIcon);
            titleTextView = itemView.findViewById(R.id.categoryName);
        }
    }

    public static class Categoria {
        private String nombre;
        private int iconoRes;

        public Categoria(String nombre, int iconoRes) {
            this.nombre = nombre;
            this.iconoRes = iconoRes;
        }

        public String getNombre() { return nombre; }
        public int getIconoRes() { return iconoRes; }
    }
}
