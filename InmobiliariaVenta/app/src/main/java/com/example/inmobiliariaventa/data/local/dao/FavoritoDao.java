package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;

import androidx.lifecycle.LiveData;
import androidx.room.OnConflictStrategy;

@Dao
public interface FavoritoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Favorito favorito);

    @Delete
    void eliminar(Favorito favorito);

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND propiedadId = :propiedadId")
    void eliminarPorIds(String usuarioId, String propiedadId);

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND propiedadId = :propiedadId LIMIT 1)")
    boolean esFavorito(String usuarioId, String propiedadId);

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND propiedadId = :propiedadId LIMIT 1)")
    LiveData<Boolean> esFavoritoLiveData(String usuarioId, String propiedadId);

    @Query("SELECT * FROM favoritos WHERE usuarioId = :usuarioId AND propiedadId = :propiedadId LIMIT 1")
    Favorito obtenerFavorito(String usuarioId, String propiedadId);

    @Query("SELECT fechaGuardado FROM favoritos WHERE usuarioId = :usuarioId AND propiedadId = :propiedadId LIMIT 1")
    Long obtenerFechaGuardado(String usuarioId, String propiedadId);

    @Query("SELECT p.* FROM propiedades p INNER JOIN favoritos f ON p.id = f.propiedadId WHERE f.usuarioId = :usuarioId ORDER BY f.fechaGuardado DESC, f.propiedadId ASC")
    List<Propiedad> obtenerFavoritosDeUsuario(String usuarioId);

    @Query("SELECT p.* FROM propiedades p INNER JOIN favoritos f ON p.id = f.propiedadId WHERE f.usuarioId = :usuarioId ORDER BY f.fechaGuardado DESC, f.propiedadId ASC")
    LiveData<List<Propiedad>> obtenerFavoritosDeUsuarioLiveData(String usuarioId);

    @Query("SELECT propiedadId FROM favoritos WHERE usuarioId = :usuarioId")
    List<String> obtenerIdsFavoritosDeUsuario(String usuarioId);

    @Query("SELECT propiedadId FROM favoritos WHERE usuarioId = :usuarioId")
    LiveData<List<String>> obtenerIdsFavoritosDeUsuarioLiveData(String usuarioId);

    @Query("SELECT * FROM favoritos WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Favorito> obtenerPendientesDeSubir();
}
