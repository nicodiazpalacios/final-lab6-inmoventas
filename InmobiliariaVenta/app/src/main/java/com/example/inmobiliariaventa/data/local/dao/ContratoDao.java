package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
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

    @Query("SELECT * FROM contratos ORDER BY timestamp DESC")
    LiveData<List<Contrato>> obtenerTodosLiveData();

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
