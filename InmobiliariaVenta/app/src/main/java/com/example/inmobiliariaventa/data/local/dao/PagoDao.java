package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
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
