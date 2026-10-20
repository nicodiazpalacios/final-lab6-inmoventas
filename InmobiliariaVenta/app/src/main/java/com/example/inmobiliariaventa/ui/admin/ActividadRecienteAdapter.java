package com.example.inmobiliariaventa.ui.admin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ActividadRecienteAdapter extends RecyclerView.Adapter<ActividadRecienteAdapter.ViewHolder> {

    public interface OnActividadClickListener {
        void onActividadClick(ActividadAdmin actividad);
    }

    private final List<ActividadAdmin> items = new ArrayList<>();
    private final OnActividadClickListener listener;

    public ActividadRecienteAdapter(List<ActividadAdmin> lista, OnActividadClickListener listener) {
        if (lista != null) {
            this.items.addAll(lista);
        }
        this.listener = listener;
    }

    public void setActividades(List<ActividadAdmin> nuevas) {
        this.items.clear();
        if (nuevas != null) {
            this.items.addAll(nuevas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_actividad_reciente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActividadAdmin item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvNombre.setText(item.getNombrePropiedad());
        holder.tvHorario.setText(item.getHorario());
        holder.tvPrecio.setText(item.getPrecio());
        holder.tvDetalle.setText(item.getDetalle());

        switch (item.getTipo()) {
            case NUEVA_PROPIEDAD:
                holder.ivIcono.setImageResource(R.drawable.ic_real_estate_agent);
                holder.cardIconoBg.setCardBackgroundColor(Color.parseColor("#E6F4EA"));
                holder.ivIcono.setImageTintList(ColorStateList.valueOf(Color.parseColor("#006C49")));
                holder.tvBadgeTipo.setText("NUEVA PROPIEDAD");
                holder.tvBadgeTipo.setTextColor(Color.parseColor("#006C49"));
                holder.tvBadgeTipo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E6F4EA")));
                holder.tvPrecio.setTextColor(Color.parseColor("#006C49"));
                break;

            case CAMBIO_PRECIO:
                holder.ivIcono.setImageResource(R.drawable.ic_trending_up);
                holder.cardIconoBg.setCardBackgroundColor(Color.parseColor("#FFF0D4"));
                holder.ivIcono.setImageTintList(ColorStateList.valueOf(Color.parseColor("#B06000")));
                holder.tvBadgeTipo.setText("CAMBIO DE PRECIO");
                holder.tvBadgeTipo.setTextColor(Color.parseColor("#B06000"));
                holder.tvBadgeTipo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0D4")));
                holder.tvPrecio.setTextColor(Color.parseColor("#B06000"));
                break;

            case DADA_DE_BAJA:
                holder.ivIcono.setImageResource(R.drawable.ic_pause_circle);
                holder.cardIconoBg.setCardBackgroundColor(Color.parseColor("#FCE8E6"));
                holder.ivIcono.setImageTintList(ColorStateList.valueOf(Color.parseColor("#BA1A1A")));
                holder.tvBadgeTipo.setText("DADA DE BAJA");
                holder.tvBadgeTipo.setTextColor(Color.parseColor("#BA1A1A"));
                holder.tvBadgeTipo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FCE8E6")));
                holder.tvPrecio.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActividadClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardIconoBg;
        final ImageView ivIcono;
        final TextView tvBadgeTipo;
        final TextView tvHorario;
        final TextView tvNombre;
        final TextView tvPrecio;
        final TextView tvDetalle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIconoBg = itemView.findViewById(R.id.card_actividad_icono_bg);
            ivIcono = itemView.findViewById(R.id.iv_actividad_icono);
            tvBadgeTipo = itemView.findViewById(R.id.tv_badge_tipo_actividad);
            tvHorario = itemView.findViewById(R.id.tv_actividad_horario);
            tvNombre = itemView.findViewById(R.id.tv_actividad_nombre);
            tvPrecio = itemView.findViewById(R.id.tv_actividad_precio);
            tvDetalle = itemView.findViewById(R.id.tv_actividad_detalle);
        }
    }
}
