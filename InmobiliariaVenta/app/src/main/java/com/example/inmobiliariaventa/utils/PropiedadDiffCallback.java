package com.example.inmobiliariaventa.utils;

import androidx.recyclerview.widget.DiffUtil;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PropiedadDiffCallback extends DiffUtil.Callback {

    private final List<Propiedad> oldList;
    private final List<Propiedad> newList;

    public PropiedadDiffCallback(List<Propiedad> oldList, List<Propiedad> newList) {
        this.oldList = oldList != null ? oldList : Collections.emptyList();
        this.newList = newList != null ? newList : Collections.emptyList();
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Propiedad oldItem = oldList.get(oldItemPosition);
        Propiedad newItem = newList.get(newItemPosition);
        if (oldItem == null || newItem == null) return false;
        if (oldItem.id != null && newItem.id != null) {
            return oldItem.id.equals(newItem.id);
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Propiedad oldItem = oldList.get(oldItemPosition);
        Propiedad newItem = newList.get(newItemPosition);
        if (oldItem == null || newItem == null) return false;

        return Double.compare(oldItem.precio, newItem.precio) == 0 &&
                Objects.equals(oldItem.titulo, newItem.titulo) &&
                Objects.equals(oldItem.domicilio, newItem.domicilio) &&
                Objects.equals(oldItem.ciudad, newItem.ciudad) &&
                Objects.equals(oldItem.estado, newItem.estado) &&
                Objects.equals(oldItem.tipoPropiedad, newItem.tipoPropiedad) &&
                oldItem.ambientes == newItem.ambientes &&
                oldItem.banos == newItem.banos &&
                Objects.equals(oldItem.superficieM2, newItem.superficieM2) &&
                Objects.equals(oldItem.imagenes, newItem.imagenes);
    }
}
