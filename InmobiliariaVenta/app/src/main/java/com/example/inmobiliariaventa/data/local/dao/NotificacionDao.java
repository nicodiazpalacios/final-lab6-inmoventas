package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.inmobiliariaventa.domain.entity.Notificacion;

import java.util.List;

@Dao
public interface NotificacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Notificacion notificacion);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<Notificacion> notificaciones);

    @Update
    void actualizar(Notificacion notificacion);

    @Delete
    void eliminar(Notificacion notificacion);

    @Query("DELETE FROM notificaciones WHERE id = :id")
    void eliminarPorId(String id);

    @Query("UPDATE notificaciones SET isLeida = 1 WHERE id = :id")
    void marcarComoLeida(String id);

    @Query("UPDATE notificaciones SET isLeida = 1")
    void marcarTodasComoLeidas();

    @Query("SELECT * FROM notificaciones ORDER BY fecha DESC")
    LiveData<List<Notificacion>> obtenerTodasLiveData();

    @Query("SELECT * FROM notificaciones ORDER BY fecha DESC")
    List<Notificacion> obtenerTodasSync();

    @Query("SELECT * FROM notificaciones WHERE usuario_id = :usuarioId ORDER BY fecha DESC")
    List<Notificacion> obtenerDeUsuario(String usuarioId);

    @Query("SELECT * FROM notificaciones WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Notificacion> obtenerPendientesDeSubir();
}
