package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.inmobiliariaventa.domain.entity.CiudadGeoref;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface CiudadGeorefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(CiudadGeoref ciudad);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<CiudadGeoref> ciudades);

    @Query("SELECT nombreCiudad FROM ciudades_georef WHERE provinciaKey = :provinciaKey ORDER BY nombreCiudad COLLATE NOCASE ASC")
    List<String> obtenerNombresPorProvinciaSync(String provinciaKey);

    @Query("SELECT * FROM ciudades_georef WHERE provinciaKey = :provinciaKey ORDER BY nombreCiudad COLLATE NOCASE ASC")
    List<CiudadGeoref> obtenerEntidadesPorProvinciaSync(String provinciaKey);

    @Query("SELECT * FROM ciudades_georef WHERE provinciaKey = :provinciaKey ORDER BY nombreCiudad COLLATE NOCASE ASC")
    LiveData<List<CiudadGeoref>> obtenerPorProvinciaLiveData(String provinciaKey);

    @Query("SELECT COUNT(*) FROM ciudades_georef WHERE provinciaKey = :provinciaKey")
    int contarCiudadesPorProvinciaSync(String provinciaKey);

    @Query("SELECT DISTINCT provinciaNombre FROM ciudades_georef WHERE provinciaNombre IS NOT NULL AND provinciaNombre != '' ORDER BY provinciaNombre ASC")
    List<String> obtenerProvinciasCacheadasSync();

    @Transaction
    default void guardarCacheProvincia(String provinciaKey, String provinciaNombre, List<String> nombresCiudades) {
        if (provinciaKey == null || provinciaKey.trim().isEmpty() || nombresCiudades == null || nombresCiudades.isEmpty()) {
            return;
        }
        String keyNorm = provinciaKey.trim();
        long ahora = System.currentTimeMillis();
        List<CiudadGeoref> entidades = new ArrayList<>(nombresCiudades.size());
        for (String nombre : nombresCiudades) {
            if (nombre != null && !nombre.trim().isEmpty()) {
                entidades.add(new CiudadGeoref(keyNorm, nombre.trim(), provinciaNombre, ahora));
            }
        }
        if (!entidades.isEmpty()) {
            insertarTodas(entidades);
        }
    }
}
