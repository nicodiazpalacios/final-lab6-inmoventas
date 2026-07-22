package com.example.inmobiliariaventa.data.repository;

import android.app.Application;
import com.example.inmobiliariaventa.domain.repository.ContratoRepository;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.ContratoDao;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import java.util.List;
import java.util.UUID;

public class ContratoRepositoryImpl implements ContratoRepository {

    private ContratoDao contratoDao;

    public ContratoRepositoryImpl(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        contratoDao = database.contratoDao();
    }

    @Override
    public androidx.lifecycle.LiveData<Contrato> obtenerPorId(String id) {
        return contratoDao.obtenerPorId(id);
    }

    @Override
    public androidx.lifecycle.LiveData<List<Contrato>> obtenerDeCliente(String clienteId) {
        return contratoDao.obtenerDeCliente(clienteId);
    }

    @Override
    public void insert(Contrato contrato) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (contrato.id == null || contrato.id.isEmpty()) {
                contrato.id = UUID.randomUUID().toString();
            }
            contrato.estadoSincronizacion = "PENDIENTE_SUBIR";
            contratoDao.insertar(contrato);
        });
    }

    @Override
    public void update(Contrato contrato) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            contrato.estadoSincronizacion = "PENDIENTE_SUBIR";
            contratoDao.actualizar(contrato);
        });
    }

    @Override
    public void delete(Contrato contrato) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            contratoDao.eliminar(contrato);
        });
    }

    @Override
    public List<Contrato> obtenerPendientesDeSubir() {
        return contratoDao.obtenerPendientesDeSubir();
    }
}
