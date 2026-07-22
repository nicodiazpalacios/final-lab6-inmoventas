package com.example.inmobiliariaventa.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;
import com.example.inmobiliariaventa.domain.entity.Contrato;

@Dao
public interface ContratoDao {
    @Insert
    void insertar(Contrato contrato);

    @Update
    void actualizar(Contrato contrato);

    @Delete
    void eliminar(Contrato contrato);

    @Query("SELECT * FROM contratos WHERE id = :id LIMIT 1")
    androidx.lifecycle.LiveData<Contrato> obtenerPorId(String id);
    
    @Query("SELECT * FROM contratos WHERE clienteId = :clienteId")
    androidx.lifecycle.LiveData<List<Contrato>> obtenerDeCliente(String clienteId);

    @Query("SELECT * FROM contratos WHERE estadoSincronizacion = 'PENDIENTE_SUBIR'")
    List<Contrato> obtenerPendientesDeSubir();
}
