package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;

@Dao
public interface FavoritoDao {
    @Insert
    void insertar(Favorito favorito);

    @Delete
    void eliminar(Favorito favorito);

    @Query("SELECT p.* FROM propiedades p INNER JOIN favoritos f ON p.id = f.propiedadId WHERE f.usuarioId = :usuarioId")
    List<Propiedad> obtenerFavoritosDeUsuario(String usuarioId);

    @Query("SELECT * FROM favoritos WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Favorito> obtenerPendientesDeSubir();
}
