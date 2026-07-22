package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;

@Dao
public interface PropiedadDao {
    @Insert
    void insertar(Propiedad propiedad);

    @Update
    void actualizar(Propiedad propiedad);

    @Delete
    void eliminar(Propiedad propiedad);

    @Query("SELECT * FROM propiedades")
    LiveData<List<Propiedad>> obtenerTodas();

    @Query("SELECT * FROM propiedades WHERE tipoPropiedad = :tipo")
    LiveData<List<Propiedad>> obtenerPorTipo(TipoPropiedad tipo);

    @Query("SELECT * FROM propiedades WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Propiedad> obtenerPendientesDeSubir();
}
