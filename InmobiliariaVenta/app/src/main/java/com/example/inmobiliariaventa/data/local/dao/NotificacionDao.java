package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Notificacion;

@Dao
public interface NotificacionDao {
    @Insert
    void insertar(Notificacion notificacion);

    @Update
    void actualizar(Notificacion notificacion);

    @Delete
    void eliminar(Notificacion notificacion);

    @Query("UPDATE notificaciones SET isLeida = 1 WHERE id = :id")
    void marcarComoLeida(String id);
    
    @Query("SELECT * FROM notificaciones WHERE usuario_id = :usuarioId")
    List<Notificacion> obtenerDeUsuario(String usuarioId);

    @Query("SELECT * FROM notificaciones WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Notificacion> obtenerPendientesDeSubir();
}
