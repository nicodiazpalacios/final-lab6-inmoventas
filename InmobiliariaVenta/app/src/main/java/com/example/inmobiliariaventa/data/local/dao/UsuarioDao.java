package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Usuario;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Usuario usuario);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<Usuario> usuarios);

    @Update
    void actualizar(Usuario usuario);

    @Delete
    void eliminar(Usuario usuario);
    
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    LiveData<Usuario> obtenerPorId(String id);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    Usuario obtenerUsuarioDirectoPorId(String id);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario obtenerPorEmail(String email);

    @Query("SELECT * FROM usuarios ORDER BY nombre ASC")
    LiveData<List<Usuario>> obtenerTodos();

    @Query("SELECT * FROM usuarios ORDER BY nombre ASC")
    List<Usuario> obtenerTodosDirecto();

    @Query("SELECT * FROM usuarios WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Usuario> obtenerPendientesDeSubir();

    @Query("UPDATE usuarios SET foto_perfil = :fotoPerfil WHERE id = :userId")
    void actualizarFotoPerfil(String userId, String fotoPerfil);

    @Query("UPDATE usuarios SET nombre = :nombre, email = :email, telefono = :telefono WHERE id = :userId")
    void actualizarDatosPersonales(String userId, String nombre, String email, String telefono);
}
