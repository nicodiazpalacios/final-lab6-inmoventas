package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import com.example.inmobiliariaventa.domain.entity.Usuario;

@Dao
public interface UsuarioDao {
    @Insert
    void insertar(Usuario usuario);

    @Update
    void actualizar(Usuario usuario);

    @Delete
    void eliminar(Usuario usuario);
    
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    androidx.lifecycle.LiveData<Usuario> obtenerPorId(String id);

    @Query("SELECT * FROM usuarios WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    java.util.List<Usuario> obtenerPendientesDeSubir();
}
