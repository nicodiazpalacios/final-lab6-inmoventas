package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Contrato;

@Dao
public interface ContratoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Contrato contrato);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodos(List<Contrato> contratos);

    @Update
    void actualizar(Contrato contrato);

    @Delete
    void eliminar(Contrato contrato);

    @Query("DELETE FROM contratos WHERE id = :id")
    void eliminarPorId(String id);

    @Query("DELETE FROM contratos WHERE id NOT IN (:idsServidor) AND (estadoSincronizacion IS NULL OR estadoSincronizacion != 'PENDIENTE_SUBIR')")
    void eliminarNoEnServidor(List<String> idsServidor);

    @Query("DELETE FROM contratos WHERE (estadoSincronizacion IS NULL OR estadoSincronizacion != 'PENDIENTE_SUBIR')")
    void eliminarTodosSincronizados();

    @Transaction
    default void sincronizarConServidor(List<Contrato> listaServidor) {
        if (listaServidor != null && !listaServidor.isEmpty()) {
            insertarTodos(listaServidor);
            List<String> ids = new java.util.ArrayList<>();
            for (Contrato c : listaServidor) {
                if (c.id != null) ids.add(c.id);
            }
            if (!ids.isEmpty()) {
                eliminarNoEnServidor(ids);
            }
        } else {
            eliminarTodosSincronizados();
        }
    }

    @Query("SELECT * FROM contratos ORDER BY timestamp DESC, id DESC")
    LiveData<List<Contrato>> obtenerTodosLiveData();

    @Query("SELECT * FROM contratos ORDER BY timestamp DESC, id DESC")
    List<Contrato> obtenerTodosSync();

    @Query("SELECT COUNT(*) FROM contratos")
    int countSync();

    @Query("SELECT * FROM contratos WHERE id = :id LIMIT 1")
    LiveData<Contrato> obtenerPorId(String id);

    @Query("SELECT * FROM contratos WHERE id = :id LIMIT 1")
    Contrato obtenerPorIdSync(String id);

    @Query("SELECT * FROM contratos WHERE clienteId = :clienteId")
    LiveData<List<Contrato>> obtenerDeCliente(String clienteId);

    @Query("SELECT * FROM contratos WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Contrato> obtenerPendientesDeSubir();
}
