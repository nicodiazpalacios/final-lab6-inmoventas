package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.inmobiliariaventa.domain.entity.Pago;

import java.util.List;

@Dao
public interface PagoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Pago pago);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodos(List<Pago> pagos);

    @Update
    void actualizar(Pago pago);

    @Delete
    void eliminar(Pago pago);

    @Query("DELETE FROM pagos WHERE id = :id")
    void eliminarPorId(String id);

    @Query("SELECT * FROM pagos WHERE id = :id LIMIT 1")
    Pago obtenerPorIdSync(String id);

    @Query("DELETE FROM pagos WHERE id NOT IN (:idsServidor) AND (estadoSincronizacion IS NULL OR estadoSincronizacion != 'PENDIENTE_SUBIR')")
    void eliminarNoEnServidor(List<String> idsServidor);

    @Query("DELETE FROM pagos WHERE (estadoSincronizacion IS NULL OR estadoSincronizacion != 'PENDIENTE_SUBIR')")
    void eliminarTodosSincronizados();

    @Transaction
    default void upsertSeguroDesdeServidor(List<Pago> listaServidor) {
        if (listaServidor == null || listaServidor.isEmpty()) {
            // Política de persistencia segura: nunca borrar pagos locales si la respuesta es nula o vacía
            return;
        }
        for (Pago remoto : listaServidor) {
            if (remoto == null || remoto.id == null || remoto.id.trim().isEmpty()) {
                continue;
            }
            Pago local = obtenerPorIdSync(remoto.id);
            if (local != null && "PENDIENTE_SUBIR".equalsIgnoreCase(local.estadoSincronizacion)) {
                continue;
            }
            remoto.estadoSincronizacion = "SINCRONIZADO";
            insertar(remoto);
        }
    }

    @Transaction
    default void sincronizarConServidor(List<Pago> listaServidor) {
        upsertSeguroDesdeServidor(listaServidor);
    }

    @Query("SELECT * FROM pagos WHERE contratoId = :contratoId ORDER BY nroCuota ASC, id ASC")
    LiveData<List<Pago>> obtenerPorContratoLiveData(String contratoId);

    @Query("SELECT * FROM pagos WHERE contratoId = :contratoId ORDER BY nroCuota ASC, id ASC")
    List<Pago> obtenerPorContratoSync(String contratoId);

    @Query("SELECT * FROM pagos ORDER BY nroCuota ASC")
    List<Pago> obtenerTodosSync();

    @Query("DELETE FROM pagos WHERE contratoId = :contratoId")
    void eliminarPorContrato(String contratoId);

    @Query("SELECT * FROM pagos WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Pago> obtenerPendientesDeSubir();
}
