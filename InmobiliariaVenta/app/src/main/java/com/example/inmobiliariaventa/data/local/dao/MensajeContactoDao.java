package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.inmobiliariaventa.domain.entity.MensajeContacto;

import java.util.List;

@Dao
public interface MensajeContactoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MensajeContacto mensaje);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MensajeContacto> mensajes);

    @Update
    void update(MensajeContacto mensaje);

    @Delete
    void delete(MensajeContacto mensaje);

    @Query("SELECT * FROM mensajes_contacto ORDER BY timestamp DESC")
    LiveData<List<MensajeContacto>> getAllMensajesLiveData();

    @Query("SELECT * FROM mensajes_contacto ORDER BY timestamp DESC")
    List<MensajeContacto> getAllMensajesSync();

    @Query("SELECT * FROM mensajes_contacto WHERE id = :id LIMIT 1")
    MensajeContacto getMensajePorId(long id);

    @Query("SELECT * FROM mensajes_contacto WHERE remote_id = :remoteId LIMIT 1")
    MensajeContacto getMensajePorRemoteId(String remoteId);

    @Query("SELECT * FROM mensajes_contacto WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<MensajeContacto> getPendientesDeSubir();

    @Query("SELECT COUNT(*) FROM mensajes_contacto")
    int countMensajesSync();

    @Query("UPDATE mensajes_contacto SET leido = 1 WHERE id = :id")
    void marcarComoLeido(long id);

    @Query("UPDATE mensajes_contacto SET leido = 1 WHERE remote_id = :remoteId")
    void marcarComoLeidoPorRemoteId(String remoteId);

    @Query("UPDATE mensajes_contacto SET estadoSincronizacion = :estado WHERE id = :id")
    void actualizarEstadoSincronizacion(long id, String estado);

    @Query("SELECT COUNT(*) FROM mensajes_contacto WHERE leido = 0")
    LiveData<Integer> getMensajesNoLeidosCount();

    @Query("DELETE FROM mensajes_contacto WHERE id = :id")
    void eliminarPorId(long id);
}
