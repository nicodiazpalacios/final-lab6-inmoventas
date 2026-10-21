package com.example.inmobiliariaventa.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AdminFotosAdapter extends RecyclerView.Adapter<AdminFotosAdapter.FotoViewHolder> {

    public interface OnFotoEliminarListener {
        void onEliminarFoto(int position);
    }

    private final List<String> fotos = new ArrayList<>();
    private final OnFotoEliminarListener listener;

    public AdminFotosAdapter(OnFotoEliminarListener listener) {
        this.listener = listener;
    }

    public void setFotos(List<String> nuevasFotos) {
        this.fotos.clear();
        if (nuevasFotos != null) {
            this.fotos.addAll(nuevasFotos);
        }
        notifyDataSetChanged();
    }

    public void agregarFoto(String fotoUri) {
        if (fotoUri != null && !fotoUri.trim().isEmpty()) {
            this.fotos.add(fotoUri);
            notifyItemInserted(this.fotos.size() - 1);
        }
    }

    public void agregarFotos(List<String> nuevas) {
        if (nuevas != null && !nuevas.isEmpty()) {
            int startPos = this.fotos.size();
            this.fotos.addAll(nuevas);
            notifyItemRangeInserted(startPos, nuevas.size());
        }
    }

    public void eliminarFoto(int position) {
        if (position >= 0 && position < fotos.size()) {
            fotos.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, fotos.size() - position);
        }
    }

    public List<String> getFotos() {
        return new ArrayList<>(fotos);
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_foto_propiedad, parent, false);
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        String urlOrUri = fotos.get(position);
        ImageLoader.loadImage(holder.ivThumbnail, urlOrUri);

        holder.btnEliminar.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onEliminarFoto(currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    public static class FotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumbnail;
        final MaterialCardView btnEliminar;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_foto_thumbnail);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_foto);
        }
    }
}
