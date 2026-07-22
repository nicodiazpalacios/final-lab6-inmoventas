package com.example.inmobiliariaventa.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import java.util.List;

public interface PropiedadRepository {
    LiveData<List<Propiedad>> getAllPropiedades();
    void insert(Propiedad propiedad);
    void update(Propiedad propiedad);
    void delete(Propiedad propiedad);
    List<Propiedad> obtenerPendientesDeSubir();
}
