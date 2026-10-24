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
    default void upsertSeguroDesdeServidor(List<Contrato> listaServidor) {
        if (listaServidor == null || listaServidor.isEmpty()) {
            // Política de persistencia segura: bajo ninguna circunstancia se borran
            // los contratos locales en Room si la respuesta remota es nula o vacía.
            return;
        }
        for (Contrato remoto : listaServidor) {
            if (remoto == null || remoto.id == null || remoto.id.trim().isEmpty()) {
                continue;
            }
            Contrato local = obtenerPorIdSync(remoto.id);
            if (local != null) {
                // No sobrescribir contratos locales que tengan modificaciones pendientes de subir
                if ("PENDIENTE_SUBIR".equalsIgnoreCase(local.estadoSincronizacion)) {
                    continue;
                }
                // Preservar rutaLocalPdf si el dispositivo ya tenía un PDF generado localmente
                if (local.rutaLocalPdf != null && !local.rutaLocalPdf.trim().isEmpty()
                        && (remoto.rutaLocalPdf == null || remoto.rutaLocalPdf.trim().isEmpty())) {
                    remoto.rutaLocalPdf = local.rutaLocalPdf;
                }
            }
            remoto.estadoSincronizacion = "SINCRONIZADO";
            insertar(remoto);
        }
    }

    @Transaction
    default void sincronizarConServidor(List<Contrato> listaServidor) {
        upsertSeguroDesdeServidor(listaServidor);
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
