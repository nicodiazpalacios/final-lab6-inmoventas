package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;
import androidx.room.Query;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;

@Dao
public interface PropiedadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Propiedad propiedad);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<Propiedad> propiedades);

    @Update
    void actualizar(Propiedad propiedad);

    @Delete
    void eliminar(Propiedad propiedad);

    @Query("DELETE FROM propiedades WHERE id = :id")
    void eliminarPorId(String id);

    @Query("DELETE FROM propiedades WHERE id NOT IN (:idsServidor) AND (estadoSincronizacion IS NULL OR estadoSincronizacion != 'PENDIENTE_SUBIR')")
    void eliminarNoEnServidor(List<String> idsServidor);

    @Transaction
    default void sincronizarConServidor(List<Propiedad> listaServidor) {
        if (listaServidor != null && !listaServidor.isEmpty()) {
            insertarTodas(listaServidor);
            List<String> ids = new java.util.ArrayList<>();
            for (Propiedad p : listaServidor) {
                if (p.id != null) ids.add(p.id);
            }
            if (!ids.isEmpty()) {
                eliminarNoEnServidor(ids);
            }
        }
    }

    @Query("SELECT * FROM propiedades ORDER BY created_at DESC, id DESC")
    LiveData<List<Propiedad>> obtenerTodas();

    @Query("SELECT * FROM propiedades ORDER BY id DESC")
    List<Propiedad> obtenerTodasSync();

    @Query("SELECT * FROM propiedades WHERE id = :id LIMIT 1")
    LiveData<Propiedad> obtenerPorId(String id);

    @Query("SELECT * FROM propiedades WHERE id = :id LIMIT 1")
    Propiedad obtenerPorIdSync(String id);

    @Query("SELECT * FROM propiedades WHERE tipoPropiedad = :tipo")
    LiveData<List<Propiedad>> obtenerPorTipo(TipoPropiedad tipo);

    @Query("SELECT * FROM propiedades WHERE estado = :estado")
    LiveData<List<Propiedad>> obtenerPorEstado(String estado);

    @Query("SELECT * FROM propiedades WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Propiedad> obtenerPendientesDeSubir();
}
